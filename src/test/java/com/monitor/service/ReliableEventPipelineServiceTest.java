package com.monitor.service;

import com.monitor.model.EventType;
import com.monitor.model.IngestMetadata;
import com.monitor.model.Severity;
import com.monitor.model.UnifiedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReliableEventPipelineServiceTest {

    private EventDeduplicator deduplicator;
    private InMemoryCriticalOutbox outbox;

    @BeforeEach
    void setUp() {
        deduplicator = new EventDeduplicator(60000);
        outbox = new InMemoryCriticalOutbox();
    }

    @Test
    void ingest_duplicateInfoEvent_isDroppedByDeduplicator() {
        List<UnifiedEvent> sseDelivered = new CopyOnWriteArrayList<>();
        EventNotifier sse = new TestNotifier("sse", sseDelivered, true);
        EventNotifier email = new TestNotifier("email", List.of(), true);

        ReliableEventPipelineService pipeline = new ReliableEventPipelineService(
                deduplicator, outbox, sse, email, 10, 1000, 5, 20);

        UnifiedEvent info = new UnifiedEvent(EventType.INFRASTRUCTURE, Severity.INFO, "db-1", "UP");
        pipeline.ingest(info, IngestMetadata.live("polling:db-1"));
        pipeline.ingest(info, IngestMetadata.live("polling:db-1"));

        pipeline.dispatchStandard();

        assertEquals(1, sseDelivered.size());
    }

    @Test
    void dispatchCritical_whenEmailFails_marksRetryAndEventuallyDelivers() {
        AtomicBoolean mailHealthy = new AtomicBoolean(false);
        EventNotifier sse = new TestNotifier("sse", new CopyOnWriteArrayList<>(), true);
        EventNotifier email = new EventNotifier() {
            @Override
            public String channel() { return "email"; }

            @Override
            public boolean notify(UnifiedEvent event) {
                if (event.getSeverity() != Severity.CRITICAL) {
                    return true;
                }
                return mailHealthy.get();
            }
        };

        ReliableEventPipelineService pipeline = new ReliableEventPipelineService(
                deduplicator, outbox, sse, email, 10, 1, 5, 20);

        UnifiedEvent critical = new UnifiedEvent(EventType.DATA, Severity.CRITICAL, "kafka:err", "fatal");
        pipeline.ingest(critical, IngestMetadata.live("kafka:topic"));

        pipeline.dispatchCritical();
        OutboxEntry first = outbox.find(1L).orElseThrow();
        assertFalse(first.delivered());
        assertTrue(first.attempts() >= 1);

        // Make SMTP healthy and fast-forward eligibility.
        mailHealthy.set(true);
        outbox.markRetry(1L, Instant.now().minusSeconds(1), "manual-test-retry");

        pipeline.replayCriticalDue();
        pipeline.dispatchCritical();

        OutboxEntry delivered = outbox.find(1L).orElseThrow();
        assertTrue(delivered.delivered());
    }

    private record TestNotifier(String channel, List<UnifiedEvent> sink, boolean result) implements EventNotifier {

        @Override
        public boolean notify(UnifiedEvent event) {
            sink.add(event);
            return result;
        }
    }
}
