package com.monitor.service;

import com.monitor.model.UnifiedEvent;
import com.monitor.service.persistence.CriticalOutboxEntity;
import com.monitor.service.persistence.CriticalOutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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

    private static final Logger log = LoggerFactory.getLogger(JpaCriticalOutbox.class);

    private final CriticalOutboxRepository repository;
    private final int maxAttempts;

    public JpaCriticalOutbox(
            CriticalOutboxRepository repository,
            @Value("${monitor.outbox.jpa.max-attempts:5}") int maxAttempts) {
        this.repository = repository;
        this.maxAttempts = maxAttempts;
        log.info("JpaCriticalOutbox activated — persistence backend: JPA/Oracle, maxAttempts={}", maxAttempts);
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
        long id = repository.save(entity).getId();
        log.info("CRITICAL EVENT SAVED: id={} type={} severity={} source={} message={}",
                id, event.getType(), event.getSeverity(), event.getSource(), event.getMessage());
        return id;
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
            log.info("CRITICAL EVENT DELIVERED: id={} source={} totalAttempts={}",
                    id, entity.getSource(), entity.getAttempts());
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
            if (entity.getAttempts() >= maxAttempts) {
                log.error("CRITICAL EVENT DEAD LETTER: id={} source={} attempts={} reason={}",
                        id, entity.getSource(), entity.getAttempts(), reason);
            } else {
                log.warn("CRITICAL EVENT RETRY: id={} source={} attempt={} nextAt={} reason={}",
                        id, entity.getSource(), entity.getAttempts(), nextAttemptAt, reason);
            }
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
