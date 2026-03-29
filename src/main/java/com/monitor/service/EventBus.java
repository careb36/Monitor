package com.monitor.service;

import com.monitor.model.UnifiedEvent;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Central in-memory event bus that decouples event producers (Kafka, Polling)
 * from SSE consumers. Each connected browser client gets its own {@link SseEmitter}.
 *
 * <h3>Resilience features</h3>
 * <ul>
 *   <li><b>Deduplication</b> – an {@link AlertDeduplicator} suppresses identical alerts
 *       that arrive within the configured time window, preventing alert storms when a
 *       target flickers (inspired by Prometheus Alertmanager's {@code repeat_interval}).</li>
 *   <li><b>Bounded internal queue</b> – a {@link LinkedBlockingQueue} decouples producers
 *       from the SSE fan-out.  When the queue is full (back-pressure) the event is
 *       logged and dropped rather than blocking the caller thread.</li>
 *   <li><b>Background drainer</b> – a single daemon thread drains the queue and broadcasts
 *       to all live SSE emitters, isolating the producer path from slow or broken clients.</li>
 * </ul>
 */
@Component
public class EventBus {

    private static final Logger log = LoggerFactory.getLogger(EventBus.class);

    private final AlertDeduplicator deduplicator;
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    private final BlockingQueue<UnifiedEvent> queue;
    private final ExecutorService drainerExecutor;

    public EventBus(AlertDeduplicator deduplicator,
                    @Value("${monitor.queue.capacity:1000}") int capacity) {
        this.deduplicator = deduplicator;
        this.queue = new LinkedBlockingQueue<>(capacity);
        this.drainerExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "alert-queue-drainer");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * Starts the background drainer thread that broadcasts queued events to all
     * registered SSE clients.
     */
    @PostConstruct
    public void startDrainer() {
        drainerExecutor.submit(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    UnifiedEvent event = queue.take();
                    broadcastToEmitters(event);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        log.debug("Alert queue drainer started");
    }

    /**
     * Shuts down the drainer thread gracefully on application stop.
     */
    @PreDestroy
    public void stopDrainer() {
        drainerExecutor.shutdownNow();
        try {
            if (!drainerExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                log.warn("Alert queue drainer did not terminate cleanly");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

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
        sendToEmitter(emitter, SseEmitter.event().comment("connected"), "initial SSE heartbeat");
        log.debug("SSE emitter registered. Active clients: {}", emitters.size());
    }

    @Scheduled(fixedDelayString = "${monitor.sse.heartbeat-interval-ms:15000}")
    void sendHeartbeat() {
        broadcast(SseEmitter.event().comment("heartbeat"), "periodic SSE heartbeat");
    }

    /**
     * Publishes an event to all registered SSE clients.
     *
     * <p>The event is first checked for deduplication; if it is a duplicate within
     * the configured window it is silently discarded.  Otherwise it is enqueued for
     * asynchronous fan-out by the background drainer thread.  If the internal queue
     * is full (back-pressure scenario) the event is logged and dropped.</p>
     *
     * @param event the unified event to broadcast
     */
    public void publish(UnifiedEvent event) {
        if (deduplicator.isDuplicate(event)) {
            return;
        }
        boolean enqueued = queue.offer(event);
        if (!enqueued) {
            log.warn("Alert queue full (capacity exhausted), event dropped: source={} severity={}",
                    event.getSource(), event.getSeverity());
        }
    }

    /**
     * Fan-out to all live SSE emitters.  Emitters that throw {@link IOException}
     * are considered dead and removed from the registry.
     */
    private void broadcastToEmitters(UnifiedEvent event) {
        List<SseEmitter> deadEmitters = new CopyOnWriteArrayList<>();
        for (SseEmitter emitter : emitters) {
            if (!sendToEmitter(emitter, eventBuilder, operation)) {
                deadEmitters.add(emitter);
            }
        }
        emitters.removeAll(deadEmitters);
    }

    private boolean sendToEmitter(SseEmitter emitter,
                                  SseEmitter.SseEventBuilder eventBuilder,
                                  String operation) {
        try {
            emitter.send(eventBuilder);
            return true;
        } catch (IOException e) {
            emitters.remove(emitter);
            log.debug("Removing disconnected SSE emitter after {}: {}", operation, e.getMessage());
            return false;
        }
    }
}
