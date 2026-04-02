package com.monitor.model;

/**
 * Discriminator that classifies the origin of a {@link UnifiedEvent}.
 *
 * <p>The lowercase name of each constant is used as the SSE event name so that
 * the Next.js frontend can attach type-specific listeners:</p>
 * <pre>
 *   eventSource.addEventListener('data', handler);
 *   eventSource.addEventListener('infrastructure', handler);
 * </pre>
 */
public enum EventType {
    /** Originates from CDC / Debezium (log_traza inserts). */
    DATA,
    /** Originates from the scheduled health-check polling. */
    INFRASTRUCTURE
}
