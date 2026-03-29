package com.monitor.collector;

/**
 * Base abstraction for all event-source collectors in Monitor.
 *
 * <p>Inspired by the Prometheus scraping interface and the Zabbix agent contract,
 * every collector must advertise its name and the {@link CollectionMode} it uses
 * so that the platform can manage and observe collectors uniformly.</p>
 *
 * <p>Concrete implementations must also implement either {@link PullCollector} or
 * {@link PushCollector} to fulfil the collection contract for their mode.</p>
 */
public interface EventCollector {

    /**
     * Returns a unique, human-readable identifier for this collector.
     *
     * @return collector name (e.g. {@code "infrastructure-poller"}, {@code "kafka-cdc"})
     */
    String name();

    /**
     * Returns the collection mode this collector operates in.
     *
     * @return {@link CollectionMode#PULL} or {@link CollectionMode#PUSH}
     */
    CollectionMode mode();
}
