/**
 * JPA persistence layer for the critical-event outbox.
 *
 * <p>Contains the {@link com.monitor.service.persistence.CriticalOutboxEntity} JPA entity
 * mapped to the {@code CRITICAL_OUTBOX} table and the
 * {@link com.monitor.service.persistence.CriticalOutboxRepository} Spring Data repository
 * that provides CRUD operations plus custom JPQL queries for dispatch and SSE replay.</p>
 *
 * <p>This sub-package is only active when {@code monitor.outbox.jpa.enabled=true}.
 * When the property is absent or {@code false}, the in-memory implementation
 * ({@link com.monitor.service.InMemoryCriticalOutbox}) is used instead and no JPA
 * infrastructure is required.</p>
 */
package com.monitor.service.persistence;
