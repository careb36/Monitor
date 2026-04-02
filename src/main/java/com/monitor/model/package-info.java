/**
 * Domain model for the Monitor platform.
 *
 * <p>Contains the core data types that flow through the entire event pipeline:</p>
 * <ul>
 *   <li>{@link com.monitor.model.UnifiedEvent} – the canonical event representation
 *       shared by all producers and consumers.</li>
 *   <li>{@link com.monitor.model.EventType} – discriminator enum ({@code DATA} vs
 *       {@code INFRASTRUCTURE}).</li>
 *   <li>{@link com.monitor.model.Severity} – operational severity levels that drive
 *       routing ({@code INFO}, {@code WARNING}, {@code CRITICAL}).</li>
 *   <li>{@link com.monitor.model.IngestMetadata} – transport metadata record that keeps
 *       pipeline concerns separate from the domain event.</li>
 * </ul>
 *
 * <p>Classes in this package are intentionally free of Spring or persistence annotations
 * to keep the domain model portable and easy to unit-test.</p>
 */
package com.monitor.model;
