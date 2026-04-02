/**
 * Monitor – real-time operations monitoring dashboard for high-stakes environments.
 *
 * <p>The application ingests events from two sources:</p>
 * <ul>
 *   <li><strong>Apache Kafka / Debezium CDC</strong> – change-data-capture events from the
 *       {@code log_traza} database table, pushed by Debezium to a Kafka topic.</li>
 *   <li><strong>Scheduled health-check polling</strong> – periodic availability checks for
 *       databases and background daemon processes.</li>
 * </ul>
 *
 * <p>All events are normalized into {@link com.monitor.model.UnifiedEvent} and broadcast to
 * connected browser clients via a Server-Sent Events stream at
 * {@code GET /api/events/stream}. Critical events additionally trigger email alerts and are
 * persisted in the {@link com.monitor.service.CriticalOutbox} to guarantee at-least-once
 * delivery.</p>
 *
 * <h2>Key packages</h2>
 * <ul>
 *   <li>{@code com.monitor.model} – domain model: {@code UnifiedEvent}, {@code EventType}, {@code Severity}.</li>
 *   <li>{@code com.monitor.service} – pipeline services: {@code EventBus}, {@code ReliableEventPipelineService},
 *       {@code EmailService}, {@code PollingService}, {@code KafkaConsumerService}.</li>
 *   <li>{@code com.monitor.collector} – collector abstractions: {@code PullCollector}, {@code PushCollector}.</li>
 *   <li>{@code com.monitor.controller} – REST/SSE endpoints: {@code SseController}.</li>
 *   <li>{@code com.monitor.config} – Spring configuration: CORS, Security, Jackson, mail, polling.</li>
 *   <li>{@code com.monitor.security} – rate limiting and audit logging.</li>
 * </ul>
 */
package com.monitor;
