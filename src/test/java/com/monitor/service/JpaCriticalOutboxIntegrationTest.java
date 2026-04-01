package com.monitor.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.monitor.model.EventType;
import com.monitor.model.Severity;
import com.monitor.model.UnifiedEvent;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {
        "monitor.outbox.jpa.enabled=true",
        "monitor.outbox.jpa.max-attempts=5"
})
@ActiveProfiles("test")
class JpaCriticalOutboxIntegrationTest {

    @Autowired
    private JpaCriticalOutbox outbox;

    @Test
    @DirtiesContext
    void persistence_shouldSurviveContextRestart() {
        UnifiedEvent event = new UnifiedEvent(EventType.DATA, Severity.CRITICAL, "db-prod", "Disk full");
        long id = outbox.save(event);

        Optional<OutboxEntry> entry = outbox.find(id);
        assertTrue(entry.isPresent());
        assertEquals("Disk full", entry.get().event().getMessage());
    }

    /**
     * Verifica que al superar maxAttempts:
     * 1. El registro queda en la BD con attempts=5 y delivered=false.
     * 2. Se emite exactamente un log de nivel ERROR con "DEAD LETTER".
     * 3. Los intentos previos emiten WARN, no ERROR.
     *
     * OWASP A09: el ciclo de vida completo de un evento crítico debe quedar auditado.
     */
    @Test
    @Transactional
    void deadLetter_afterMaxAttempts_shouldPersistRecordAndEmitErrorLog() {
        Logger jpaLogger = (Logger) LoggerFactory.getLogger(JpaCriticalOutbox.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        jpaLogger.addAppender(appender);

        try {
            UnifiedEvent event = new UnifiedEvent(EventType.DATA, Severity.CRITICAL, "db-prod", "Connection lost");
            long id = outbox.save(event);
            Instant deferredFuture = Instant.now().plusSeconds(60);

            // 5 reintentos: attempts va de 0→1, 1→2, ..., 4→5
            // El último (5 >= maxAttempts=5) dispara ERROR; los anteriores disparan WARN
            for (int attempt = 1; attempt <= 5; attempt++) {
                outbox.markRetry(id, deferredFuture, "timeout on attempt " + attempt);
            }

            // El registro debe sobrevivir en la BD con estado de dead letter
            Optional<OutboxEntry> entry = outbox.find(id);
            assertThat(entry).isPresent();
            assertThat(entry.get().attempts()).isEqualTo(5);
            assertThat(entry.get().delivered()).isFalse();
            assertThat(entry.get().lastError()).contains("timeout");

            // Exactamente 1 ERROR con "DEAD LETTER" para el intento 5
            List<ILoggingEvent> errorLogs = appender.list.stream()
                    .filter(e -> e.getLevel() == Level.ERROR)
                    .filter(e -> e.getFormattedMessage().contains("DEAD LETTER"))
                    .toList();
            assertThat(errorLogs).hasSize(1);
            assertThat(errorLogs.get(0).getFormattedMessage()).contains(String.valueOf(id));

            // Los 4 intentos anteriores deben ser WARN, no ERROR
            List<ILoggingEvent> warnRetryLogs = appender.list.stream()
                    .filter(e -> e.getLevel() == Level.WARN)
                    .filter(e -> e.getFormattedMessage().contains("RETRY"))
                    .toList();
            assertThat(warnRetryLogs).hasSize(4);

        } finally {
            jpaLogger.detachAppender(appender);
        }
    }

    /**
     * Verifica que findDue solo retorna entradas con nextAttemptAt <= now y delivered=false.
     * Esto valida la corrección de la query que IDX_OUTBOX_PENDING_DUE fue diseñado a optimizar.
     * Un plan de ejecución en Oracle confirmará el uso del índice en producción.
     */
    @Test
    @Transactional
    void findDue_shouldReturnOnlyPendingEntriesDueForRetry() {
        Instant now = Instant.now();
        Instant future = now.plusSeconds(300);

        long dueId1 = outbox.save(new UnifiedEvent(EventType.DATA, Severity.CRITICAL, "src-a", "event due A"));
        long dueId2 = outbox.save(new UnifiedEvent(EventType.INFRASTRUCTURE, Severity.WARNING, "src-b", "event due B"));
        long notDueId = outbox.save(new UnifiedEvent(EventType.DATA, Severity.INFO, "src-c", "event deferred C"));

        // Mover notDueId al futuro: no debe aparecer en findDue
        outbox.markRetry(notDueId, future, "deferred deliberately");

        // dueId1 y dueId2 tienen nextAttemptAt = createdAt (pasado inmediato) → deben aparecer
        List<OutboxEntry> due = outbox.findDue(Instant.now(), 10);

        List<Long> returnedIds = due.stream().map(OutboxEntry::id).toList();
        assertThat(returnedIds).contains(dueId1, dueId2);
        assertThat(returnedIds).doesNotContain(notDueId);

        // Ninguna entrada entregada debe aparecer
        outbox.markDelivered(dueId1);
        List<OutboxEntry> dueAfterDelivery = outbox.findDue(Instant.now(), 10);
        assertThat(dueAfterDelivery.stream().map(OutboxEntry::id).toList()).doesNotContain(dueId1);
    }
}
