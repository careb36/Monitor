package com.monitor.service;

import com.monitor.model.Severity;
import com.monitor.model.UnifiedEvent;
import org.springframework.stereotype.Component;

/**
 * {@link EventNotifier} implementation that delivers events to the email channel.
 *
 * <p>Acts as a thin adapter between the pipeline's {@link EventNotifier} contract and
 * the {@link EmailService}. Only {@link com.monitor.model.Severity#CRITICAL} events
 * trigger an actual email; lower-severity events are accepted without action and
 * return {@code true} to keep the pipeline moving.</p>
 *
 * <p><strong>Resilience note:</strong> {@link EmailService#sendCriticalAlert} is
 * {@code @Async} and manages its own internal retry queue. This notifier always
 * returns {@code true} so that the outbox entry is marked delivered at the pipeline
 * level; email-specific retry logic is fully owned by {@link EmailService}.</p>
 */
@Component
public class EmailEventNotifier implements EventNotifier {

    private final EmailService emailService;

    /**
     * @param emailService the underlying email delivery service
     */
    public EmailEventNotifier(EmailService emailService) {
        this.emailService = emailService;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@code "email"}
     */
    @Override
    public String channel() {
        return "email";
    }

    /**
     * Triggers a critical alert email for {@link com.monitor.model.Severity#CRITICAL} events;
     * non-critical events are silently accepted.
     *
     * @param event the event to deliver; must not be {@code null}
     * @return always {@code true} — delivery failures are handled internally by
     *         {@link EmailService}'s retry queue
     */
    @Override
    public boolean notify(UnifiedEvent event) {
        if (event.getSeverity() != Severity.CRITICAL) {
            return true;
        }
        // sendCriticalAlert is @Async and returns void; actual delivery failures are
        // handled internally by EmailService's retry queue.  We return true here so the
        // pipeline marks the outbox entry as delivered and does not schedule an email
        // retry at this layer (email resilience is owned by EmailService).
        emailService.sendCriticalAlert(event);
        return true;
    }
}
