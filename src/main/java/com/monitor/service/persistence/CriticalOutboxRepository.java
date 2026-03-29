package com.monitor.service.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface CriticalOutboxRepository extends JpaRepository<CriticalOutboxEntity, Long> {

    @Query("""
            select e from CriticalOutboxEntity e
            where e.delivered = false and e.nextAttemptAt <= :now
            order by e.nextAttemptAt asc
            """)
    List<CriticalOutboxEntity> findDue(@Param("now") Instant now, Pageable pageable);

    @Query("""
            select e from CriticalOutboxEntity e
            where e.id > :lastId
            order by e.id asc
            """)
    List<CriticalOutboxEntity> findAfterId(@Param("lastId") long lastId, Pageable pageable);

    long countByDeliveredFalse();
}

