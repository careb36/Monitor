package com.monitor.service;

import com.monitor.model.UnifiedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Central in-memory event bus that decouples event producers (Kafka, Polling)
 * from SSE consumers. Each connected browser client gets its own {@link SseEmitter}.
 */
@Component
public class EventBus {

    private static final Logger log = LoggerFactory.getLogger(EventBus.class);

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    /**
     * Registers a new SSE client emitter.
     *
     * @param emitter the emitter created by the SSE controller
     */
    public void addEmitter(SseEmitter emitter) {
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));
        log.debug("SSE emitter registered. Active clients: {}", emitters.size());
    }

    /**
     * Publishes an event to all registered SSE clients.
     *
     * @param event the unified event to broadcast
     */
    public void publish(UnifiedEvent event) {
        List<SseEmitter> deadEmitters = new CopyOnWriteArrayList<>();
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name(event.getType().name().toLowerCase())
                        .data(event));
            } catch (IOException e) {
                deadEmitters.add(emitter);
                log.debug("Removing disconnected SSE emitter: {}", e.getMessage());
            }
        }
        emitters.removeAll(deadEmitters);
    }
}
