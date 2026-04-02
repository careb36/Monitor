package com.monitor.service;

import com.monitor.model.IngestMetadata;
import com.monitor.model.UnifiedEvent;

/**
 * Single entry point for all event producers (pull and push collectors).
 *
 * <p>Centralizing ingestion behind this facade decouples producers from the
 * internal pipeline mechanics (deduplication, outbox persistence, dispatch
 * scheduling). Producers only need to call
 * {@link #ingest(UnifiedEvent, IngestMetadata)}; the implementation decides
 * how and when to deliver the event.</p>
 *
 * @see ReliableEventPipelineService
 * @see com.monitor.model.IngestMetadata
 */
public interface IngestionFacade {

    /**
     * Submits an event for processing by the pipeline.
     *
     * <p>The implementation is free to apply deduplication, persistence, and
     * asynchronous dispatch. Callers should treat this method as fire-and-forget
     * and must not assume that delivery has completed when the method returns.</p>
     *
     * @param event    the event to ingest; must not be {@code null}
     * @param metadata transport and tracing metadata; must not be {@code null}
     */
    void ingest(UnifiedEvent event, IngestMetadata metadata);
}
