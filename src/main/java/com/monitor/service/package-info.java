/**
 * Core event processing services for the Monitor platform.
 *
 * <p>This package contains the central pipeline that routes events from producers
 * to consumers:</p>
 * <ul>
 *   <li>{@link com.monitor.service.EventBus} – central in-memory fan-out bus that delivers
 *       events to all registered SSE emitters using Java 21 Virtual Threads.</li>
 *   <li>{@link com.monitor.service.ReliableEventPipelineService} – durable ingestion pipeline
 *       with deduplication, outbox persistence for CRITICAL events, and exponential-backoff retry.</li>
 *   <li>{@link com.monitor.service.KafkaConsumerService} – Debezium CDC event consumer (push).</li>
 *   <li>{@link com.monitor.service.PollingService} – scheduled infrastructure health-check (pull).</li>
 *   <li>{@link com.monitor.service.EmailService} – asynchronous SMTP alert delivery with retry queue.</li>
 *   <li>{@link com.monitor.service.AlertDeduplicator} – coarse-grained alert-storm suppressor.</li>
 *   <li>{@link com.monitor.service.EventDeduplicator} – fine-grained pipeline-level deduplicator.</li>
 *   <li>{@link com.monitor.service.CriticalOutbox} – outbox interface (in-memory or JPA backed).</li>
 *   <li>{@link com.monitor.service.CriticalReplayService} – SSE missed-event replay on reconnect.</li>
 * </ul>
 */
package com.monitor.service;
