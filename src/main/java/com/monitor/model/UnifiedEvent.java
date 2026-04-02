package com.monitor.model;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;

/**
 * Unified representation of any observable event in the Monitor platform.
 *
 * <p>Both CDC change-data-capture events (Debezium / {@code log_traza}) and
 * infrastructure health-check events (polling) are normalized into this single
 * class before being published to the {@link com.monitor.service.EventBus}.
 * This allows the Next.js frontend to consume a homogeneous SSE stream regardless
 * of the originating data source.</p>
 *
 * <p>Instances are serialized as JSON for the SSE payload. The {@code timestamp}
 * field is formatted as an ISO-8601 string (e.g. {@code "2024-01-15T10:30:00Z"})
 * rather than epoch millis for readability on the client side.</p>
 *
 * @see EventType
 * @see Severity
 * @see com.monitor.service.EventBus
 */
public class UnifiedEvent {

    /** Discriminator that indicates whether the event came from CDC or infrastructure polling. */
    private EventType type;

    /** Operational severity used to route the event (INFO → SSE only, CRITICAL → SSE + email + outbox). */
    private Severity severity;

    /**
     * Human-readable identifier of the originating system.
     * Examples: {@code "log_traza [ORA-00001]"}, {@code "DATABASE :: prod-db-01"}.
     */
    private String source;

    /** Free-text description of what occurred. */
    private String message;

    /**
     * Wall-clock time at which the event was created inside the Monitor process.
     * Serialized as an ISO-8601 string in UTC.
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant timestamp;

    /**
     * No-arg constructor for Jackson deserialization.
     * Sets {@code timestamp} to {@link Instant#now()} so that deserialized events
     * without an explicit timestamp are still time-stamped on arrival.
     */
    public UnifiedEvent() {
        this.timestamp = Instant.now();
    }

    /**
     * Convenience constructor for creating fully-populated events inline.
     *
     * @param type     the event category (CDC or infrastructure)
     * @param severity the operational severity
     * @param source   identifier of the originating system or component
     * @param message  human-readable description of the event
     */
    public UnifiedEvent(EventType type, Severity severity, String source, String message) {
        this();
        this.type = type;
        this.severity = severity;
        this.source = source;
        this.message = message;
    }

    /**
     * Returns the event category.
     *
     * @return {@link EventType#DATA} for CDC events or {@link EventType#INFRASTRUCTURE} for polling events
     */
    public EventType getType() {
        return type;
    }

    /**
     * Sets the event category.
     *
     * @param type the event category; must not be {@code null}
     */
    public void setType(EventType type) {
        this.type = type;
    }

    /**
     * Returns the operational severity of this event.
     *
     * @return {@link Severity#INFO}, {@link Severity#WARNING}, or {@link Severity#CRITICAL}
     */
    public Severity getSeverity() {
        return severity;
    }

    /**
     * Sets the operational severity.
     *
     * @param severity the severity level; must not be {@code null}
     */
    public void setSeverity(Severity severity) {
        this.severity = severity;
    }

    /**
     * Returns the identifier of the system or component that generated the event.
     *
     * @return source identifier string (e.g. {@code "DATABASE :: prod-db-01"})
     */
    public String getSource() {
        return source;
    }

    /**
     * Sets the source identifier.
     *
     * @param source the originating system identifier
     */
    public void setSource(String source) {
        this.source = source;
    }

    /**
     * Returns the human-readable description of the event.
     *
     * @return descriptive message text
     */
    public String getMessage() {
        return message;
    }

    /**
     * Sets the event description.
     *
     * @param message descriptive text
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Returns the UTC instant at which this event was created.
     *
     * @return event creation timestamp
     */
    public Instant getTimestamp() {
        return timestamp;
    }

    /**
     * Overrides the creation timestamp. Useful when replaying persisted events
     * that carry their original timestamp from the outbox.
     *
     * @param timestamp the timestamp to assign
     */
    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
