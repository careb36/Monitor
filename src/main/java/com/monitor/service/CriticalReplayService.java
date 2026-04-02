package com.monitor.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

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

    /**
     * @param criticalOutbox the outbox from which missed events are retrieved
     * @param eventBus       used to send replayed events directly to the reconnecting emitter
     * @param replayLimit    maximum number of events to replay per reconnect;
     *                       controlled by {@code monitor.sse.replay-limit} (default: 200)
     */
    public CriticalReplayService(CriticalOutbox criticalOutbox,
                                 EventBus eventBus,
                                 @Value("${monitor.sse.replay-limit:200}") int replayLimit) {
        this.criticalOutbox = criticalOutbox;
        this.eventBus = eventBus;
        this.replayLimit = replayLimit;
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
}
