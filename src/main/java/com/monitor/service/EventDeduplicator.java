package com.monitor.service;

import com.monitor.model.UnifiedEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Time-window deduplication gate that reduces alert storms from noisy event sources.
 *
 * <p>Unlike {@link AlertDeduplicator} (which operates on the legacy path and uses a
 * coarse-grained fingerprint of type + severity + source), this deduplicator builds a
 * finer-grained key that also includes the event message. It is used by
 * {@link ReliableEventPipelineService} as the first stage of the ingestion pipeline.</p>
 *
 * <p>A deduplication TTL is configurable via {@code monitor.pipeline.dedup-ttl-ms}
 * (default 60 000 ms / 1 minute). Within the TTL window, identical events are silently
 * dropped. After the window expires the event is allowed through and the TTL resets.</p>
 *
 * <p>The internal map is cleaned up inline on every {@link #isDuplicate} call to avoid
 * unbounded growth without requiring a dedicated scheduler thread.</p>
 */
@Component
public class EventDeduplicator {

    private final Duration ttl;
    private final Map<String, Instant> seenEvents = new ConcurrentHashMap<>();

    /**
     * @param ttlMs deduplication window in milliseconds; controlled by
     *              {@code monitor.pipeline.dedup-ttl-ms} (default: 60 000)
     */
    public EventDeduplicator(@Value("${monitor.pipeline.dedup-ttl-ms:60000}") long ttlMs) {
        this.ttl = Duration.ofMillis(ttlMs);
    }

    /**
     * Checks whether the given event is a duplicate of one seen within the TTL window.
     *
     * <p>If this is the first occurrence (or the TTL has expired), the event's key is
     * recorded and {@code false} is returned. Subsequent identical events within the
     * window return {@code true} and are expected to be discarded by the caller.</p>
     *
     * @param event the candidate event; must not be {@code null}
     * @return {@code true} if the event is a duplicate and should be dropped
     */
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

    /**
     * Builds a composite deduplication key from all identifying fields of the event.
     * Including the message (unlike {@link AlertDeduplicator}) allows distinct error
     * messages from the same source to be treated as separate events.
     */
    private String buildKey(UnifiedEvent event) {
        return String.join("|",
                String.valueOf(event.getType()),
                String.valueOf(event.getSeverity()),
                String.valueOf(event.getSource()),
                String.valueOf(event.getMessage()));
    }

    /**
     * Inline eviction of entries whose TTL has expired.
     * Called on every {@link #isDuplicate} invocation to keep the map bounded
     * without a separate scheduler.
     *
     * @param now the current instant used as the eviction reference point
     */
    private void cleanup(Instant now) {
        for (Map.Entry<String, Instant> entry : seenEvents.entrySet()) {
            if (Duration.between(entry.getValue(), now).compareTo(ttl) >= 0) {
                seenEvents.remove(entry.getKey(), entry.getValue());
            }
        }
    }
}
