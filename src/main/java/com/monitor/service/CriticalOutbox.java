package com.monitor.service;

import com.monitor.model.UnifiedEvent;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface CriticalOutbox {

    long save(UnifiedEvent event);

    Optional<OutboxEntry> find(long id);

    void markDelivered(long id);

    void markRetry(long id, Instant nextAttemptAt, String reason);

    List<OutboxEntry> findDue(Instant now, int limit);

    List<OutboxEntry> findAfterId(long lastId, int limit);

    long pendingCount();
}
