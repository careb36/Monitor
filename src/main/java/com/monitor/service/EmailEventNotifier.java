package com.monitor.service;

import com.monitor.model.Severity;
import com.monitor.model.UnifiedEvent;
import org.springframework.stereotype.Component;

@Component
public class EmailEventNotifier implements EventNotifier {

    private final EmailService emailService;

    public EmailEventNotifier(EmailService emailService) {
        this.emailService = emailService;
    }

    @Override
    public String channel() {
        return "email";
    }

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
