package com.monitor.service;

import com.monitor.model.UnifiedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Queue;

/**
 * SSE replay service that re-delivers missed critical events to a reconnecting client.
 *
 * <p>When a browser's {@code EventSource} reconnects after a network interruption, it
 * sends the {@code Last-Event-ID} header containing the ID of the last event it received.
 * This service reads that header and replays all persisted critical events with a higher
 * ID directly onto the reconnecting {@link SseEmitter}, ensuring zero message loss for
 * CRITICAL-severity events across transient disconnects.</p>
 *
 * <p>Replay is bounded by {@code monitor.sse.replay-limit} (default: 200 entries) to
 * prevent memory pressure on reconnect storms.</p>
 *
 * @see CriticalOutbox#findAfterId(long, int)
 * @see com.monitor.controller.SseController
 */
@Service
public class CriticalReplayService {

    private static final Logger log = LoggerFactory.getLogger(CriticalReplayService.class);

    private final CriticalOutbox criticalOutbox;
    private final EventBus eventBus;
    private final int replayLimit;
    private final Duration processingTimeout;

    /**
     * @param criticalOutbox the outbox from which missed events are retrieved
     * @param eventBus       used to send replayed events directly to the reconnecting emitter
     * @param replayLimit    maximum number of events to replay per reconnect;
     *                       controlled by {@code monitor.sse.replay-limit} (default: 200)
     */
    public CriticalReplayService(CriticalOutbox criticalOutbox,
                                 EventBus eventBus,
                                 @Value("${monitor.sse.replay-limit:200}") int replayLimit,
                                 @Value("${monitor.outbox.processing-timeout:5m}") Duration processingTimeout) {
        this.criticalOutbox = criticalOutbox;
        this.eventBus = eventBus;
        this.replayLimit = replayLimit;
        this.processingTimeout = processingTimeout;
    }

    /**
     * Replays critical events that occurred after the client's last acknowledged event.
     *
     * <p>If {@code lastEventIdHeader} is {@code null}, blank, or not a valid {@code long},
     * the method returns immediately without replaying anything (first-time connections
     * do not receive a history).</p>
     *
     * @param lastEventIdHeader the raw value of the {@code Last-Event-ID} HTTP header;
     *                          may be {@code null} for new connections
     * @param emitter           the newly registered SSE emitter for the reconnecting client
     */
    public void replaySince(String lastEventIdHeader, SseEmitter emitter) {
        if (lastEventIdHeader == null || lastEventIdHeader.isBlank()) {
            return;
        }

        long lastEventId;
        try {
            lastEventId = Long.parseLong(lastEventIdHeader);
        } catch (NumberFormatException ex) {
            log.warn("Ignoring invalid Last-Event-ID value: {}", lastEventIdHeader);
            return;
        }

        List<OutboxEntry> replayEvents = criticalOutbox.findAfterId(lastEventId, replayLimit);
        for (OutboxEntry entry : replayEvents) {
            boolean sent = eventBus.publishToEmitter(emitter, entry.event(), String.valueOf(entry.id()));
            if (!sent) {
                break;
            }
        }
        if (!replayEvents.isEmpty()) {
            log.debug("Replayed {} critical events for Last-Event-ID={}", replayEvents.size(), lastEventId);
        }
    }

    /**
     * Finds PENDING entries that are due and marks them as PROCESSING.
     * Uses optimistic locking to ensure only one instance picks up the entry.
     */
    public void pollAndLockDueEntries(Queue<Long> criticalQueue, int batchSize) {
        List<OutboxEntry> dueEntries = criticalOutbox.findDue(Instant.now(), batchSize);
        for (OutboxEntry entry : dueEntries) {
            try {
                criticalOutbox.markProcessing(entry.id());
                if (criticalQueue.offer(entry.id())) {
                    log.debug("Entry {} locked and queued for dispatch", entry.id());
                } else {
                    // Queue full, reset to PENDING so others can pick it up or we retry later
                    criticalOutbox.markRetry(entry.id(), Instant.now(), "dispatch-queue-full");
                }
            } catch (ObjectOptimisticLockingFailureException ex) {
                log.warn("Conflict: Entry {} already picked up by another instance. Skipping.", entry.id());
            } catch (Exception ex) {
                log.error("Failed to mark entry {} as processing", entry.id(), ex);
            }
        }
    }

    @Scheduled(fixedDelayString = "${monitor.outbox.cleanup-interval-ms:60000}")
    public void cleanupTimedOutProcessing() {
        Instant olderThan = Instant.now().minus(processingTimeout);
        criticalOutbox.resetProcessingToPending(olderThan);
    }
}
