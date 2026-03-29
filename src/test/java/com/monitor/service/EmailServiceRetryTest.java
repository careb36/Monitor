package com.monitor.service;

import com.monitor.config.MonitorMailProperties;
import com.monitor.model.EventType;
import com.monitor.model.Severity;
import com.monitor.model.UnifiedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests the retry-queue behaviour introduced in {@link EmailService}.
 */
@ExtendWith(MockitoExtension.class)
class EmailServiceRetryTest {

    @Mock
    private JavaMailSender mailSender;

    private EmailService emailService;

    @BeforeEach
    void setUp() {
        MonitorMailProperties mailProperties = new MonitorMailProperties();
        mailProperties.setFrom("monitor@example.com");
        mailProperties.setRecipients(List.of("ops@example.com"));
        mailProperties.setSubjectPrefix("CRITICAL ALERT");
        emailService = new EmailService(mailSender, mailProperties);
    }

    @Test
    void sendCriticalAlert_whenMailFails_eventIsQueuedForRetry() {
        doThrow(new RuntimeException("SMTP unreachable")).when(mailSender).send(any(SimpleMailMessage.class));

        UnifiedEvent event = new UnifiedEvent(
                EventType.DATA, Severity.CRITICAL, "log_traza [ERR-001]", "Fatal error");

        emailService.sendCriticalAlert(event);

        assertEquals(1, emailService.retryQueueSize(),
                "Failed alert must be placed on the retry queue");
    }

    @Test
    void drainRetryQueue_retriesPreviouslyFailedAlert() {
        // First call fails, second (retry) succeeds
        doThrow(new RuntimeException("SMTP unreachable"))
                .doNothing()
                .when(mailSender).send(any(SimpleMailMessage.class));

        UnifiedEvent event = new UnifiedEvent(
                EventType.INFRASTRUCTURE, Severity.CRITICAL, "oracle-02", "is DOWN");

        emailService.sendCriticalAlert(event); // fails → enqueued
        assertEquals(1, emailService.retryQueueSize());

        emailService.drainRetryQueue(); // retry succeeds

        assertEquals(0, emailService.retryQueueSize(), "Retry queue must be empty after successful retry");
        verify(mailSender, times(2)).send(any(SimpleMailMessage.class));
    }

    @Test
    void drainRetryQueue_whenRetryAlsoFails_eventRemainsInQueue() {
        doThrow(new RuntimeException("SMTP still down")).when(mailSender).send(any(SimpleMailMessage.class));

        UnifiedEvent event = new UnifiedEvent(
                EventType.DATA, Severity.CRITICAL, "log_traza [ERR-002]", "Still failing");

        emailService.sendCriticalAlert(event); // fails → enqueued
        emailService.drainRetryQueue();        // retry also fails → re-enqueued

        assertEquals(1, emailService.retryQueueSize(),
                "Event must remain in queue when retry also fails");
    }

    @Test
    void drainRetryQueue_whenQueueEmpty_doesNothing() {
        emailService.drainRetryQueue(); // must not throw
        verify(mailSender, times(0)).send(any(SimpleMailMessage.class));
    }
}
