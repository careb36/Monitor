package com.monitor.service;

import com.monitor.model.UnifiedEvent;

/**
 * Delivery channel abstraction used by the resilient dispatcher.
 */
public interface EventNotifier {

    String channel();

    boolean notify(UnifiedEvent event);
}

