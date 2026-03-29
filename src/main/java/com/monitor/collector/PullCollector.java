package com.monitor.collector;

/**
 * Contract for <em>Pull-based</em> collectors (Prometheus-style).
 *
 * <p>Implementations are responsible for actively fetching data from their targets
 * on a fixed schedule.  The platform calls {@link #scrape()} at each tick; the
 * implementation queries its targets and publishes any resulting {@code UnifiedEvent}s
 * to the {@code EventBus}.</p>
 *
 * <p>Example: {@code PollingService} performs TCP/JDBC health checks every 30 s.</p>
 */
public interface PullCollector extends EventCollector {

    /**
     * Executes one scrape cycle.
     *
     * <p>Implementations should be idempotent and tolerate partial failures
     * (e.g. one target is unreachable while others succeed).</p>
     */
    void scrape();

    /** {@inheritDoc} */
    @Override
    default CollectionMode mode() {
        return CollectionMode.PULL;
    }
}
