package com.monitor.service;

import com.monitor.model.EventType;
import com.monitor.model.Severity;
import com.monitor.model.UnifiedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class CriticalReplayServiceTest {

    @Test
    void replaySince_withValidLastEventId_replaysMissedCriticalEvents() {
        CriticalOutbox outbox = mock(CriticalOutbox.class);
        EventBus eventBus = mock(EventBus.class);
        SseEmitter emitter = new SseEmitter(1000L);

        UnifiedEvent event = new UnifiedEvent(EventType.DATA, Severity.CRITICAL, "src", "msg");
        event.setTimestamp(Instant.now());
        OutboxEntry entry = new OutboxEntry(11L, event, 1, Instant.now(), Instant.now(), true, "");

        when(outbox.findAfterId(10L, 50)).thenReturn(List.of(entry));
        when(eventBus.publishToEmitter(eq(emitter), eq(event), eq("11"))).thenReturn(true);

        CriticalReplayService replayService = new CriticalReplayService(outbox, eventBus, 50);
        replayService.replaySince("10", emitter);

        verify(outbox).findAfterId(10L, 50);
        verify(eventBus).publishToEmitter(emitter, event, "11");
    }

    @Test
    void replaySince_withInvalidLastEventId_doesNothing() {
        CriticalOutbox outbox = mock(CriticalOutbox.class);
        EventBus eventBus = mock(EventBus.class);

        CriticalReplayService replayService = new CriticalReplayService(outbox, eventBus, 50);
        replayService.replaySince("abc", new SseEmitter(1000L));

        verify(outbox, never()).findAfterId(anyLong(), anyInt());
        verify(eventBus, never()).publishToEmitter(any(), any(), any());
    }
}
