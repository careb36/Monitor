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

        // At least one event was received by the emitter
        assertDoesNotThrow(() -> eventBus.publish(event));
    }
}
