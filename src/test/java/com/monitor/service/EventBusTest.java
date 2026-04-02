package com.monitor.service;

import com.monitor.model.EventType;
import com.monitor.model.Severity;
import com.monitor.model.UnifiedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class EventBusTest {

    private EventBus eventBus;

    // A simple fake emitter that counts calls to avoid Mockito/ByteBuddy issues on Java 25
    private static class FakeSseEmitter extends SseEmitter {
        private final AtomicInteger sendCount = new AtomicInteger(0);
        private final boolean shouldFailOnSecondCall;

        FakeSseEmitter(Long timeout, boolean shouldFailOnSecondCall) {
            super(timeout);
            this.shouldFailOnSecondCall = shouldFailOnSecondCall;
        }

        @Override
        public void send(SseEventBuilder builder) throws IOException {
            int current = sendCount.incrementAndGet();
            if (shouldFailOnSecondCall && current == 2) {
                throw new IOException("disconnected");
            }
        }

        int getSendCount() {
            return sendCount.get();
        }
    }

    @BeforeEach
    void setUp() {
        eventBus = new EventBus();
    }

    @Test
    void addEmitter_doesNotThrow() {
        FakeSseEmitter emitter = new FakeSseEmitter(1000L, false);
        assertDoesNotThrow(() -> eventBus.addEmitter(emitter));
    }

    @Test
    void addEmitter_sendsInitialHeartbeat() {
        FakeSseEmitter emitter = new FakeSseEmitter(5000L, false);
        eventBus.addEmitter(emitter);
        
        // Initial heartbeat is synchronous in addEmitter
        assertEquals(1, emitter.getSendCount());
    }

    @Test
    void sendHeartbeat_toRegisteredEmitter_sendsPeriodicHeartbeat() {
        FakeSseEmitter emitter = new FakeSseEmitter(5000L, false);
        eventBus.addEmitter(emitter);
        
        eventBus.sendHeartbeat();

        // Wait for the async broadcast via Virtual Threads (Total: initial + heartbeat)
        await().atMost(2, TimeUnit.SECONDS).until(() -> emitter.getSendCount() == 2);
    }

    @Test
    void publish_withNoEmitters_doesNotThrow() {
        UnifiedEvent event = new UnifiedEvent(EventType.INFRASTRUCTURE, Severity.INFO, "test", "all ok");
        assertDoesNotThrow(() -> eventBus.publish(event));
    }

    @Test
    void publish_toRegisteredEmitter_sendsEvent() {
        FakeSseEmitter emitter = new FakeSseEmitter(5000L, false);
        eventBus.addEmitter(emitter);
        
        UnifiedEvent event = new UnifiedEvent(EventType.DATA, Severity.CRITICAL, "src", "msg");
        eventBus.publish(event);

        // Wait for the async broadcast (Total: initial + publish)
        await().atMost(2, TimeUnit.SECONDS).until(() -> emitter.getSendCount() == 2);
    }

    @Test
    void sendHeartbeat_removesDisconnectedEmitter() {
        FakeSseEmitter emitter = new FakeSseEmitter(5000L, true);
        eventBus.addEmitter(emitter); // Calls send (1)
        
        eventBus.sendHeartbeat(); // Fails on send (2)
        
        // Wait for removal and verify that a subsequent publish doesn't increment count further
        await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> {
            eventBus.publish(new UnifiedEvent(EventType.DATA, Severity.INFO, "src", "msg"));
            // If it was removed, count stays at 2 (failed attempt included)
            assertEquals(2, emitter.getSendCount());
        });
    }
}
