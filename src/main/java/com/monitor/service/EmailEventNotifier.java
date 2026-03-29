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
        return emailService.sendCriticalAlert(event);
    }
}

