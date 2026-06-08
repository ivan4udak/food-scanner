package com.foodscanner.infrastructure.scheduling;

import com.foodscanner.domain.repository.ClientActivityRepository;
import com.foodscanner.domain.repository.ClientLogRepository;
import com.foodscanner.domain.repository.ServerEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

/**
 * Слой: infrastructure.
 *
 * Очистка телеметрии по retention: обычные client_logs — 30 дней, WARN/ERROR — 90 дней,
 * client_activity — 30 дней, server_events — 90 дней. Сроки настраиваются.
 */
@Component
public class TelemetryRetentionJob {

    private static final Logger log = LoggerFactory.getLogger(TelemetryRetentionJob.class);

    private final ClientLogRepository clientLogs;
    private final ClientActivityRepository activity;
    private final ServerEventRepository serverEvents;

    private final long routineDays;
    private final long importantDays;
    private final long activityDays;
    private final long serverEventDays;

    public TelemetryRetentionJob(ClientLogRepository clientLogs,
                                 ClientActivityRepository activity,
                                 ServerEventRepository serverEvents,
                                 @Value("${telemetry.retention.routine-days:30}") long routineDays,
                                 @Value("${telemetry.retention.important-days:90}") long importantDays,
                                 @Value("${telemetry.retention.activity-days:30}") long activityDays,
                                 @Value("${telemetry.retention.server-events-days:90}") long serverEventDays) {
        this.clientLogs = clientLogs;
        this.activity = activity;
        this.serverEvents = serverEvents;
        this.routineDays = routineDays;
        this.importantDays = importantDays;
        this.activityDays = activityDays;
        this.serverEventDays = serverEventDays;
    }

    @Scheduled(cron = "${telemetry.retention.cron:0 30 3 * * *}")
    @Transactional
    public void purge() {
        Instant now = Instant.now();
        int routine = clientLogs.deleteRoutineOlderThan(now.minus(Duration.ofDays(routineDays)));
        int important = clientLogs.deleteImportantOlderThan(now.minus(Duration.ofDays(importantDays)));
        int acts = activity.deleteOlderThan(now.minus(Duration.ofDays(activityDays)));
        int events = serverEvents.deleteOlderThan(now.minus(Duration.ofDays(serverEventDays)));

        if (routine + important + acts + events > 0) {
            log.info("Telemetry retention: client_logs routine={} important={}, activity={}, server_events={}",
                routine, important, acts, events);
        }
    }
}
