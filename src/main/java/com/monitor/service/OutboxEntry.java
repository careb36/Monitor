package com.monitor.service;

import com.monitor.model.UnifiedEvent;

import java.time.Instant;

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
