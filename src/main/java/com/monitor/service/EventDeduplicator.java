package com.monitor.service;

import com.monitor.model.UnifiedEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Time-window deduplication to reduce alert storms from noisy targets.
 */
@Component
public class EventDeduplicator {

    private final Duration ttl;
    private final Map<String, Instant> seenEvents = new ConcurrentHashMap<>();

    public EventDeduplicator(@Value("${monitor.pipeline.dedup-ttl-ms:60000}") long ttlMs) {
        this.ttl = Duration.ofMillis(ttlMs);
    }

    public boolean isDuplicate(UnifiedEvent event) {
        Instant now = Instant.now();
        String key = buildKey(event);
        Instant lastSeen = seenEvents.get(key);

        if (lastSeen != null && Duration.between(lastSeen, now).compareTo(ttl) < 0) {
            return true;
        }

        seenEvents.put(key, now);
        cleanup(now);
        return false;
    }

    private String buildKey(UnifiedEvent event) {
        return String.join("|",
                String.valueOf(event.getType()),
                String.valueOf(event.getSeverity()),
                String.valueOf(event.getSource()),
                String.valueOf(event.getMessage()));
    }

    private void cleanup(Instant now) {
        for (Map.Entry<String, Instant> entry : seenEvents.entrySet()) {
            if (Duration.between(entry.getValue(), now).compareTo(ttl) >= 0) {
                seenEvents.remove(entry.getKey(), entry.getValue());
            }
        }
    }
}
