package com.monitor.service;

import com.monitor.model.UnifiedEvent;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Central in-memory event bus that decouples event producers (Kafka, Polling)
 * from SSE consumers. Uses Java 21 Virtual Threads for high-performance, 
 * non-blocking broadcast to thousands of clients.
 */
@Component
public class EventBus {

    private static final Logger log = LoggerFactory.getLogger(EventBus.class);

    // O(1) registration and removal. No GC pressure compared to CopyOnWriteArrayList for 10k+ clients.
    private final Set<SseEmitter> emitters = ConcurrentHashMap.newKeySet();
    
    // Dedicated Virtual Thread executor for non-blocking I/O fan-out
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down EventBus executor...");
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Registers a new SSE client emitter.
     */
    public void addEmitter(SseEmitter emitter) {
        emitters.add(emitter);
        
        // Use the same executor to handle lifecycle callbacks to avoid blocking platform threads
        emitter.onCompletion(() -> {
            log.trace("SSE emitter completed. Remaining clients: {}", emitters.size());
            emitters.remove(emitter);
        });
        emitter.onTimeout(() -> {
            log.trace("SSE emitter timed out. Remaining clients: {}", emitters.size());
            emitters.remove(emitter);
        });
        emitter.onError(e -> {
            log.debug("SSE emitter error: {}. Remaining clients: {}", e.getMessage(), emitters.size());
            emitters.remove(emitter);
        });

        sendToEmitter(emitter, SseEmitter.event().comment("connected"), "initial SSE heartbeat");
        log.debug("SSE emitter registered. Active clients: {}", emitters.size());
    }

    @Scheduled(fixedDelayString = "${monitor.sse.heartbeat-interval-ms:15000}")
    void sendHeartbeat() {
        broadcast(SseEmitter.event().comment("heartbeat"), "periodic SSE heartbeat");
    }

    /**
     * Publishes an event to all registered SSE clients using Virtual Threads.
     */
    public void publish(UnifiedEvent event) {
        broadcast(SseEmitter.event()
                .name(event.getType().name().toLowerCase())
                .data(event), "domain SSE event");
    }

    /**
     * Sends an event directly to a single emitter (used for SSE replay on reconnect).
     */
    public boolean publishToEmitter(SseEmitter emitter, UnifiedEvent event, String id) {
        return sendToEmitter(
                emitter,
                SseEmitter.event()
                        .id(id)
                        .name(event.getType().name().toLowerCase())
                        .data(event),
                "replay SSE event"
        );
    }

    /**
     * Fan-out broadcast using Virtual Threads.
     * Each emission runs in its own lightweight thread, so one slow client won't stall the others.
     */
    private void broadcast(SseEmitter.SseEventBuilder eventBuilder, String operation) {
        for (SseEmitter emitter : emitters) {
            executor.submit(() -> sendToEmitter(emitter, eventBuilder, operation));
        }
    }

    /**
     * Sends an event to a single emitter and handles removal on failure.
     */
    private boolean sendToEmitter(SseEmitter emitter,
                                  SseEmitter.SseEventBuilder eventBuilder,
                                  String operation) {
        try {
            emitter.send(eventBuilder);
            return true;
        } catch (IOException | IllegalStateException e) {
            // IllegalStateException handles cases where the emitter is already completed
            if (emitters.remove(emitter)) {
                log.debug("Removing disconnected/failed SSE emitter during {}: {}", operation, e.getMessage());
            }
            return false;
        }
    }
}
