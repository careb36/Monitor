package com.monitor.service;

import com.monitor.model.UnifiedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Default in-memory implementation of {@link CriticalOutbox}.
 *
 * <p>All state lives in JVM heap using {@link ConcurrentHashMap} and a
 * {@link ConcurrentLinkedQueue} for delivered-entry history. This implementation is
 * thread-safe and requires no external infrastructure, making it suitable for
 * development, testing, and deployments where event durability across restarts is
 * not required.</p>
 *
 * <p><strong>Memory management:</strong> Undelivered entries are kept indefinitely
 * until delivered. Delivered entries are retained up to {@code maxDeliveredEntries}
 * (default: 1000) to support SSE replay; the oldest delivered entries are evicted
 * when the limit is exceeded.</p>
 *
 * <p>Activate the JPA-backed implementation instead by setting
 * {@code monitor.outbox.jpa.enabled=true}.</p>
 *
 * @see JpaCriticalOutbox
 */
@Component
@ConditionalOnProperty(name = "monitor.outbox.jpa.enabled", havingValue = "false", matchIfMissing = true)
public class InMemoryCriticalOutbox implements CriticalOutbox {

    private static final Logger log = LoggerFactory.getLogger(InMemoryCriticalOutbox.class);

    private final AtomicLong sequence = new AtomicLong(0);
    private final Map<Long, OutboxEntry> entries = new ConcurrentHashMap<>();
    private final Set<Long> pendingIds = ConcurrentHashMap.newKeySet();
    private final ConcurrentLinkedQueue<Long> deliveredHistory = new ConcurrentLinkedQueue<>();

    private final int maxDeliveredEntries;

    /**
     * @param maxDeliveredEntries maximum number of delivered entries to retain in memory
     *                            for SSE replay; controlled by
     *                            {@code monitor.outbox.in-memory.max-delivered-entries}
     *                            (default: 1000)
     */
    public InMemoryCriticalOutbox(
            @Value("${monitor.outbox.in-memory.max-delivered-entries:1000}") int maxDeliveredEntries) {
        this.maxDeliveredEntries = maxDeliveredEntries;
        log.info("Initialized InMemoryCriticalOutbox with maxDeliveredEntries={}", maxDeliveredEntries);
    }

    /**
     * {@inheritDoc}
     *
     * <p>The new entry is immediately added to {@code pendingIds} so that the next
     * {@link #findDue} call can return it for dispatch.</p>
     */
    @Override
    public long save(UnifiedEvent event) {
        long id = sequence.incrementAndGet();
        Instant now = Instant.now();
        OutboxEntry entry = new OutboxEntry(id, event, 0, now, now, false, "");
        
        entries.put(id, entry);
        pendingIds.add(id);

        log.info("CRITICAL EVENT SAVED: id={} type={} source={} message={}",
                id, event.getType(), event.getSeverity(), event.getSource(), event.getMessage());
        
        return id;
    }

    /** {@inheritDoc} */
    @Override
    public Optional<OutboxEntry> find(long id) {
        return Optional.ofNullable(entries.get(id));
    }

    /**
     * {@inheritDoc}
     *
     * <p>Updates the entry in-place (records are immutable; a new record replaces the old one),
     * removes the ID from {@code pendingIds}, and appends it to the delivered-history queue.
     * Evicts the oldest delivered entry from the map when the history queue exceeds the limit.</p>
     */
    @Override
    public void markDelivered(long id) {
        OutboxEntry current = entries.get(id);
        if (current == null || current.delivered()) {
            return;
        }

        OutboxEntry updated = new OutboxEntry(
                current.id(),
                current.event(),
                current.attempts() + 1,
                current.createdAt(),
                Instant.now(),
                true,
                current.lastError()
        );

        entries.put(id, updated);
        pendingIds.remove(id);
        deliveredHistory.offer(id);

        log.info("CRITICAL EVENT DELIVERED: id={} source={} totalAttempts={}",
                id, current.event().getSource(), updated.attempts());

        // Evict old delivered entries if limit reached
        while (deliveredHistory.size() > maxDeliveredEntries) {
            Long oldestId = deliveredHistory.poll();
            if (oldestId != null) {
                entries.remove(oldestId);
                log.trace("Evicted old delivered entry: id={}", oldestId);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void markProcessing(long id) {
        // In-memory implementation is single-node, so explicit processing lock
        // is not strictly required for race prevention, but we implement for interface compliance.
        log.debug("Marking entry {} as processing (in-memory)", id);
    }

    @Override
    public void markRetry(long id, Instant nextAttemptAt, String reason) {
        OutboxEntry current = entries.get(id);
        if (current == null) {
            return;
        }

        OutboxEntry updated = new OutboxEntry(
                current.id(),
                current.event(),
                current.attempts() + 1,
                current.createdAt(),
                nextAttemptAt,
                false,
                reason
        );

        entries.put(id, updated);
        // Ensure it's in pendingIds (should already be there)
        pendingIds.add(id);

        log.warn("CRITICAL EVENT RETRY: id={} attempt={} nextAt={} reason={}",
                id, updated.attempts(), nextAttemptAt, reason);
    }

    /** {@inheritDoc} */
    @Override
    public void resetProcessingToPending(Instant olderThan) {
        // In-memory doesn't track processing state in this implementation yet.
        log.trace("Resetting processing to pending (in-memory) - no-op");
    }

    @Override
    public List<OutboxEntry> findDue(Instant now, int limit) {
        return pendingIds.stream()
                .map(entries::get)
                .filter(entry -> entry != null && !entry.nextAttemptAt().isAfter(now))
                .sorted(Comparator.comparing((OutboxEntry e) -> e.event().getSeverity()).reversed()
                        .thenComparing(OutboxEntry::nextAttemptAt))
                .limit(limit)
                .toList();
    }

    /** {@inheritDoc} */
    @Override
    public List<OutboxEntry> findAfterId(long lastId, int limit) {
        return entries.keySet().stream()
                .filter(id -> id > lastId)
                .sorted()
                .limit(limit)
                .map(entries::get)
                .toList();
    }

    /** {@inheritDoc} */
    @Override
    public long pendingCount() {
        return pendingIds.size();
    }
}
