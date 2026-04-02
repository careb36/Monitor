package com.monitor.service;

import com.monitor.model.UnifiedEvent;

/**
 * Delivery channel abstraction used by the resilient event dispatcher.
 *
 * <p>Each implementation represents a distinct delivery mechanism (SSE, email, webhook,
 * etc.) and encapsulates its own resilience strategy. The dispatcher in
 * {@link ReliableEventPipelineService} is channel-agnostic and interacts only through
 * this interface, allowing new channels to be added without modifying the pipeline.</p>
 *
 * @see SseEventNotifier
 * @see EmailEventNotifier
 */
public interface EventNotifier {

    /**
     * Returns a unique, human-readable identifier for this delivery channel.
     *
     * @return channel name (e.g. {@code "sse"}, {@code "email"})
     */
    String channel();

    /**
     * Attempts to deliver the given event via this channel.
     *
     * <p>Implementations should be non-blocking where possible and handle transient
     * failures internally. A return value of {@code false} signals the dispatcher that
     * delivery failed and that it should schedule a retry (e.g. via the outbox).</p>
     *
     * @param event the event to deliver; must not be {@code null}
     * @return {@code true} if delivery succeeded or was accepted for async processing,
     *         {@code false} if the attempt failed and a retry is warranted
     */
    boolean notify(UnifiedEvent event);
}
