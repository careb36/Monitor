package com.monitor.service;

import com.monitor.model.EventType;
import com.monitor.model.Severity;
import com.monitor.model.UnifiedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class EventBusTest {

    private EventBus eventBus;

    @BeforeEach
    void setUp() {
        eventBus = new EventBus();
    }

    @Test
    void addEmitter_doesNotThrow() {
        SseEmitter emitter = new SseEmitter(1000L);
        assertDoesNotThrow(() -> eventBus.addEmitter(emitter));
    }

    @Test
    void addEmitter_sendsInitialHeartbeat() {
        List<Object> received = new CopyOnWriteArrayList<>();
        SseEmitter emitter = new SseEmitter(5000L) {
            @Override
            public void send(SseEventBuilder builder) throws IOException {
                received.add(builder);
            }
        };

        eventBus.addEmitter(emitter);

        // This one is synchronous in addEmitter, but let's be safe
        await().atMost(2, TimeUnit.SECONDS).until(() -> received.size() == 1);
    }

    @Test
    void sendHeartbeat_toRegisteredEmitter_sendsPeriodicHeartbeat() {
        List<Object> received = new CopyOnWriteArrayList<>();
        SseEmitter emitter = new SseEmitter(5000L) {
            @Override
            public void send(SseEventBuilder builder) throws IOException {
                received.add(builder);
            }
        };

        eventBus.addEmitter(emitter);
        eventBus.sendHeartbeat();

        // Wait for the async broadcast via Virtual Threads
        await().atMost(2, TimeUnit.SECONDS).until(() -> received.size() == 2);
    }

    @Test
    void publish_withNoEmitters_doesNotThrow() {
        UnifiedEvent event = new UnifiedEvent(EventType.INFRASTRUCTURE, Severity.INFO, "test", "all ok");
        assertDoesNotThrow(() -> eventBus.publish(event));
    }

    @Test
    void publish_toRegisteredEmitter_sendsEvent() {
        List<Object> received = new CopyOnWriteArrayList<>();
        SseEmitter emitter = new SseEmitter(5000L) {
            @Override
            public void send(SseEventBuilder builder) throws IOException {
                received.add(builder);
            }
        };
        eventBus.addEmitter(emitter);

        UnifiedEvent event = new UnifiedEvent(EventType.DATA, Severity.CRITICAL, "src", "msg");
        eventBus.publish(event);

        // Wait for the async broadcast via Virtual Threads
        await().atMost(2, TimeUnit.SECONDS).until(() -> received.size() == 2);
    }

    @Test
    void sendHeartbeat_removesDisconnectedEmitter() {
        SseEmitter emitter = new SseEmitter(5000L) {
            private boolean firstSend = true;

            @Override
            public void send(SseEventBuilder builder) throws IOException {
                if (firstSend) {
                    firstSend = false;
                    return;
                }
                throw new IOException("disconnected");
            }
        };

        eventBus.addEmitter(emitter);

        assertDoesNotThrow(() -> eventBus.sendHeartbeat());
        
        // Wait a bit for the async removal to potentially happen
        await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> 
            assertDoesNotThrow(() -> eventBus.publish(
                new UnifiedEvent(EventType.DATA, Severity.INFO, "src", "msg")))
        );
    }
}
