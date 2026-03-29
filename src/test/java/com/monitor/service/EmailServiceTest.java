package com.monitor.service;

import com.monitor.config.MonitorMailProperties;
import com.monitor.model.EventType;
import com.monitor.model.Severity;
import com.monitor.model.UnifiedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    private EmailService emailService;
    private MonitorMailProperties mailProperties;

    @BeforeEach
    void setUp() {
        mailProperties = new MonitorMailProperties();
        mailProperties.setFrom("monitor@example.com");
        mailProperties.setRecipients(List.of("ops@example.com"));
        mailProperties.setSubjectPrefix("CRITICAL ALERT");
        emailService = new EmailService(mailSender, mailProperties);
    }

    @Test
    void sendCriticalAlert_sendsMailWithCorrectFields() {
        UnifiedEvent event = new UnifiedEvent(
                EventType.INFRASTRUCTURE, Severity.CRITICAL,
                "DATABASE :: oracle-secondary-01", "oracle-secondary-01 is DOWN");

        emailService.sendCriticalAlert(event);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage sent = captor.getValue();

        assertEquals("monitor@example.com", sent.getFrom());
        assertNotNull(sent.getTo());
        assertEquals(1, sent.getTo().length);
        assertEquals("ops@example.com", sent.getTo()[0]);
        assertTrue(sent.getSubject().startsWith("CRITICAL ALERT"));
        assertTrue(sent.getText().contains("oracle-secondary-01 is DOWN"));
    }

    @Test
    void sendCriticalAlert_whenMailFails_doesNotPropagate() {
        doThrow(new RuntimeException("SMTP unreachable")).when(mailSender).send(any(SimpleMailMessage.class));

        UnifiedEvent event = new UnifiedEvent(
                EventType.DATA, Severity.CRITICAL, "log_traza [ERR-001]", "Fatal error");

        assertDoesNotThrow(() -> emailService.sendCriticalAlert(event));
    }
}
