package com.monitor.service;

import com.monitor.config.MonitorMailProperties;
import com.monitor.model.UnifiedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Asynchronous email alert service.
 * Sends a plain-text email via Groupwise SMTP only for {@code CRITICAL} events.
 * The recipient list and SMTP settings are defined in {@code application.yml}.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final MonitorMailProperties mailProperties;

    public EmailService(JavaMailSender mailSender, MonitorMailProperties mailProperties) {
        this.mailSender = mailSender;
        this.mailProperties = mailProperties;
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
            message.setFrom(mailProperties.getFrom());
            message.setTo(mailProperties.getRecipients().toArray(new String[0]));
            message.setSubject(mailProperties.getSubjectPrefix() + " – " + event.getSource());
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
