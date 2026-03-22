package com.monitor.service;

import com.monitor.model.UnifiedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Asynchronous email alert service.
 * Sends a plain-text email via Groupwise SMTP only for {@code CRITICAL} events.
 * The recipient list and SMTP settings are defined in {@code application.yml}.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${monitor.mail.from}")
    private String from;

    @Value("${monitor.mail.recipients}")
    private List<String> recipients;

    @Value("${monitor.mail.subject-prefix:CRITICAL ALERT}")
    private String subjectPrefix;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Sends a critical alert email asynchronously so it never blocks the event pipeline.
     *
     * @param event the critical event to notify about
     */
    @Async
    public void sendCriticalAlert(UnifiedEvent event) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(recipients.toArray(new String[0]));
            message.setSubject(subjectPrefix + " – " + event.getSource());
            message.setText(buildBody(event));
            mailSender.send(message);
            log.info("Critical alert email sent for source: {}", event.getSource());
        } catch (Exception e) {
            log.error("Failed to send critical alert email: {}", e.getMessage(), e);
        }
    }

    private String buildBody(UnifiedEvent event) {
        return String.format(
                "CRITICAL ALERT%n%n" +
                "Type    : %s%n" +
                "Source  : %s%n" +
                "Message : %s%n" +
                "Time    : %s%n",
                event.getType(),
                event.getSource(),
                event.getMessage(),
                event.getTimestamp()
        );
    }
}
