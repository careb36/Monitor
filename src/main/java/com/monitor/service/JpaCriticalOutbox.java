package com.monitor.service;

import com.monitor.model.OutboxStatus;
import com.monitor.model.UnifiedEvent;
import com.monitor.service.persistence.CriticalOutboxEntity;
import com.monitor.service.persistence.CriticalOutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * JPA-backed implementation of {@link CriticalOutbox} for durable event persistence.
 *
 * <p>Activated by setting {@code monitor.outbox.jpa.enabled=true}. When active, every
 * critical event is persisted to the {@code CRITICAL_OUTBOX} database table before any
 * delivery attempt, providing full durability across application restarts.</p>
 *
 * <p>This implementation is designed for Oracle but is compatible with any JPA-supported
 * database. It relies on optimistic locking ({@code @Version}) on
 * {@link CriticalOutboxEntity} to prevent concurrent modification races in multi-instance
 * deployments.</p>
 *
 * <p>The in-memory implementation ({@link InMemoryCriticalOutbox}) is the default
 * ({@code @Primary}) and is used when this bean is not activated.</p>
 *
 * @see InMemoryCriticalOutbox
 * @see CriticalOutboxRepository
 */
@Service
@ConditionalOnProperty(name = "monitor.outbox.jpa.enabled", havingValue = "true")
public class JpaCriticalOutbox implements CriticalOutbox {

    private static final Logger log = LoggerFactory.getLogger(JpaCriticalOutbox.class);

    private final CriticalOutboxRepository repository;
    private final int maxAttempts;

    /**
     * @param repository  JPA repository for the {@code CRITICAL_OUTBOX} table
     * @param maxAttempts maximum delivery attempts before an entry is moved to dead-letter state;
     *                    controlled by {@code monitor.outbox.jpa.max-attempts} (default: 5)
     */
    public JpaCriticalOutbox(
            CriticalOutboxRepository repository,
            @Value("${monitor.outbox.jpa.max-attempts:5}") int maxAttempts) {
        this.repository = repository;
        this.maxAttempts = maxAttempts;
        log.info("JpaCriticalOutbox activated — persistence backend: JPA/Oracle, maxAttempts={}", maxAttempts);
    }

    /** {@inheritDoc} */
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
        entity.setStatus(OutboxStatus.PENDING);
        entity.setLastError("");
        long id = repository.save(entity).getId();
        log.info("CRITICAL EVENT SAVED: id={} type={} severity={} source={} message={}",
                id, event.getType(), event.getSeverity(), event.getSource(), event.getMessage());
        return id;
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public Optional<OutboxEntry> find(long id) {
        return repository.findById(id).map(this::toOutboxEntry);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public void markDelivered(long id) {
        repository.findById(id).ifPresent(entity -> {
            entity.setAttempts(entity.getAttempts() + 1);
            entity.setDelivered(true);
            entity.setStatus(OutboxStatus.DELIVERED);
            entity.setNextAttemptAt(Instant.now());
            repository.save(entity);
            log.info("CRITICAL EVENT DELIVERED: id={} source={} totalAttempts={}",
                    id, entity.getSource(), entity.getAttempts());
        });
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public void markProcessing(long id) {
        repository.findById(id).ifPresent(entity -> {
            if (entity.getStatus() != OutboxStatus.PENDING) {
                throw new ObjectOptimisticLockingFailureException(CriticalOutboxEntity.class, id);
            }
            entity.setStatus(OutboxStatus.PROCESSING);
            repository.save(entity);
            log.debug("CRITICAL EVENT PROCESSING: id={}", id);
        });
    }

    @Override
    @Transactional
    public void markRetry(long id, Instant nextAttemptAt, String reason) {
        repository.findById(id).ifPresent(entity -> {
            entity.setAttempts(entity.getAttempts() + 1);
            entity.setDelivered(false);
            entity.setStatus(OutboxStatus.PENDING);
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

    /** {@inheritDoc} */
    @Override
    @Transactional
    public void resetProcessingToPending(Instant olderThan) {
        List<CriticalOutboxEntity> timedOut = repository.findTimedOutProcessing(olderThan);
        for (CriticalOutboxEntity entity : timedOut) {
            log.warn("TIMEOUT: Resetting entry {} from PROCESSING to PENDING (locked at {})",
                    entity.getId(), entity.getLastStatusChangeAt());
            entity.setStatus(OutboxStatus.PENDING);
            repository.save(entity);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<OutboxEntry> findDue(Instant now, int limit) {
        return repository.findDue(now, PageRequest.of(0, limit)).stream()
                .map(this::toOutboxEntry)
                .toList();
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public List<OutboxEntry> findAfterId(long lastId, int limit) {
        return repository.findAfterId(lastId, PageRequest.of(0, limit)).stream()
                .map(this::toOutboxEntry)
                .toList();
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public long pendingCount() {
        return repository.countByDeliveredFalse();
    }

    /**
     * Maps a JPA entity to its immutable {@link OutboxEntry} view.
     * Reconstructs the {@link com.monitor.model.UnifiedEvent} from the entity's
     * persisted fields, preserving the original event timestamp.
     *
     * @param entity the JPA entity loaded from the database
     * @return an equivalent {@code OutboxEntry} record
     */
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
