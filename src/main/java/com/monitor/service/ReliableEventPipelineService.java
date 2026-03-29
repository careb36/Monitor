package com.monitor.service;

import com.monitor.model.IngestMetadata;
import com.monitor.model.Severity;
import com.monitor.model.UnifiedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Resilient ingestion and delivery pipeline with channel decoupling.
 */
@Service
public class ReliableEventPipelineService implements IngestionFacade {

    private static final Logger log = LoggerFactory.getLogger(ReliableEventPipelineService.class);

    private final EventDeduplicator deduplicator;
    private final CriticalOutbox criticalOutbox;
    private final EventNotifier sseNotifier;
    private final EventNotifier emailNotifier;

    private final BlockingQueue<UnifiedEvent> standardQueue;
    private final BlockingQueue<Long> criticalQueue = new LinkedBlockingQueue<>();

    private final Duration retryBaseDelay;
    private final int retryMaxAttempts;
    private final int drainBatchSize;

    public ReliableEventPipelineService(EventDeduplicator deduplicator,
                                        CriticalOutbox criticalOutbox,
                                        @Qualifier("sseEventNotifier") EventNotifier sseNotifier,
                                        @Qualifier("emailEventNotifier") EventNotifier emailNotifier,
                                        @Value("${monitor.pipeline.standard-queue-capacity:1000}") int standardQueueCapacity,
                                        @Value("${monitor.pipeline.retry-base-delay-ms:5000}") long retryBaseDelayMs,
                                        @Value("${monitor.pipeline.retry-max-attempts:12}") int retryMaxAttempts,
                                        @Value("${monitor.pipeline.drain-batch-size:100}") int drainBatchSize) {
        this.deduplicator = deduplicator;
        this.criticalOutbox = criticalOutbox;
        this.sseNotifier = sseNotifier;
        this.emailNotifier = emailNotifier;
        this.standardQueue = new LinkedBlockingQueue<>(standardQueueCapacity);
        this.retryBaseDelay = Duration.ofMillis(retryBaseDelayMs);
        this.retryMaxAttempts = retryMaxAttempts;
        this.drainBatchSize = drainBatchSize;
    }

    @Override
    public void ingest(UnifiedEvent event, IngestMetadata metadata) {
        if (deduplicator.isDuplicate(event)) {
            log.debug("Dropped duplicate event: type={} source={} severity={} corrId={}",
                    event.getType(), event.getSource(), event.getSeverity(), metadata.correlationId());
            return;
        }

        if (event.getSeverity() == Severity.CRITICAL) {
            long outboxId = criticalOutbox.save(event);
            criticalQueue.offer(outboxId);
            return;
        }

        if (!standardQueue.offer(event)) {
            log.warn("Standard queue full; dropping event type={} source={} severity={}",
                    event.getType(), event.getSource(), event.getSeverity());
        }
    }

    @Scheduled(fixedDelayString = "${monitor.pipeline.dispatch-interval-ms:500}")
    void dispatchStandard() {
        int processed = 0;
        while (processed < drainBatchSize) {
            UnifiedEvent event = standardQueue.poll();
            if (event == null) {
                break;
            }
            sseNotifier.notify(event);
            processed++;
        }
    }

    @Scheduled(fixedDelayString = "${monitor.pipeline.dispatch-interval-ms:500}")
    void dispatchCritical() {
        int processed = 0;
        while (processed < drainBatchSize) {
            Long outboxId = criticalQueue.poll();
            if (outboxId == null) {
                break;
            }
            dispatchCriticalEntry(outboxId);
            processed++;
        }
    }

    @Scheduled(fixedDelayString = "${monitor.pipeline.replay-interval-ms:3000}")
    void replayCriticalDue() {
        List<OutboxEntry> dueEntries = criticalOutbox.findDue(Instant.now(), drainBatchSize);
        for (OutboxEntry entry : dueEntries) {
            criticalQueue.offer(entry.id());
        }
    }

    private void dispatchCriticalEntry(long outboxId) {
        OutboxEntry entry = criticalOutbox.find(outboxId).orElse(null);
        if (entry == null || entry.delivered()) {
            return;
        }

        UnifiedEvent event = entry.event();
        sseNotifier.notify(event);
        boolean emailOk = emailNotifier.notify(event);

        if (emailOk) {
            criticalOutbox.markDelivered(outboxId);
            return;
        }

        int attempts = entry.attempts() + 1;
        if (attempts >= retryMaxAttempts) {
            log.error("Critical event exhausted retries. outboxId={} source={} message={}",
                    outboxId, event.getSource(), event.getMessage());
            criticalOutbox.markRetry(outboxId, Instant.now().plus(Duration.ofHours(1)), "retry-limit-reached");
            return;
        }

        // Cap at attempt 8 (2^8 = 256x base delay) to prevent integer overflow and
        // keep the maximum retry interval bounded.
        Duration nextDelay = retryBaseDelay.multipliedBy(1L << Math.min(attempts, 8));
        criticalOutbox.markRetry(outboxId, Instant.now().plus(nextDelay), "email-delivery-failed");
    }
}
