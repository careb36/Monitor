package com.monitor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration properties for outbound email alerts.
 *
 * <p>Bound from the {@code monitor.mail.*} namespace in {@code application.yml}.
 * These properties are injected into {@link com.monitor.service.EmailService} to
 * configure the SMTP sender address, recipient list, and email subject prefix.</p>
 *
 * <p>Example configuration:</p>
 * <pre>
 * monitor:
 *   mail:
 *     from: monitor-noreply@example.com
 *     recipients:
 *       - ops-team@example.com
 *       - oncall@example.com
 *     subject-prefix: "[MONITOR]"
 * </pre>
 */
@Component
@ConfigurationProperties(prefix = "monitor.mail")
public class MonitorMailProperties {

    /** SMTP envelope sender address. */
    private String from;

    /** List of email addresses that will receive critical alert notifications. */
    private List<String> recipients = new ArrayList<>();

    /**
     * Prefix prepended to every alert email subject line.
     * Defaults to {@code "CRITICAL ALERT"}.
     */
    private String subjectPrefix = "CRITICAL ALERT";

    /**
     * Returns the SMTP sender address.
     *
     * @return the {@code from} address
     */
    public String getFrom() {
        return from;
    }

    /**
     * Sets the SMTP sender address.
     *
     * @param from a valid RFC-5321 email address
     */
    public void setFrom(String from) {
        this.from = from;
    }

    /**
     * Returns the list of alert recipient email addresses.
     *
     * @return mutable list of recipient addresses; never {@code null}
     */
    public List<String> getRecipients() {
        return recipients;
    }

    /**
     * Sets the alert recipient list.
     *
     * @param recipients list of RFC-5321 email addresses; must not be {@code null}
     */
    public void setRecipients(List<String> recipients) {
        this.recipients = recipients;
    }

    /**
     * Returns the subject-line prefix for alert emails.
     *
     * @return the subject prefix string
     */
    public String getSubjectPrefix() {
        return subjectPrefix;
    }

    /**
     * Sets the subject-line prefix.
     *
     * @param subjectPrefix the prefix to prepend to every alert email subject
     */
    public void setSubjectPrefix(String subjectPrefix) {
        this.subjectPrefix = subjectPrefix;
    }
}
