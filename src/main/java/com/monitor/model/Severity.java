package com.monitor.model;

/**
 * Operational severity levels for {@link UnifiedEvent} instances.
 *
 * <p>Severity drives routing decisions throughout the pipeline:</p>
 * <ul>
 *   <li>{@link #INFO} – delivered to SSE clients only; no email, no outbox persistence.</li>
 *   <li>{@link #WARNING} – delivered to SSE clients only; intended for transient anomalies
 *       that do not require immediate action.</li>
 *   <li>{@link #CRITICAL} – delivered to SSE clients <em>and</em> triggers an email alert;
 *       the event is also persisted in the {@link com.monitor.service.CriticalOutbox}
 *       to guarantee at-least-once delivery even if the first attempt fails.</li>
 * </ul>
 */
public enum Severity {
    /** Informational event; no action required. */
    INFO,
    /** Anomaly detected; warrants attention but is not immediately critical. */
    WARNING,
    /** Service degradation or failure; triggers email notification and outbox persistence. */
    CRITICAL
}
