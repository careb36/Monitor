package com.monitor.service;

import com.monitor.model.UnifiedEvent;
import org.springframework.stereotype.Component;

@Component
public class SseEventNotifier implements EventNotifier {

    private final EventBus eventBus;

    public SseEventNotifier(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    @Override
    public String channel() {
        return "sse";
    }

    @Override
    public boolean notify(UnifiedEvent event) {
        // publish() fans out to all live SSE emitters synchronously; any disconnected
        // emitter is silently removed.  We treat the call as best-effort and always
        // return true so the pipeline does not retry SSE delivery.
        eventBus.publish(event);
        return true;
    }
}
