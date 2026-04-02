package com.monitor.service;

import java.util.Map;

/**
 * Internal push-ingestor contract for service-layer components that receive
 * raw binary payloads from message brokers or external agents.
 *
 * <p>Implementations parse the raw payload and its headers, construct a
 * {@link com.monitor.model.UnifiedEvent}, and submit it to the
 * {@link IngestionFacade} or {@link com.monitor.service.EventBus} for further
 * processing. This interface provides a uniform boundary for Kafka listeners,
 * webhook receivers, and other push-based data sources.</p>
 *
 * @see com.monitor.collector.PushCollector
 */
public interface PushIngestor {

    /**
     * Returns the unique identifier for this ingestor's data source.
     *
     * @return source ID (e.g. {@code "kafka-cdc"}, {@code "webhook-receiver"})
     */
    String sourceId();

    /**
     * Processes a raw message payload delivered by an external system.
     *
     * <p>Implementations must handle malformed payloads gracefully (log and discard)
     * rather than propagating exceptions to the message broker, which could
     * trigger undesired redelivery loops.</p>
     *
     * @param payload raw serialized event data (typically JSON encoded as UTF-8)
     * @param headers message metadata such as content-type or tracing headers;
     *                may be empty but never {@code null}
     */
    void onMessage(byte[] payload, Map<String, String> headers);
}
