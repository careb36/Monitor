package com.monitor.model;

public enum EventType {
    /** Originates from CDC / Debezium (log_traza inserts). */
    DATA,
    /** Originates from the scheduled health-check polling. */
    INFRASTRUCTURE
}
