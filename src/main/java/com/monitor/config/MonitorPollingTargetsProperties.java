package com.monitor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "monitor.polling.targets")
public class MonitorPollingTargetsProperties {

    private List<String> databases = new ArrayList<>();
    private List<String> daemons = new ArrayList<>();

    public List<String> getDatabases() {
        return databases;
    }

    public void setDatabases(List<String> databases) {
        this.databases = databases;
    }

    public List<String> getDaemons() {
        return daemons;
    }

    public void setDaemons(List<String> daemons) {
        this.daemons = daemons;
    }
}
