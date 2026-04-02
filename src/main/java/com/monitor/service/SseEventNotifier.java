package com.monitor.service;

import com.monitor.model.UnifiedEvent;
import org.springframework.stereotype.Component;

/**
 * {@link EventNotifier} implementation that delivers events to all connected SSE clients.
 *
 * <p>Acts as a thin adapter between the pipeline's {@link EventNotifier} contract and
 * the {@link EventBus}. SSE delivery is best-effort: disconnected emitters are silently
 * removed and no retry is attempted at the SSE level (clients reconnect automatically
 * via standard SSE behaviour).</p>
 *
 * <p>This notifier always returns {@code true} so that the pipeline does not attempt
 * SSE-specific retries through the outbox mechanism.</p>
 */
@Component
public class SseEventNotifier implements EventNotifier {

    private final EventBus eventBus;

    /**
     * @param eventBus the central event bus holding all active SSE emitters
     */
    public SseEventNotifier(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@code "sse"}
     */
    @Override
    public String channel() {
        return "sse";
    }

    /**
     * Broadcasts the event to all active SSE clients via the {@link EventBus}.
     *
     * @param event the event to broadcast; must not be {@code null}
     * @return always {@code true} — SSE delivery is best-effort and failures
     *         are handled by client-side reconnection
     */
    @Override
    public boolean notify(UnifiedEvent event) {
        // publish() fans out to all live SSE emitters synchronously; any disconnected
        // emitter is silently removed.  We treat the call as best-effort and always
        // return true so the pipeline does not retry SSE delivery.
        eventBus.publish(event);
        return true;
    }
}
