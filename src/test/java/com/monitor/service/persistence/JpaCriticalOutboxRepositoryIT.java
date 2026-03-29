package com.monitor.service.persistence;

import com.monitor.model.EventType;
import com.monitor.model.Severity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * EN: Integration test for JPA outbox repository behavior against H2 in Oracle mode.
 * ES: Prueba de integracion del repositorio JPA outbox sobre H2 en modo Oracle.
 */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class JpaCriticalOutboxRepositoryIT {

    @Autowired
    private CriticalOutboxRepository repository;

    @Test
    void save_andQueryDueAndReplay_shouldRespectOutboxContracts() {
        // EN: Persist three rows with mixed delivery states.
        // ES: Persistir tres filas con estados de entrega mixtos.
        CriticalOutboxEntity dueCritical = createEntity("db-a", false, Instant.now().minusSeconds(10), 0);
        CriticalOutboxEntity pendingFuture = createEntity("db-b", false, Instant.now().plusSeconds(600), 1);
        CriticalOutboxEntity alreadyDelivered = createEntity("db-c", true, Instant.now().minusSeconds(5), 3);

        dueCritical = repository.save(dueCritical);
        pendingFuture = repository.save(pendingFuture);
        alreadyDelivered = repository.save(alreadyDelivered);

        assertTrue(dueCritical.getId() > 0);
        assertTrue(pendingFuture.getId() > dueCritical.getId());
        assertTrue(alreadyDelivered.getId() > pendingFuture.getId());

        List<CriticalOutboxEntity> due = repository.findDue(Instant.now(), PageRequest.of(0, 10));
        assertEquals(1, due.size());
        assertEquals("db-a", due.get(0).getSource());

        List<CriticalOutboxEntity> replay = repository.findAfterId(dueCritical.getId(), PageRequest.of(0, 10));
        assertEquals(2, replay.size());
        assertEquals("db-b", replay.get(0).getSource());
        assertEquals("db-c", replay.get(1).getSource());

        assertEquals(2, repository.countByDeliveredFalse());

        // EN: Mark one pending row as delivered and verify pending counter updates.
        // ES: Marcar una fila pendiente como entregada y verificar contador pendiente.
        pendingFuture.setDelivered(true);
        repository.save(pendingFuture);

        assertEquals(1, repository.countByDeliveredFalse());

        List<CriticalOutboxEntity> dueAfterUpdate = repository.findDue(Instant.now().plusSeconds(700), PageRequest.of(0, 10));
        assertFalse(dueAfterUpdate.stream().anyMatch(entity -> "db-b".equals(entity.getSource())));
    }

    private CriticalOutboxEntity createEntity(String source,
                                              boolean delivered,
                                              Instant nextAttemptAt,
                                              int attempts) {
        CriticalOutboxEntity entity = new CriticalOutboxEntity();
        entity.setEventType(EventType.INFRASTRUCTURE);
        entity.setSeverity(Severity.CRITICAL);
        entity.setSource(source);
        entity.setMessage(source + " is DOWN");
        entity.setEventTimestamp(Instant.now().minusSeconds(30));
        entity.setAttempts(attempts);
        entity.setCreatedAt(Instant.now().minusSeconds(30));
        entity.setNextAttemptAt(nextAttemptAt);
        entity.setDelivered(delivered);
        entity.setLastError(delivered ? "" : "smtp-failed");
        return entity;
    }
}

