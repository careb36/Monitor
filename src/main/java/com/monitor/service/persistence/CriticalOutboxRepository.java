package com.monitor.service.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

/**
 * Spring Data JPA repository for {@link CriticalOutboxEntity}.
 *
 * <p>Provides standard CRUD operations via {@link JpaRepository} plus two custom
 * JPQL queries needed by the dispatch and replay mechanisms:</p>
 * <ul>
 *   <li>{@link #findDue} – returns undelivered entries ready for a dispatch attempt.</li>
 *   <li>{@link #findAfterId} – returns entries after a given ID for SSE client replay.</li>
 * </ul>
 */
public interface CriticalOutboxRepository extends JpaRepository<CriticalOutboxEntity, Long> {

    /**
     * Finds undelivered outbox entries whose next attempt time is on or before {@code now},
     * ordered oldest-first to ensure fair dispatch ordering.
     *
     * @param now      the reference instant (typically {@link java.time.Instant#now()})
     * @param pageable pagination descriptor; use {@code PageRequest.of(0, limit)} to bound results
     * @return ordered list of due entities; never {@code null}
     */
    @Query("""
            select e from CriticalOutboxEntity e
            where e.delivered = false and e.nextAttemptAt <= :now
            order by e.nextAttemptAt asc
            """)
    List<CriticalOutboxEntity> findDue(@Param("now") Instant now, Pageable pageable);

    /**
     * Finds all entries (delivered or not) with an ID strictly greater than {@code lastId},
     * ordered by ID ascending. Used by the SSE replay service to recover missed events after
     * a client reconnect.
     *
     * @param lastId   the last event ID acknowledged by the client
     * @param pageable pagination descriptor; use {@code PageRequest.of(0, limit)} to bound results
     * @return ordered list of subsequent entities; never {@code null}
     */
    @Query("""
            select e from CriticalOutboxEntity e
            where e.id > :lastId
            order by e.id asc
            """)
    List<CriticalOutboxEntity> findAfterId(@Param("lastId") long lastId, Pageable pageable);

    /**
     * Counts the number of entries that have not yet been successfully delivered.
     *
     * @return non-negative count of pending entries
     */
    long countByDeliveredFalse();
}
