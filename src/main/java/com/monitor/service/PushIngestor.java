package com.monitor.service;

import java.util.Map;

public interface PushIngestor {

    String sourceId();

    void onMessage(byte[] payload, Map<String, String> headers);
}
