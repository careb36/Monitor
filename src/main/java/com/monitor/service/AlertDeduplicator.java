package com.monitor.service;

import com.monitor.model.UnifiedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Prevents alert storms by suppressing duplicate events that share the same
 * fingerprint within a configurable sliding time window.
 *
 * <p>A fingerprint is derived from the combination of
 * {@code EventType + Severity + source}, matching the deduplication approach
 * used by Prometheus Alertmanager's {@code group_by} / {@code repeat_interval}.
 * Only the first occurrence within the window is allowed through; subsequent
 * identical alerts are silently dropped with a DEBUG log entry.</p>
 *
 * <p>An internal {@link ConcurrentHashMap} tracks the last-seen timestamp for
 * each fingerprint.  A scheduled cleanup task evicts expired entries so the map
 * does not grow unboundedly.</p>
 */
@Component
public class AlertDeduplicator {

    private static final Logger log = LoggerFactory.getLogger(AlertDeduplicator.class);

    /** Last time each alert fingerprint was allowed through. */
    private final ConcurrentHashMap<String, Instant> seen = new ConcurrentHashMap<>();

    /**
     * Duration (in seconds) during which an identical alert is suppressed.
     * Defaults to 300 s (5 minutes).
     */
    @Value("${monitor.dedup.window-seconds:300}")
    private long windowSeconds;

    /**
     * Checks whether the given event is a duplicate of a recently seen alert.
     *
     * <p>If this is the first occurrence (or the dedup window has expired), the
     * fingerprint timestamp is updated and the method returns {@code false}
     * (not a duplicate).  Otherwise it returns {@code true} and the caller should
     * discard the event.</p>
     *
     * @param event the candidate event
     * @return {@code true} if the event should be suppressed as a duplicate
     */
    public boolean isDuplicate(UnifiedEvent event) {
        String fp = fingerprint(event);
        Instant now = Instant.now();
        Instant last = seen.get(fp);
        if (last != null && Duration.between(last, now).getSeconds() < windowSeconds) {
            log.debug("Duplicate alert suppressed [window={}s]: {}", windowSeconds, fp);
            return true;
        }
        seen.put(fp, now);
        return false;
    }

    /**
     * Scheduled cleanup that removes fingerprints whose dedup window has already
     * expired.  Runs every minute by default to keep the map from growing forever.
     */
    @Scheduled(fixedDelayString = "${monitor.dedup.cleanup-interval-ms:60000}")
    public void evictExpired() {
        Instant cutoff = Instant.now().minusSeconds(windowSeconds);
        int before = seen.size();
        seen.entrySet().removeIf(entry -> entry.getValue().isBefore(cutoff));
        int removed = before - seen.size();
        if (removed > 0) {
            log.debug("Dedup map eviction: removed {} expired fingerprints", removed);
        }
    }

    /**
     * Builds a stable string key that uniquely identifies an alert by its logical
     * identity (type, severity, and source), ignoring volatile fields like timestamp
     * and free-text message.
     */
    private String fingerprint(UnifiedEvent event) {
        return event.getType() + "::" + event.getSeverity() + "::" + event.getSource();
    }

    // ── Package-private helpers for testing ──────────────────────────────────

    /** Sets the dedup window (seconds). Intended for unit tests only. */
    void setWindowSeconds(long windowSeconds) {
        this.windowSeconds = windowSeconds;
    }

    /** Returns the number of fingerprints currently tracked. */
    int trackedCount() {
        return seen.size();
    }
}
