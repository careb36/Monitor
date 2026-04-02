/**
 * REST controllers for the Monitor dashboard API.
 *
 * <p>Currently contains a single controller:</p>
 * <ul>
 *   <li>{@link com.monitor.controller.SseController} – exposes the
 *       {@code GET /api/events/stream} Server-Sent Events endpoint that the
 *       Next.js dashboard subscribes to for real-time event delivery.</li>
 * </ul>
 */
package com.monitor.controller;
