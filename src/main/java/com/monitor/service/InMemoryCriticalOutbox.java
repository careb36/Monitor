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
 * Optimized in-memory implementation of CriticalOutbox.
 * Uses a bounded buffer for delivered entries to prevent memory leaks.
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

    public InMemoryCriticalOutbox(
            @Value("${monitor.outbox.in-memory.max-delivered-entries:1000}") int maxDeliveredEntries) {
        this.maxDeliveredEntries = maxDeliveredEntries;
        log.info("Initialized InMemoryCriticalOutbox with maxDeliveredEntries={}", maxDeliveredEntries);
    }

    @Override
    public long save(UnifiedEvent event) {
        long id = sequence.incrementAndGet();
        Instant now = Instant.now();
        OutboxEntry entry = new OutboxEntry(id, event, 0, now, now, false, "");
        
        entries.put(id, entry);
        pendingIds.add(id);

        log.info("CRITICAL EVENT SAVED: id={} type={} source={} message={}",
                id, event.getType(), event.getSource(), event.getMessage());
        
        return id;
    }

    @Override
    public Optional<OutboxEntry> find(long id) {
        return Optional.ofNullable(entries.get(id));
    }

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

    @Override
    public List<OutboxEntry> findAfterId(long lastId, int limit) {
        return entries.keySet().stream()
                .filter(id -> id > lastId)
                .sorted()
                .limit(limit)
                .map(entries::get)
                .toList();
    }

    @Override
    public long pendingCount() {
        return pendingIds.size();
    }
}
