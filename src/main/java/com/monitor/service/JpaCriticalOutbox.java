package com.monitor.service;

import com.monitor.model.UnifiedEvent;
import com.monitor.service.persistence.CriticalOutboxEntity;
import com.monitor.service.persistence.CriticalOutboxRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@ConditionalOnProperty(name = "monitor.outbox.jpa.enabled", havingValue = "true")
public class JpaCriticalOutbox implements CriticalOutbox {

    private final CriticalOutboxRepository repository;

    public JpaCriticalOutbox(CriticalOutboxRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public long save(UnifiedEvent event) {
        Instant now = Instant.now();
        CriticalOutboxEntity entity = new CriticalOutboxEntity();
        entity.setEventType(event.getType());
        entity.setSeverity(event.getSeverity());
        entity.setSource(event.getSource());
        entity.setMessage(event.getMessage());
        entity.setEventTimestamp(event.getTimestamp());
        entity.setAttempts(0);
        entity.setCreatedAt(now);
        entity.setNextAttemptAt(now);
        entity.setDelivered(false);
        entity.setLastError("");
        return repository.save(entity).getId();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OutboxEntry> find(long id) {
        return repository.findById(id).map(this::toOutboxEntry);
    }

    @Override
    @Transactional
    public void markDelivered(long id) {
        repository.findById(id).ifPresent(entity -> {
            entity.setAttempts(entity.getAttempts() + 1);
            entity.setDelivered(true);
            entity.setNextAttemptAt(Instant.now());
            repository.save(entity);
        });
    }

    @Override
    @Transactional
    public void markRetry(long id, Instant nextAttemptAt, String reason) {
        repository.findById(id).ifPresent(entity -> {
            entity.setAttempts(entity.getAttempts() + 1);
            entity.setDelivered(false);
            entity.setNextAttemptAt(nextAttemptAt);
            entity.setLastError(reason);
            repository.save(entity);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public List<OutboxEntry> findDue(Instant now, int limit) {
        return repository.findDue(now, PageRequest.of(0, limit)).stream()
                .map(this::toOutboxEntry)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OutboxEntry> findAfterId(long lastId, int limit) {
        return repository.findAfterId(lastId, PageRequest.of(0, limit)).stream()
                .map(this::toOutboxEntry)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long pendingCount() {
        return repository.countByDeliveredFalse();
    }

    private OutboxEntry toOutboxEntry(CriticalOutboxEntity entity) {
        UnifiedEvent event = new UnifiedEvent(
                entity.getEventType(),
                entity.getSeverity(),
                entity.getSource(),
                entity.getMessage()
        );
        event.setTimestamp(entity.getEventTimestamp());
        return new OutboxEntry(
                entity.getId(),
                event,
                entity.getAttempts(),
                entity.getCreatedAt(),
                entity.getNextAttemptAt(),
                entity.isDelivered(),
                entity.getLastError() == null ? "" : entity.getLastError()
        );
    }
}
