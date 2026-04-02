package com.monitor.service;

import com.monitor.model.EventType;
import com.monitor.model.Severity;
import com.monitor.model.UnifiedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class CriticalReplayServiceTest {

    @Test
    void replaySince_withValidLastEventId_replaysMissedCriticalEvents() {
        CriticalOutbox outbox = mock(CriticalOutbox.class);
        EventBus eventBus = mock(EventBus.class);
        SseEmitter emitter = new SseEmitter(1000L);

        UnifiedEvent event = new UnifiedEvent(EventType.DATA, Severity.CRITICAL, "src", "msg");
        event.setTimestamp(Instant.now());
        OutboxEntry entry = new OutboxEntry(11L, event, 1, Instant.now(), Instant.now(), true, "");

        when(outbox.findAfterId(10L, 50)).thenReturn(List.of(entry));
        when(eventBus.publishToEmitter(eq(emitter), eq(event), eq("11"))).thenReturn(true);

        CriticalReplayService replayService = new CriticalReplayService(outbox, eventBus, 50, Duration.ofMinutes(5));
        replayService.replaySince("10", emitter);

        verify(outbox).findAfterId(10L, 50);
        verify(eventBus).publishToEmitter(emitter, event, "11");
    }

    @Test
    void pollAndLockDueEntries_withConcurrency_handlesOptimisticLockingAndAvoidsDuplicates() throws InterruptedException {
        // Arrange
        CriticalOutbox outbox = mock(CriticalOutbox.class);
        EventBus eventBus = mock(EventBus.class);
        CriticalReplayService replayService = new CriticalReplayService(outbox, eventBus, 50, Duration.ofMinutes(5));

        int numThreads = 10;
        int numEntries = 20;
        Queue<Long> criticalQueue = new ConcurrentLinkedQueue<>();
        
        List<OutboxEntry> dueEntries = new ArrayList<>();
        for (long i = 1; i <= numEntries; i++) {
            UnifiedEvent event = new UnifiedEvent(EventType.DATA, Severity.CRITICAL, "src", "msg " + i);
            dueEntries.add(new OutboxEntry(i, event, 0, Instant.now(), Instant.now(), false, ""));
        }

        // Each thread will see the same due entries
        when(outbox.findDue(any(Instant.class), anyInt())).thenReturn(dueEntries);

        // Atomic counter to track how many times markProcessing was successfully called per entry
        AtomicInteger successfulLocks = new AtomicInteger(0);
        
        // Use a set to track which entries were already "locked" by a thread in this mock session
        java.util.Set<Long> lockedEntries = java.util.concurrent.ConcurrentHashMap.newKeySet();

        doAnswer(invocation -> {
            Long id = invocation.getArgument(0);
            if (!lockedEntries.add(id)) {
                throw new ObjectOptimisticLockingFailureException("Conflict", new RuntimeException());
            }
            successfulLocks.incrementAndGet();
            return null;
        }).when(outbox).markProcessing(anyLong());

        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);

        // Act
        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                try {
                    replayService.pollAndLockDueEntries(criticalQueue, numEntries);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        // Assert
        // Each entry should have been locked exactly once across all threads
        assertEquals(numEntries, successfulLocks.get(), "Total successful locks should match number of entries");
        assertEquals(numEntries, criticalQueue.size(), "Queue should contain each entry exactly once");
        
        // Verify outbox.markProcessing was called many times (some failed)
        verify(outbox, atLeast(numEntries)).markProcessing(anyLong());
    }

    @Test
    void replaySince_withInvalidLastEventId_doesNothing() {
        CriticalOutbox outbox = mock(CriticalOutbox.class);
        EventBus eventBus = mock(EventBus.class);

        CriticalReplayService replayService = new CriticalReplayService(outbox, eventBus, 50, Duration.ofMinutes(5));
        replayService.replaySince("abc", new SseEmitter(1000L));

        verify(outbox, never()).findAfterId(anyLong(), anyInt());
        verify(eventBus, never()).publishToEmitter(any(), any(), any());
    }
}
