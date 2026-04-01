package com.monitor.service;

import com.monitor.model.EventType;
import com.monitor.model.Severity;
import com.monitor.model.UnifiedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryCriticalOutboxTest {

    private InMemoryCriticalOutbox outbox;
    private static final int MAX_DELIVERED = 5;

    @BeforeEach
    void setUp() {
        outbox = new InMemoryCriticalOutbox(MAX_DELIVERED);
    }

    @Test
    void save_shouldIncrementIdAndStoreEvent() {
        UnifiedEvent event = createEvent("test-1");
        long id = outbox.save(event);
        
        Optional<OutboxEntry> entry = outbox.find(id);
        assertTrue(entry.isPresent());
        assertEquals(event, entry.get().event());
        assertEquals(1, outbox.pendingCount());
    }

    @Test
    void markDelivered_shouldRemoveFromPendingAndTrackHistory() {
        long id = outbox.save(createEvent("test-1"));
        outbox.markDelivered(id);
        
        assertEquals(0, outbox.pendingCount());
        assertTrue(outbox.find(id).get().delivered());
    }

    @Test
    void memoryProtection_shouldEvictOldestDeliveredEntries() {
        for (int i = 1; i <= MAX_DELIVERED; i++) {
            long id = outbox.save(createEvent("event-" + i));
            outbox.markDelivered(id);
        }

        assertEquals(MAX_DELIVERED, outbox.findAfterId(0, 100).size());

        long idExtra = outbox.save(createEvent("event-extra"));
        outbox.markDelivered(idExtra);

        assertTrue(outbox.find(1L).isEmpty(), "First entry should be evicted");
        assertTrue(outbox.find(idExtra).isPresent());
        assertEquals(MAX_DELIVERED, outbox.findAfterId(0, 100).size());
    }

    @Test
    void memoryProtection_shouldNeverEvictPendingEntries() {
        for (int i = 1; i <= MAX_DELIVERED * 2; i++) {
            outbox.save(createEvent("pending-" + i));
        }

        assertEquals(MAX_DELIVERED * 2, outbox.pendingCount());
        
        for (int i = 1; i <= MAX_DELIVERED; i++) {
            outbox.markDelivered(i);
        }
        
        assertEquals(MAX_DELIVERED * 2, outbox.findAfterId(0, 100).size());
        
        long idExtra = outbox.save(createEvent("extra-delivered"));
        outbox.markDelivered(idExtra);
        
        assertTrue(outbox.find(1L).isEmpty());
        assertTrue(outbox.find(MAX_DELIVERED + 1).isPresent());
        assertEquals(MAX_DELIVERED * 2, outbox.findAfterId(0, 100).size());
    }

    @Test
    void findDue_shouldOnlyReturnPendingEventsEligibleForRetry() {
        long id1 = outbox.save(createEvent("due-now"));
        long id2 = outbox.save(createEvent("due-future"));
        
        outbox.markRetry(id2, Instant.now().plusSeconds(60), "wait");
        
        List<OutboxEntry> due = outbox.findDue(Instant.now(), 10);
        assertEquals(1, due.size());
        assertEquals(id1, due.get(0).id());
    }

    @Test
    void findDue_shouldPrioritizeCriticalOverInfo() {
        long idInfo = outbox.save(new UnifiedEvent(EventType.DATA, Severity.INFO, "src", "info-msg"));
        long idCritical = outbox.save(new UnifiedEvent(EventType.DATA, Severity.CRITICAL, "src", "crit-msg"));

        List<OutboxEntry> due = outbox.findDue(Instant.now(), 10);

        assertEquals(2, due.size());
        assertEquals(idCritical, due.get(0).id());
        assertEquals(idInfo, due.get(1).id());
    }

    @Test
    void retryExhaustion_shouldStayInPendingButNotBeDueUntilLongFuture() {
        long id = outbox.save(createEvent("exhausted"));
        Instant longFuture = Instant.now().plus(java.time.Duration.ofHours(24));
        
        outbox.markRetry(id, longFuture, "retry-limit-reached");
        
        List<OutboxEntry> due = outbox.findDue(Instant.now(), 10);
        assertTrue(due.isEmpty());
        
        Optional<OutboxEntry> entry = outbox.find(id);
        assertTrue(entry.isPresent());
        assertEquals("retry-limit-reached", entry.get().lastError());
    }

    private UnifiedEvent createEvent(String msg) {
        return new UnifiedEvent(EventType.DATA, Severity.CRITICAL, "source", msg);
    }
}
