package com.monitor.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.monitor.collector.PushCollector;
import com.monitor.model.EventType;
import com.monitor.model.Severity;
import com.monitor.model.UnifiedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * Consumes Debezium CDC events from the Kafka topic that mirrors {@code log_traza}.
 * Each insert in the table arrives as a Debezium envelope; this service extracts
 * the relevant fields and forwards a {@link UnifiedEvent} to the {@link EventBus}.
 *
 * <p>Implements {@link PushCollector} to participate in the unified collector
 * abstraction: Kafka acts as the agent that pushes CDC events to Monitor
 * (Zabbix-agent / Kafka-style push model).</p>
 */
@Service
public class KafkaConsumerService implements PushCollector {

    private static final Logger log = LoggerFactory.getLogger(KafkaConsumerService.class);

    private final ObjectMapper objectMapper;
    private final EventBus eventBus;
    private final EmailService emailService;

    public KafkaConsumerService(ObjectMapper objectMapper,
                                EventBus eventBus,
                                EmailService emailService) {
        this.objectMapper = objectMapper;
        this.eventBus = eventBus;
        this.emailService = emailService;
    }

    // ── PushCollector contract ────────────────────────────────────────────────

    @Override
    public String name() {
        return "kafka-cdc";
    }

    /**
     * Entry point for the platform's push-collector framework.
     * Delegates to {@link #consume(String)} which carries the
     * {@code @KafkaListener} annotation for Spring-Kafka binding.
     */
    @Override
    public void onEvent(String rawPayload) {
        consume(rawPayload);
    }

    // ── Kafka listener ────────────────────────────────────────────────────────

    /**
     * Listens to the Debezium topic for {@code log_traza} inserts.
     * The topic name follows the Debezium convention: {@code <server>.<schema>.<table>}.
     */
    @KafkaListener(topics = "${monitor.kafka.topic.log-traza}",
                   groupId = "${spring.kafka.consumer.group-id}",
                   containerFactory = "kafkaListenerContainerFactory")
    public void consume(String message) {
        try {
            JsonNode root = objectMapper.readTree(message);

            // Debezium payload: { "payload": { "after": { ... }, "op": "c" } }
            JsonNode payload = root.path("payload");
            String operation = payload.path("op").asText();

            // Only process inserts ("c" = create)
            if (!"c".equals(operation)) {
                return;
            }

            JsonNode after = payload.path("after");
            String errorCode  = after.path("ERROR_CODE").asText("UNKNOWN");
            String errorMsg   = after.path("ERROR_MSG").asText("No description");
            String severityRaw = after.path("SEVERITY").asText("INFO");

            Severity severity = parseSeverity(severityRaw);

            UnifiedEvent event = new UnifiedEvent(
                    EventType.DATA,
                    severity,
                    "log_traza [" + errorCode + "]",
                    errorMsg
            );

            eventBus.publish(event);

            if (severity == Severity.CRITICAL) {
                emailService.sendCriticalAlert(event);
            }

        } catch (Exception e) {
            log.error("Error processing Kafka CDC message: {}", e.getMessage(), e);
        }
    }

    private Severity parseSeverity(String raw) {
        try {
            return Severity.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return Severity.INFO;
        }
    }
}
