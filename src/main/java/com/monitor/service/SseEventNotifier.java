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
        return eventBus.publish(event).failed() == 0;
    }
}

