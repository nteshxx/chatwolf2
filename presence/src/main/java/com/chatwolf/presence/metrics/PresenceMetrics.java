package com.chatwolf.presence.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class PresenceMetrics {
    public PresenceMetrics(MeterRegistry registry) {
        registry.gauge("presence_service_connected_subscribers", 0);
        // Add more metrics as needed
    }
}
