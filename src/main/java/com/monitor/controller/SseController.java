package com.monitor.controller;

import com.monitor.service.EventBus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Exposes a single Server-Sent Events endpoint that the Next.js dashboard
 * subscribes to. Both infrastructure and CDC/data events are pushed here.
 */
@RestController
@RequestMapping("/api/events")
public class SseController {

    private static final long SSE_TIMEOUT_MS = 30L * 60 * 1000; // 30 minutes

    private final EventBus eventBus;

    public SseController(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    /**
     * Opens an SSE stream for the caller.
     * The browser should reconnect automatically on disconnect (standard SSE behaviour).
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        eventBus.addEmitter(emitter);
        return emitter;
    }
}
