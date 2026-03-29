package com.monitor.collector;

/**
 * Contract for <em>Push-based</em> collectors (Zabbix-agent / Kafka-style).
 *
 * <p>Implementations receive raw event payloads that external agents or message brokers
 * deliver to Monitor.  The platform invokes {@link #onEvent(String)} for each arriving
 * message; the implementation parses the payload and publishes the resulting
 * {@code UnifiedEvent} to the {@code EventBus}.</p>
 *
 * <p>Example: {@code KafkaConsumerService} receives Debezium CDC envelopes from Kafka.</p>
 */
public interface PushCollector extends EventCollector {

    /**
     * Called when an external system delivers a raw event payload.
     *
     * @param rawPayload the serialized event payload (typically JSON)
     */
    void onEvent(String rawPayload);

    /** {@inheritDoc} */
    @Override
    default CollectionMode mode() {
        return CollectionMode.PUSH;
    }
}
