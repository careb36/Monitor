package com.monitor.service.persistence;

import java.time.Instant;

import com.monitor.model.EventType;
import com.monitor.model.OutboxStatus;
import com.monitor.model.Severity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * JPA entity that persists a critical-event outbox entry in the {@code CRITICAL_OUTBOX} table.
 *
 * <p>Each row represents a single {@link com.monitor.model.UnifiedEvent} with
 * {@link com.monitor.model.Severity#CRITICAL} severity that must be delivered at least once.
 * The pipeline updates the row after each delivery attempt, tracking the attempt count,
 * delivery status, and the next scheduled retry time.</p>
 *
 * <p>An optimistic-locking version column ({@code ROW_VERSION}) prevents concurrent
 * updates from multiple application instances silently overwriting each other's changes.</p>
 *
 * <p>The compound index {@code IDX_OUTBOX_PENDING_DUE} ({@code DELIVERED, NEXT_ATTEMPT_AT})
 * supports the most frequent query: finding undelivered entries due for dispatch.</p>
 *
 * @see CriticalOutboxRepository
 * @see com.monitor.service.JpaCriticalOutbox
 */
@Entity
@Table(
    name = "CRITICAL_OUTBOX",
    indexes = {
        // Supports the findDue query: WHERE delivered = false AND next_attempt_at <= :now
        // Column order: low-cardinality (DELIVERED) first — Oracle can use prefix skip scan
        @Index(name = "IDX_OUTBOX_PENDING_DUE", columnList = "DELIVERED, NEXT_ATTEMPT_AT")
    }
)
public class CriticalOutboxEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "critical_outbox_seq_gen")
    @SequenceGenerator(name = "critical_outbox_seq_gen", sequenceName = "CRITICAL_OUTBOX_SEQ", allocationSize = 1)
    @Column(name = "ID", nullable = false)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "EVENT_TYPE", nullable = false, length = 64)
    private EventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "SEVERITY", nullable = false, length = 32)
    private Severity severity;

    @Column(name = "EVENT_SOURCE", nullable = false, length = 255)
    private String source;

    @Column(name = "EVENT_MESSAGE", nullable = false, length = 2000)
    private String message;

    @Column(name = "EVENT_TIMESTAMP", nullable = false)
    private Instant eventTimestamp;

    @Column(name = "ATTEMPTS", nullable = false)
    private int attempts;

    @Column(name = "CREATED_AT", nullable = false)
    private Instant createdAt;

    @Column(name = "NEXT_ATTEMPT_AT", nullable = false)
    private Instant nextAttemptAt;

    @Column(name = "DELIVERED", nullable = false)
    private boolean delivered;

    @Column(name = "LAST_ERROR", length = 1000)
    private String lastError;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 32)
    private OutboxStatus status;

    @Column(name = "LAST_STATUS_CHANGE_AT", nullable = false)
    private Instant lastStatusChangeAt;

    @Version
    @Column(name = "ROW_VERSION", nullable = false)
    private long rowVersion;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public EventType getEventType() {
        return eventType;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
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

    public Instant getEventTimestamp() {
        return eventTimestamp;
    }

    public void setEventTimestamp(Instant eventTimestamp) {
        this.eventTimestamp = eventTimestamp;
    }

    public int getAttempts() {
        return attempts;
    }

    public void setAttempts(int attempts) {
        this.attempts = attempts;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getNextAttemptAt() {
        return nextAttemptAt;
    }

    public void setNextAttemptAt(Instant nextAttemptAt) {
        this.nextAttemptAt = nextAttemptAt;
    }

    public boolean isDelivered() {
        return delivered;
    }

    public void setDelivered(boolean delivered) {
        this.delivered = delivered;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public OutboxStatus getStatus() {
        return status;
    }

    public void setStatus(OutboxStatus status) {
        this.status = status;
    }

    public Instant getLastStatusChangeAt() {
        return lastStatusChangeAt;
    }

    public void setLastStatusChangeAt(Instant lastStatusChangeAt) {
        this.lastStatusChangeAt = lastStatusChangeAt;
    }

    public long getRowVersion() {
        return rowVersion;
    }

    public void setRowVersion(long rowVersion) {
        this.rowVersion = rowVersion;
    }
}
