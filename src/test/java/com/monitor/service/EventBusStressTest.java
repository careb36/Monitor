package com.monitor.service;

import com.monitor.model.EventType;
import com.monitor.model.Severity;
import com.monitor.model.UnifiedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EventBusStressTest {

    private static final Logger log = LoggerFactory.getLogger(EventBusStressTest.class);
    private static final int CLIENT_COUNT = 10000;
    private static final int EVENT_COUNT = 5;

    @Test
    @DisplayName("Stress test: 10,000 clients should receive events via Virtual Threads without blocking")
    void stressTest10kClients() throws InterruptedException {
        EventBus eventBus = new EventBus();
        AtomicInteger receivedCount = new AtomicInteger(0);
        // Each addEmitter sends 1 heartbeat, plus EVENT_COUNT broadcasts per client
        int expectedTotal = CLIENT_COUNT + (CLIENT_COUNT * EVENT_COUNT);
        CountDownLatch latch = new CountDownLatch(expectedTotal);

        log.info("Registering {} clients...", CLIENT_COUNT);
        Instant startRegister = Instant.now();
        
        for (int i = 0; i < CLIENT_COUNT; i++) {
            SseEmitter emitter = mock(SseEmitter.class);
            try {
                doAnswer(invocation -> {
                    receivedCount.incrementAndGet();
                    latch.countDown();
                    return null;
                }).when(emitter).send(any(SseEmitter.SseEventBuilder.class));
            } catch (Exception e) {
                log.error("Failed to mock emitter", e);
            }
            eventBus.addEmitter(emitter);
        }
        
        log.info("Registration of {} clients took {}ms", CLIENT_COUNT, Duration.between(startRegister, Instant.now()).toMillis());

        UnifiedEvent event = new UnifiedEvent(
                EventType.DATA,
                Severity.CRITICAL,
                "system",
                "Stress test event"
        );

        log.info("Broadcasting {} events to {} clients...", EVENT_COUNT, CLIENT_COUNT);
        Instant startBroadcast = Instant.now();
        
        for (int i = 0; i < EVENT_COUNT; i++) {
            eventBus.publish(event);
        }

        // Wait for all virtual threads to finish their work
        boolean completed = latch.await(20, TimeUnit.SECONDS);
        long duration = Duration.between(startBroadcast, Instant.now()).toMillis();
        
        log.info("Broadcast of {} events to {} clients took {}ms", EVENT_COUNT, CLIENT_COUNT, duration);
        log.info("Total received calls: {}", receivedCount.get());

        assertTrue(completed, "Broadcast should complete within timeout. Remaining: " + latch.getCount());
        assertEquals(expectedTotal, receivedCount.get(), "All clients should have received all events (including initial heartbeat)");
        
        log.info("Stress test passed successfully.");
    }
}
