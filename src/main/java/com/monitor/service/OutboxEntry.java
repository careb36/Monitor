package com.monitor.service;

import com.monitor.model.UnifiedEvent;

import java.time.Instant;

/**
 * Immutable snapshot of a persisted critical-event outbox entry.
 *
 * <p>Records are returned by {@link CriticalOutbox} query methods and consumed by the
 * {@link ReliableEventPipelineService} dispatcher. Using a record ensures thread safety
 * and prevents accidental mutation outside of the outbox implementation.</p>
 *
 * @param id           unique identifier assigned by the outbox at save time
 * @param event        the original {@link UnifiedEvent} to be delivered
 * @param attempts     number of delivery attempts made so far (0 = not yet attempted)
 * @param createdAt    instant at which the entry was first persisted
 * @param nextAttemptAt earliest instant at which the next delivery attempt should be made;
 *                      entries are returned by {@link CriticalOutbox#findDue} only when
 *                      this instant is on or before the current time
 * @param delivered    {@code true} once the entry has been successfully delivered
 * @param lastError    short description of the most recent failure reason;
 *                     empty string if no failure has occurred yet
 */
public record OutboxEntry(
        long id,
        UnifiedEvent event,
        int attempts,
        Instant createdAt,
        Instant nextAttemptAt,
        boolean delivered,
        String lastError
) {
}
