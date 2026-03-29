package com.monitor.service;

import com.monitor.model.EventType;
import com.monitor.model.Severity;
import com.monitor.model.UnifiedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AlertDeduplicatorTest {

    private AlertDeduplicator deduplicator;

    @BeforeEach
    void setUp() {
        deduplicator = new AlertDeduplicator();
        // 300-second window by default (set via setter for unit tests)
        deduplicator.setWindowSeconds(300);
    }

    @Test
    void isDuplicate_firstOccurrence_returnsFalse() {
        UnifiedEvent event = new UnifiedEvent(
                EventType.INFRASTRUCTURE, Severity.CRITICAL, "oracle-01", "is DOWN");

        assertFalse(deduplicator.isDuplicate(event), "First occurrence must not be treated as duplicate");
    }

    @Test
    void isDuplicate_sameEventWithinWindow_returnsTrue() {
        UnifiedEvent event = new UnifiedEvent(
                EventType.INFRASTRUCTURE, Severity.CRITICAL, "oracle-01", "is DOWN");

        deduplicator.isDuplicate(event); // first call — registers fingerprint
        assertTrue(deduplicator.isDuplicate(event), "Second occurrence within window must be a duplicate");
    }

    @Test
    void isDuplicate_differentSource_notDuplicate() {
        UnifiedEvent event1 = new UnifiedEvent(
                EventType.INFRASTRUCTURE, Severity.CRITICAL, "oracle-01", "is DOWN");
        UnifiedEvent event2 = new UnifiedEvent(
                EventType.INFRASTRUCTURE, Severity.CRITICAL, "oracle-02", "is DOWN");

        deduplicator.isDuplicate(event1);
        assertFalse(deduplicator.isDuplicate(event2), "Different source must not be suppressed");
    }

    @Test
    void isDuplicate_differentSeverity_notDuplicate() {
        UnifiedEvent critical = new UnifiedEvent(
                EventType.DATA, Severity.CRITICAL, "log_traza [ERR-001]", "Fatal");
        UnifiedEvent warning = new UnifiedEvent(
                EventType.DATA, Severity.WARNING, "log_traza [ERR-001]", "Warning");

        deduplicator.isDuplicate(critical);
        assertFalse(deduplicator.isDuplicate(warning), "Different severity must not be suppressed");
    }

    @Test
    void isDuplicate_afterWindowExpires_notDuplicate() {
        UnifiedEvent event = new UnifiedEvent(
                EventType.INFRASTRUCTURE, Severity.WARNING, "batch-processor", "slow");

        // Use a zero-second window so the window expires immediately
        deduplicator.setWindowSeconds(0);

        deduplicator.isDuplicate(event); // register
        assertFalse(deduplicator.isDuplicate(event),
                "Event should NOT be a duplicate when window is 0 seconds (already expired)");
    }

    @Test
    void evictExpired_removesOnlyExpiredEntries() {
        // Window of 300 s — entries are still within the window after eviction
        UnifiedEvent event = new UnifiedEvent(
                EventType.DATA, Severity.INFO, "live-source", "msg");
        deduplicator.isDuplicate(event); // register one fingerprint

        assertEquals(1, deduplicator.trackedCount(), "One fingerprint should be tracked");

        deduplicator.evictExpired(); // should NOT remove the still-valid entry

        assertEquals(1, deduplicator.trackedCount(),
                "Valid fingerprint should survive eviction within its window");
    }

    @Test
    void evictExpired_removesExpiredEntries() {
        deduplicator.setWindowSeconds(0); // window is 0 s — entries expire immediately
        UnifiedEvent event = new UnifiedEvent(
                EventType.DATA, Severity.INFO, "expired-source", "msg");
        deduplicator.isDuplicate(event); // register

        deduplicator.evictExpired(); // should remove the expired entry

        assertEquals(0, deduplicator.trackedCount(),
                "Expired fingerprint should be removed by eviction");
    }
}
