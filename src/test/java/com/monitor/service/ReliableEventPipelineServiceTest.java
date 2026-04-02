package com.monitor.service;

import com.monitor.model.EventType;
import com.monitor.model.IngestMetadata;
import com.monitor.model.Severity;
import com.monitor.model.UnifiedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class ReliableEventPipelineServiceTest {

    private EventDeduplicator deduplicator;
    private InMemoryCriticalOutbox outbox;
    private CriticalReplayService replayService;

    @BeforeEach
    void setUp() {
        deduplicator = new EventDeduplicator(60000);
        outbox = new InMemoryCriticalOutbox(100);
        replayService = new CriticalReplayService(outbox, mock(EventBus.class), 50, Duration.ofMinutes(5));
    }

    @Test
    void ingest_duplicateInfoEvent_isDroppedByDeduplicator() {
        List<UnifiedEvent> sseDelivered = new CopyOnWriteArrayList<>();
        EventNotifier sse = new TestNotifier("sse", sseDelivered, true);
        EventNotifier email = new TestNotifier("email", List.of(), true);

        ReliableEventPipelineService pipeline = new ReliableEventPipelineService(
                deduplicator, outbox, replayService, sse, email, 10, 1000, 5, 20);

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
                deduplicator, outbox, replayService, sse, email, 10, 1, 5, 20);

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

    @Test
    void dispatchCritical_whenRetriesExhausted_marksRetryLimitReached() {
        EventNotifier sse = new TestNotifier("sse", new CopyOnWriteArrayList<>(), true);
        EventNotifier email = new TestNotifier("email", new CopyOnWriteArrayList<>(), false); // Email always fails

        // Limit to 3 retries
        ReliableEventPipelineService pipeline = new ReliableEventPipelineService(
                deduplicator, outbox, replayService, sse, email, 10, 1, 3, 20);

        UnifiedEvent critical = new UnifiedEvent(EventType.DATA, Severity.CRITICAL, "exhaust-test", "fail");
        pipeline.ingest(critical, IngestMetadata.live("test"));

        // Process until limit (3) is reached.
        for (int i = 0; i < 5; i++) {
            pipeline.dispatchCritical();
            // Let a tiny bit of time pass for the 1ms delay to expire naturally
            try { Thread.sleep(5); } catch (InterruptedException e) {}
            // Manually trigger replay to put the failed event back into the dispatch queue
            pipeline.replayCriticalDue();
        }

        OutboxEntry exhausted = outbox.find(1L).orElseThrow();
        assertFalse(exhausted.delivered());
        assertEquals("retry-limit-reached", exhausted.lastError());
    }

    private record TestNotifier(String channel, List<UnifiedEvent> sink, boolean result) implements EventNotifier {

        @Override
        public boolean notify(UnifiedEvent event) {
            sink.add(event);
            return result;
        }
    }
}
