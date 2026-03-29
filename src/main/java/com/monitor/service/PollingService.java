package com.monitor.service;

import com.monitor.config.MonitorPollingTargetsProperties;
import com.monitor.model.EventType;
import com.monitor.model.Severity;
import com.monitor.model.UnifiedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simulates health-check polling for secondary databases and background daemons.
 * Every 30 seconds it evaluates the status of each monitored target and publishes
 * a {@link UnifiedEvent} of type {@link EventType#INFRASTRUCTURE} whenever the
 * status changes or the target is DOWN.
 */
@Service
public class PollingService {

    private static final Logger log = LoggerFactory.getLogger(PollingService.class);

    private final EventBus eventBus;
    private final EmailService emailService;
    private final MonitorPollingTargetsProperties targetsProperties;

    /** Tracks the last known status so we only emit on transitions. */
    private final Map<String, Boolean> lastStatus = new ConcurrentHashMap<>();

    public PollingService(EventBus eventBus,
                          EmailService emailService,
                          MonitorPollingTargetsProperties targetsProperties) {
        this.eventBus = eventBus;
        this.emailService = emailService;
        this.targetsProperties = targetsProperties;
    }

    @Scheduled(fixedDelayString = "${monitor.polling.interval-ms:30000}")
    public void pollAll() {
        log.debug("Running health-check poll cycle");
        for (String target : targetsProperties.getDatabases()) {
            check(target, "DATABASE");
        }
        for (String target : targetsProperties.getDaemons()) {
            check(target, "DAEMON");
        }
    }

    /**
     * Simulates a ping to {@code target}. In production, replace the simulation
     * with actual JDBC connection tests or HTTP health-check calls.
     */
    private void check(String target, String category) {
        boolean isUp = simulatePing(target);
        Boolean previous = lastStatus.put(target, isUp);

        boolean statusChanged = (previous == null) || (previous != isUp);

        if (!isUp) {
            Severity severity = Severity.CRITICAL;
            UnifiedEvent event = new UnifiedEvent(
                    EventType.INFRASTRUCTURE,
                    severity,
                    category + " :: " + target,
                    target + " is DOWN"
            );
            eventBus.publish(event);
            emailService.sendCriticalAlert(event);
            log.warn("Health-check FAIL: {} [{}]", target, category);

        } else if (statusChanged) {
            // Emit a recovery INFO event only when transitioning back to UP
            UnifiedEvent event = new UnifiedEvent(
                    EventType.INFRASTRUCTURE,
                    Severity.INFO,
                    category + " :: " + target,
                    target + " is UP"
            );
            eventBus.publish(event);
            log.info("Health-check OK: {} [{}]", target, category);
        }
    }

    /**
     * Stub implementation that always returns {@code true} (UP).
     * <p>
     * This method is intentionally package-private so that tests can override it
     * via an anonymous subclass to simulate DOWN scenarios without requiring
     * real network connectivity.  In production, replace the body with actual
     * connectivity checks (e.g., {@code DataSource.getConnection()} for databases
     * or an HTTP health-check call for daemon processes).
     * </p>
     *
     * @param target the name or identifier of the target to ping
     * @return {@code true} if the target is reachable, {@code false} otherwise
     */
    boolean simulatePing(String target) {
        return true;
    }
}
