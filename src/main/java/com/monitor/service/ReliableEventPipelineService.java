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
 * Resilient ingestion and delivery pipeline that decouples event producers from consumers.
 *
 * <p>All events enter the pipeline via {@link #ingest(UnifiedEvent, IngestMetadata)}.
 * From there they are routed based on severity:</p>
 * <ul>
 *   <li><strong>Non-critical:</strong> placed on a bounded {@link LinkedBlockingQueue}
 *       and dispatched to SSE clients in fixed-interval batches.</li>
 *   <li><strong>Critical:</strong> persisted immediately to the {@link CriticalOutbox},
 *       then dispatched to SSE clients and email via an exponential-backoff retry loop
 *       until delivery succeeds or the retry limit is reached.</li>
 * </ul>
 *
 * <p>Deduplication is applied at ingestion time by {@link EventDeduplicator}; duplicate
 * events are dropped with a debug log entry and never enter either queue.</p>
 *
 * <p>All scheduled drain/replay tasks run on Spring's task scheduler thread pool.
 * They do not block the caller and are designed for high throughput with minimal
 * lock contention.</p>
 *
 * @see EventDeduplicator
 * @see CriticalOutbox
 * @see EventNotifier
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

    /**
     * @param deduplicator           event deduplication gate
     * @param criticalOutbox         persistence store for critical events
     * @param sseNotifier            SSE delivery channel
     * @param emailNotifier          email delivery channel
     * @param standardQueueCapacity  maximum number of non-critical events buffered
     *                               before new events are dropped; controlled by
     *                               {@code monitor.pipeline.standard-queue-capacity} (default: 1000)
     * @param retryBaseDelayMs       initial retry delay in milliseconds for critical events;
     *                               doubles on each failure (exponential backoff); controlled by
     *                               {@code monitor.pipeline.retry-base-delay-ms} (default: 5000)
     * @param retryMaxAttempts       maximum number of delivery attempts before an entry is
     *                               moved to dead-letter state; controlled by
     *                               {@code monitor.pipeline.retry-max-attempts} (default: 12)
     * @param drainBatchSize         maximum number of entries processed per scheduler tick;
     *                               controlled by {@code monitor.pipeline.drain-batch-size} (default: 100)
     */
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

    /**
     * {@inheritDoc}
     *
     * <p>Processing flow:
     * <ol>
     *   <li>Duplicate check via {@link EventDeduplicator}; duplicates are silently dropped.</li>
     *   <li>Critical events are persisted to the {@link CriticalOutbox} and their IDs are
     *       added to the critical dispatch queue.</li>
     *   <li>Non-critical events are offered to the bounded standard queue; events are
     *       dropped with a warning log if the queue is full (backpressure).</li>
     * </ol>
     * </p>
     */
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

    /**
     * Drains up to {@code drainBatchSize} events from the standard queue and delivers
     * each to all SSE clients. Runs every {@code monitor.pipeline.dispatch-interval-ms}
     * milliseconds (default: 500 ms).
     */
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

    /**
     * Drains up to {@code drainBatchSize} outbox IDs from the critical queue and dispatches
     * each entry via SSE and email. Runs on the same interval as {@link #dispatchStandard()}.
     */
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

    /**
     * Queries the outbox for entries whose retry window has elapsed and re-queues them
     * for dispatch. Runs every {@code monitor.pipeline.replay-interval-ms} milliseconds
     * (default: 3000 ms) to ensure previously-failed critical events are eventually
     * retried.
     */
    @Scheduled(fixedDelayString = "${monitor.pipeline.replay-interval-ms:3000}")
    void replayCriticalDue() {
        List<OutboxEntry> dueEntries = criticalOutbox.findDue(Instant.now(), drainBatchSize);
        for (OutboxEntry entry : dueEntries) {
            criticalQueue.offer(entry.id());
        }
    }

    /**
     * Dispatches a single critical outbox entry via SSE and email channels.
     *
     * <p>On success the entry is marked delivered. On email failure, exponential backoff
     * is applied (base delay × 2<sup>attempt</sup>, capped at 2<sup>8</sup> multiplier).
     * After {@code retryMaxAttempts} failures the entry is moved to a long-hold state
     * (1-hour delay) and an error is logged.</p>
     *
     * @param outboxId the outbox entry identifier to dispatch
     */
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
