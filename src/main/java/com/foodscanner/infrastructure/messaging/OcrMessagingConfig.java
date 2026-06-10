package com.foodscanner.infrastructure.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Слой: infrastructure.
 * RabbitMQ-топология OCR (exchange + очереди job/result). Активна только при
 * ocr.amqp.enabled=true; иначе AMQP не подключается (staging без брокера — без спама).
 */
@Configuration
@ConditionalOnProperty(name = "ocr.amqp.enabled", havingValue = "true")
public class OcrMessagingConfig {

    @Value("${ocr.exchange:ocr}") String exchange;
    @Value("${ocr.jobs-queue:ocr.jobs}") String jobsQueue;
    @Value("${ocr.results-queue:ocr.results}") String resultsQueue;

    @Bean DirectExchange ocrExchange() { return new DirectExchange(exchange, true, false); }
    @Bean Queue ocrJobsQueue() { return new Queue(jobsQueue, true); }
    @Bean Queue ocrResultsQueue() { return new Queue(resultsQueue, true); }

    @Bean Binding ocrJobsBinding() {
        return BindingBuilder.bind(ocrJobsQueue()).to(ocrExchange()).with("job");
    }
    @Bean Binding ocrResultsBinding() {
        return BindingBuilder.bind(ocrResultsQueue()).to(ocrExchange()).with("result");
    }

    /** JSON-конвертер для RabbitTemplate и @RabbitListener (подхватывается автоконфигом). */
    @Bean MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
