package com.monitor.service;

import com.monitor.config.MonitorPollingTargetsProperties;
import com.monitor.model.Severity;
import com.monitor.model.UnifiedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PollingServiceTest {

    @Mock
    private EventBus eventBus;

    @Mock
    private EmailService emailService;

    private PollingService pollingService;
    private MonitorPollingTargetsProperties targetsProperties;

    @BeforeEach
    void setUp() {
        targetsProperties = new MonitorPollingTargetsProperties();
        targetsProperties.setDatabases(List.of("oracle-secondary-01", "oracle-secondary-02"));
        targetsProperties.setDaemons(List.of("batch-processor"));

        pollingService = new PollingService(eventBus, emailService, targetsProperties) {
            @Override
            boolean simulatePing(String target) {
                // Simulate that "oracle-secondary-02" is down, everything else up
                return !"oracle-secondary-02".equals(target);
            }
        };
    }

    @Test
    void pollAll_publishesRecoveryAndDownEvents() {
        pollingService.pollAll();

        // "oracle-secondary-02" is down → CRITICAL event published
        verify(eventBus, atLeastOnce()).publish(argThat(e ->
                e.getSeverity() == Severity.CRITICAL && e.getSource().contains("oracle-secondary-02")));

        // Critical event → email sent
        verify(emailService, atLeastOnce()).sendCriticalAlert(any(UnifiedEvent.class));
    }

    @Test
    void pollAll_upTargets_publishInfoOnFirstPoll() {
        pollingService.pollAll();

        // "oracle-secondary-01" is UP on first poll → INFO event published
        verify(eventBus, atLeastOnce()).publish(argThat(e ->
                e.getSeverity() == Severity.INFO && e.getSource().contains("oracle-secondary-01")));
    }

    @Test
    void pollAll_upTargets_noEmailForInfo() {
        pollingService.pollAll();

        // Only the DOWN target sends an email
        verify(emailService, times(1)).sendCriticalAlert(any(UnifiedEvent.class));
    }

    @Test
    void pollAll_secondPoll_noRedundantInfoEventsForStableUpTargets() {
        // First poll triggers INFO (status transition null → true)
        pollingService.pollAll();
        // Second poll: no new transition for stable-UP targets
        pollingService.pollAll();

        // Stable-UP targets publish INFO only once (first poll)
        verify(eventBus, times(1)).publish(argThat(e ->
                e.getSeverity() == Severity.INFO && e.getSource().contains("oracle-secondary-01")));
    }
}
