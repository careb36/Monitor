package com.monitor.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Metadata for event ingestion that keeps source details out of UnifiedEvent.
 */
public record IngestMetadata(
        String source,
        Instant receivedAt,
        String correlationId,
        boolean replay
) {

    public static IngestMetadata live(String source) {
        return new IngestMetadata(source, Instant.now(), UUID.randomUUID().toString(), false);
    }

    public static IngestMetadata replay(String source, String correlationId) {
        return new IngestMetadata(source, Instant.now(), correlationId, true);
    }
}

