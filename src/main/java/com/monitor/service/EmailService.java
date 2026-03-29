package com.monitor.service;

import com.monitor.config.MonitorMailProperties;
import com.monitor.model.UnifiedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Asynchronous email alert service.
 * Sends a plain-text email via Groupwise SMTP only for {@code CRITICAL} events.
 * The recipient list and SMTP settings are defined in {@code application.yml}.
 *
 * <h3>Resilience</h3>
 * <p>When the SMTP server is temporarily unavailable the failed event is placed on
 * an internal {@link LinkedBlockingQueue retry queue} instead of being silently
 * discarded.  A {@link Scheduled @Scheduled} drain task periodically retries all
 * queued events, ensuring that critical alerts are eventually delivered even after
 * transient network or SMTP failures (inspired by Zabbix's built-in action-retry
 * and Alertmanager's {@code send_resolved} guarantees).</p>
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final MonitorMailProperties mailProperties;

    @Value("${monitor.mail.from}")
    private String from;

    @Value("${monitor.mail.recipients}")
    private List<String> recipients;

    @Value("${monitor.mail.subject-prefix:CRITICAL ALERT}")
    private String subjectPrefix;

    /** Bounded queue that holds events whose initial send attempt failed. */
    private final LinkedBlockingQueue<UnifiedEvent> retryQueue =
            new LinkedBlockingQueue<>(500);

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
        this.mailProperties = mailProperties;
    }

    /**
     * Sends a critical alert email asynchronously so it never blocks the event pipeline.
     *
     * <p>If the send fails the event is enqueued for a later retry attempt.
     * The retry queue is bounded to 500 entries; if it is full the event is logged
     * as dropped rather than blocking the caller.</p>
     *
     * @param event the critical event to notify about
     */
    @Async
    public void sendCriticalAlert(UnifiedEvent event) {
        try {
            doSend(event);
        } catch (Exception e) {
            log.error("Failed to send critical alert email, queuing for retry: source={} error={}",
                    event.getSource(), e.getMessage());
            boolean enqueued = retryQueue.offer(event);
            if (!enqueued) {
                log.error("Email retry queue full, alert dropped permanently: source={}",
                        event.getSource());
            }
        }
    }

    /**
     * Drains the retry queue and attempts to re-send all previously failed emails.
     * Runs on a fixed delay (default: 60 s).  Events that fail again are re-enqueued
     * if space allows, otherwise they are logged as permanently dropped.
     */
    @Scheduled(fixedDelayString = "${monitor.mail.retry-interval-ms:60000}")
    public void drainRetryQueue() {
        if (retryQueue.isEmpty()) {
            return;
        }
        List<UnifiedEvent> pending = new ArrayList<>();
        retryQueue.drainTo(pending);
        log.info("Email retry: attempting {} queued alert(s)", pending.size());
        for (UnifiedEvent event : pending) {
            try {
                doSend(event);
            } catch (Exception e) {
                log.error("Email retry failed: source={} error={}", event.getSource(), e.getMessage());
                boolean requeued = retryQueue.offer(event);
                if (!requeued) {
                    log.error("Email retry queue full after retry failure, alert dropped: source={}",
                            event.getSource());
                }
            }
        }
    }

    /**
     * Performs the actual SMTP send.  Extracted so it can be shared between the
     * primary send path and the retry path.
     */
    private void doSend(UnifiedEvent event) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(recipients.toArray(new String[0]));
        message.setSubject(subjectPrefix + " – " + event.getSource());
        message.setText(buildBody(event));
        mailSender.send(message);
        log.info("Critical alert email sent for source: {}", event.getSource());
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

    // ── Package-private helper for testing ───────────────────────────────────

    /** Returns the number of events currently waiting in the retry queue. */
    int retryQueueSize() {
        return retryQueue.size();
    }
}
