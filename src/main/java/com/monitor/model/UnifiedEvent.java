package com.monitor.model;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;

/**
 * Unified event that flows through the SSE endpoint.
 * Both CDC (Debezium) and infrastructure polling events are represented by this class.
 */
public class UnifiedEvent {

    private EventType type;
    private Severity severity;
    private String source;
    private String message;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant timestamp;

    public UnifiedEvent() {
        this.timestamp = Instant.now();
    }

    public UnifiedEvent(EventType type, Severity severity, String source, String message) {
        this();
        this.type = type;
        this.severity = severity;
        this.source = source;
        this.message = message;
    }

    public EventType getType() {
        return type;
    }

    public void setType(EventType type) {
        this.type = type;
    }

    public Severity getSeverity() {
        return severity;
    }

    public void setSeverity(Severity severity) {
        this.severity = severity;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
