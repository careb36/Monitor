/**
 * Collector abstraction layer for the Monitor platform.
 *
 * <p>Defines the two fundamental data-collection paradigms used to ingest events:</p>
 * <ul>
 *   <li>{@link com.monitor.collector.PullCollector} – Monitor actively scrapes targets on a
 *       schedule (Prometheus-style pull model). Implemented by {@code PollingService}.</li>
 *   <li>{@link com.monitor.collector.PushCollector} – External agents or message brokers
 *       deliver events to Monitor (Zabbix-agent / Kafka-style push model). Implemented by
 *       {@code KafkaConsumerService}.</li>
 * </ul>
 *
 * <p>Both interfaces extend {@link com.monitor.collector.EventCollector}, which provides the
 * common {@code name()} and {@code mode()} contract. The {@link com.monitor.collector.CollectionMode}
 * enum distinguishes between {@code PULL} and {@code PUSH}.</p>
 */
package com.monitor.collector;
