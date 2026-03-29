package com.monitor.service;

import com.monitor.model.UnifiedEvent;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class InMemoryCriticalOutbox implements CriticalOutbox {

    private final AtomicLong sequence = new AtomicLong(0);
    private final Map<Long, OutboxEntry> entries = new ConcurrentHashMap<>();

    @Override
    public long save(UnifiedEvent event) {
        long id = sequence.incrementAndGet();
        Instant now = Instant.now();
        entries.put(id, new OutboxEntry(id, event, 0, now, now, false, ""));
        return id;
    }

    @Override
    public Optional<OutboxEntry> find(long id) {
        return Optional.ofNullable(entries.get(id));
    }

    @Override
    public void markDelivered(long id) {
        OutboxEntry current = entries.get(id);
        if (current == null) {
            return;
        }
        entries.put(id, new OutboxEntry(
                current.id(),
                current.event(),
                current.attempts() + 1,
                current.createdAt(),
                Instant.now(),
                true,
                current.lastError()
        ));
    }

    @Override
    public void markRetry(long id, Instant nextAttemptAt, String reason) {
        OutboxEntry current = entries.get(id);
        if (current == null) {
            return;
        }
        entries.put(id, new OutboxEntry(
                current.id(),
                current.event(),
                current.attempts() + 1,
                current.createdAt(),
                nextAttemptAt,
                false,
                reason
        ));
    }

    @Override
    public List<OutboxEntry> findDue(Instant now, int limit) {
        return entries.values().stream()
                .filter(entry -> !entry.delivered())
                .filter(entry -> !entry.nextAttemptAt().isAfter(now))
                .sorted(Comparator.comparing(OutboxEntry::nextAttemptAt))
                .limit(limit)
                .toList();
    }
}

