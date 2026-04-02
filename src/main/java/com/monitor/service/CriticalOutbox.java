package com.monitor.service;

import com.monitor.model.UnifiedEvent;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Persistence contract for critical-event outbox entries.
 *
 * <p>The Outbox Pattern guarantees at-least-once delivery for {@link Severity#CRITICAL}
 * events: every critical event is persisted here <em>before</em> any delivery attempt.
 * The pipeline re-reads undelivered entries on a fixed schedule until delivery succeeds
 * or the retry limit is reached.</p>
 *
 * <p>Two implementations are provided:</p>
 * <ul>
 *   <li>{@link InMemoryCriticalOutbox} – default; fast, no external dependency,
 *       does not survive restarts (suitable for non-critical deployments or testing).</li>
 *   <li>{@link JpaCriticalOutbox} – durable JPA/Oracle backend, activated by
 *       {@code monitor.outbox.jpa.enabled=true}.</li>
 * </ul>
 */
public interface CriticalOutbox {

    /**
     * Persists a new critical event and returns its assigned identifier.
     *
     * <p>The returned ID is used as the SSE {@code Last-Event-ID} header value
     * so that reconnecting clients can request a replay of missed events.</p>
     *
     * @param event the critical event to persist; must not be {@code null}
     * @return a positive, unique ID assigned to the persisted entry
     */
    long save(UnifiedEvent event);

    /**
     * Retrieves a single outbox entry by its ID.
     *
     * @param id the entry identifier returned by {@link #save(UnifiedEvent)}
     * @return an {@link Optional} containing the entry, or empty if not found
     */
    Optional<OutboxEntry> find(long id);

    /**
     * Marks the entry as successfully delivered and increments its attempt counter.
     * Delivered entries are retained for audit purposes but excluded from future dispatch.
     *
     * @param id the entry identifier
     */
    void markDelivered(long id);

    /**
     * Schedules a retry for a failed delivery attempt.
     *
     * <p>Increments the attempt counter, records the failure reason, and sets
     * {@code nextAttemptAt} so the entry is re-queued after the backoff delay.</p>
     *
     * @param id             the entry identifier
     * @param nextAttemptAt  the earliest instant at which delivery should be retried
     * @param reason         a short human-readable description of why delivery failed
     *                       (e.g. {@code "email-delivery-failed"})
     */
    void markRetry(long id, Instant nextAttemptAt, String reason);

    /**
     * Returns undelivered entries whose {@code nextAttemptAt} is on or before {@code now}.
     * Results are ordered by priority (severity descending) then by {@code nextAttemptAt}
     * ascending so that oldest due entries are dispatched first.
     *
     * @param now   the reference instant (typically {@link Instant#now()})
     * @param limit maximum number of entries to return
     * @return ordered list of due entries; never {@code null}, may be empty
     */
    List<OutboxEntry> findDue(Instant now, int limit);

    /**
     * Returns entries whose ID is strictly greater than {@code lastId}, ordered by ID.
     * Used by the SSE replay mechanism to re-send events missed during a client disconnect.
     *
     * @param lastId the last event ID acknowledged by the client
     * @param limit  maximum number of entries to return
     * @return ordered list of subsequent entries; never {@code null}, may be empty
     */
    List<OutboxEntry> findAfterId(long lastId, int limit);

    /**
     * Returns the number of entries that have not yet been successfully delivered.
     *
     * @return non-negative pending count
     */
    long pendingCount();
}
