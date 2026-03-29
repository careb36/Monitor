package com.monitor.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@Service
public class CriticalReplayService {

    private static final Logger log = LoggerFactory.getLogger(CriticalReplayService.class);

    private final CriticalOutbox criticalOutbox;
    private final EventBus eventBus;
    private final int replayLimit;

    public CriticalReplayService(CriticalOutbox criticalOutbox,
                                 EventBus eventBus,
                                 @Value("${monitor.sse.replay-limit:200}") int replayLimit) {
        this.criticalOutbox = criticalOutbox;
        this.eventBus = eventBus;
        this.replayLimit = replayLimit;
    }

    public void replaySince(String lastEventIdHeader, SseEmitter emitter) {
        if (lastEventIdHeader == null || lastEventIdHeader.isBlank()) {
            return;
        }

        long lastEventId;
        try {
            lastEventId = Long.parseLong(lastEventIdHeader);
        } catch (NumberFormatException ex) {
            log.warn("Ignoring invalid Last-Event-ID value: {}", lastEventIdHeader);
            return;
        }

        List<OutboxEntry> replayEvents = criticalOutbox.findAfterId(lastEventId, replayLimit);
        for (OutboxEntry entry : replayEvents) {
            boolean sent = eventBus.publishToEmitter(emitter, entry.event(), String.valueOf(entry.id()));
            if (!sent) {
                break;
            }
        }
        if (!replayEvents.isEmpty()) {
            log.debug("Replayed {} critical events for Last-Event-ID={}", replayEvents.size(), lastEventId);
        }
    }
}

