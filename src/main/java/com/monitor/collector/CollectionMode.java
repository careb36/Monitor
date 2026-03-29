package com.monitor.collector;

/**
 * Distinguishes the two fundamental data-collection paradigms used by the Monitor platform.
 *
 * <ul>
 *   <li><b>PULL</b> – Monitor actively scrapes targets on a schedule (Prometheus-style).
 *       The collector owns the initiative and fetches metrics/health data at a fixed interval.</li>
 *   <li><b>PUSH</b> – External agents or message brokers deliver events to Monitor
 *       (Zabbix-agent / Kafka-style).  The collector reacts to incoming data.</li>
 * </ul>
 */
public enum CollectionMode {
    /** Monitor scrapes targets actively (e.g. HTTP health-check, JDBC ping). */
    PULL,
    /** External systems push events to Monitor (e.g. Kafka CDC, Zabbix agent). */
    PUSH
}
