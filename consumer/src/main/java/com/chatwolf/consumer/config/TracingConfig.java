package com.chatwolf.consumer.config;

import io.micrometer.tracing.exporter.SpanExportingPredicate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TracingConfig {

    @Bean
    SpanExportingPredicate noActuatorObservations() {
        return span -> {
            String uri = span.getTags().get("uri");
            return uri == null || !uri.startsWith("/actuator");
        };
    }
}
