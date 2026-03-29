package com.monitor.service;

import com.monitor.model.EventType;
import com.monitor.model.Severity;
import com.monitor.model.UnifiedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class EventBusTest {

    private EventBus eventBus;

    @BeforeEach
    void setUp() {
        // Use a real AlertDeduplicator with a zero-second window so dedup never
        // suppresses events during these unit tests (keeps tests independent of time).
        AlertDeduplicator deduplicator = new AlertDeduplicator();
        deduplicator.setWindowSeconds(0);
        eventBus = new EventBus(deduplicator, 1000);
        eventBus.startDrainer();
    }

    @Test
    void addEmitter_doesNotThrow() {
        SseEmitter emitter = new SseEmitter(1000L);
        assertDoesNotThrow(() -> eventBus.addEmitter(emitter));
    }

    @Test
    void addEmitter_sendsInitialHeartbeat() {
        List<Object> received = new ArrayList<>();
        SseEmitter emitter = new SseEmitter(5000L) {
            @Override
            public void send(SseEventBuilder builder) throws IOException {
                received.add(builder);
            }
        };

        eventBus.addEmitter(emitter);

        assertEquals(1, received.size());
    }

    @Test
    void sendHeartbeat_toRegisteredEmitter_sendsPeriodicHeartbeat() {
        List<Object> received = new ArrayList<>();
        SseEmitter emitter = new SseEmitter(5000L) {
            @Override
            public void send(SseEventBuilder builder) throws IOException {
                received.add(builder);
            }
        };

        eventBus.addEmitter(emitter);
        eventBus.sendHeartbeat();

        assertEquals(2, received.size());
    }

    @Test
    void publish_withNoEmitters_doesNotThrow() {
        UnifiedEvent event = new UnifiedEvent(EventType.INFRASTRUCTURE, Severity.INFO, "test", "all ok");
        assertDoesNotThrow(() -> eventBus.publish(event));
    }

    @Test
    void publish_toRegisteredEmitter_sendsEvent() {
        List<Object> received = new ArrayList<>();
        SseEmitter emitter = new SseEmitter(5000L) {
            @Override
            public void send(SseEventBuilder builder) throws IOException {
                received.add(builder);
            }
        };
        eventBus.addEmitter(emitter);

        UnifiedEvent event = new UnifiedEvent(EventType.DATA, Severity.CRITICAL, "src", "msg");
        eventBus.publish(event);

        assertEquals(2, received.size());
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
        assertDoesNotThrow(() -> eventBus.publish(
                new UnifiedEvent(EventType.DATA, Severity.INFO, "src", "msg")));
    }
}
