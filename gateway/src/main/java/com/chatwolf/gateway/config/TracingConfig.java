package com.chatwolf.gateway.config;

import io.micrometer.tracing.exporter.SpanExportingPredicate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.observation.SecurityObservationSettings;

@Configuration
public class TracingConfig {

    @Bean
    SecurityObservationSettings noSpringSecurityObservations() {
        return SecurityObservationSettings.noObservations();
    }

    @Bean
    SpanExportingPredicate noActuatorObservations() {
        return span -> {
            String uri = span.getTags().get("uri");
            return uri == null || !uri.startsWith("/actuator");
        };
    }

    @Bean
    SpanExportingPredicate noEurekaClientObservations() {
        return span -> {
            String clientName = span.getTags().get("client.name");
            if ("eureka".equals(clientName)) {
                return false;
            }

            String httpUrl = span.getTags().get("http.url");
            return httpUrl == null || !httpUrl.contains("/eureka/");
        };
    }
}
