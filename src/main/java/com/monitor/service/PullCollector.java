package com.monitor.service;

/**
 * Internal pull-collector contract used within the service layer.
 *
 * <p>This interface parallels {@link com.monitor.collector.PullCollector} but operates
 * at the service level, providing a simplified collection contract for components that
 * need to be invoked by a scheduler or framework without requiring full collector
 * registration. Implementations actively fetch data from their targets on demand.</p>
 *
 * @see com.monitor.collector.PullCollector
 */
public interface PullCollector {

    /**
     * Returns a unique identifier for this collector instance.
     *
     * @return collector ID (e.g. {@code "infrastructure-poller"})
     */
    String collectorId();

    /**
     * Executes one collection cycle, fetching data from the target and publishing
     * any resulting events. Implementations should be idempotent and tolerant of
     * partial failures.
     */
    void collect();
}
