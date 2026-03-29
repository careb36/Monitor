package com.monitor.service;

import com.monitor.model.IngestMetadata;
import com.monitor.model.UnifiedEvent;

/**
 * Single entrypoint for all event producers (pull and push).
 */
public interface IngestionFacade {

    void ingest(UnifiedEvent event, IngestMetadata metadata);
}

