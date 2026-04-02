package com.monitor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Monitor application entry point.
 *
 * <p>Bootstraps the Spring Boot context with scheduling ({@code @EnableScheduling})
 * for the polling and retry pipeline tasks, and async execution ({@code @EnableAsync})
 * for the {@link com.monitor.service.EmailService} alert delivery.</p>
 *
 * <p>The application exposes a single Server-Sent Events stream at
 * {@code GET /api/events/stream} that the Next.js dashboard subscribes to.
 * Events originate from two sources:
 * <ul>
 *   <li>Apache Kafka / Debezium CDC ({@link com.monitor.service.KafkaConsumerService})</li>
 *   <li>Scheduled health-check polling ({@link com.monitor.service.PollingService})</li>
 * </ul>
 * </p>
 *
 * @see com.monitor.controller.SseController
 * @see com.monitor.service.EventBus
 */
@SpringBootApplication
@EnableScheduling
@EnableAsync
public class App {

    /**
     * Application entry point.
     *
     * @param args command-line arguments passed to the Spring context
     */
    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }
}

