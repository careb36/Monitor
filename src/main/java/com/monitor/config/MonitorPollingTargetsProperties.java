package com.monitor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration properties for the health-check polling targets.
 *
 * <p>Bound from the {@code monitor.polling.targets.*} namespace in {@code application.yml}.
 * These properties are injected into {@link com.monitor.service.PollingService} to define
 * which databases and background daemons are polled for availability.</p>
 *
 * <p>Example configuration:</p>
 * <pre>
 * monitor:
 *   polling:
 *     targets:
 *       databases:
 *         - prod-db-01
 *         - prod-db-02
 *       daemons:
 *         - batch-processor
 *         - report-generator
 * </pre>
 */
@Component
@ConfigurationProperties(prefix = "monitor.polling.targets")
public class MonitorPollingTargetsProperties {

    /** Names or identifiers of database instances to poll. */
    private List<String> databases = new ArrayList<>();

    /** Names or identifiers of background daemon processes to poll. */
    private List<String> daemons = new ArrayList<>();

    /**
     * Returns the list of database target identifiers.
     *
     * @return mutable list of database names; never {@code null}
     */
    public List<String> getDatabases() {
        return databases;
    }

    /**
     * Sets the database target list.
     *
     * @param databases list of database names or identifiers; must not be {@code null}
     */
    public void setDatabases(List<String> databases) {
        this.databases = databases;
    }

    /**
     * Returns the list of daemon process target identifiers.
     *
     * @return mutable list of daemon names; never {@code null}
     */
    public List<String> getDaemons() {
        return daemons;
    }

    /**
     * Sets the daemon target list.
     *
     * @param daemons list of daemon names or identifiers; must not be {@code null}
     */
    public void setDaemons(List<String> daemons) {
        this.daemons = daemons;
    }
}
