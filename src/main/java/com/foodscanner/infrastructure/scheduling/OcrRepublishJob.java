package com.foodscanner.infrastructure.scheduling;

import com.foodscanner.application.port.OcrJobPublisher;
import com.foodscanner.domain.model.ocr.OcrJob;
import com.foodscanner.domain.repository.OcrJobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Слой: infrastructure.
 * v1.10.4 — periodic republish: активные QUEUED-задачи, не опубликованные в брокер
 * (RabbitMQ был недоступен при upload), переотправляются. Работает только при
 * ocr.amqp.enabled=true — иначе задачи штатно ждут QUEUED.
 */
@Component
public class OcrRepublishJob {

    private static final Logger log = LoggerFactory.getLogger(OcrRepublishJob.class);

    private final OcrJobRepository repository;
    private final OcrJobPublisher publisher;
    private final boolean enabled;

    public OcrRepublishJob(OcrJobRepository repository, OcrJobPublisher publisher,
                           @Value("${ocr.amqp.enabled:false}") boolean enabled) {
        this.repository = repository;
        this.publisher = publisher;
        this.enabled = enabled;
    }

    @Scheduled(fixedRateString = "${ocr.republish.interval-ms:60000}",
               initialDelayString = "${ocr.republish.initial-delay-ms:30000}")
    public void republish() {
        if (!enabled) return;
        List<OcrJob> pending = repository.findRepublishable();
        if (pending.isEmpty()) return;
        int ok = 0;
        for (OcrJob job : pending) {
            if (publisher.publish(job)) {
                repository.markPublished(job.id());
                ok++;
            } else {
                repository.recordPublishFailure(job.id(), "republish: broker unavailable");
            }
        }
        log.info("OCR republish: попыток={}, опубликовано={}", pending.size(), ok);
    }
}
