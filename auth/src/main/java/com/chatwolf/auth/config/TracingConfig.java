package com.chatwolf.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.observation.SecurityObservationSettings;

import io.micrometer.tracing.exporter.SpanExportingPredicate;

@Configuration
public class TracingConfig {
	
	@Bean
    SecurityObservationSettings noSpringSecurityObservations() {
    	return SecurityObservationSettings.noObservations();
    }

    @Bean
    SpanExportingPredicate noActuatorSpans() {
        return span -> {
            String uri = span.getTags().get("uri");
            return uri == null || !uri.startsWith("/actuator");
        };
    }

    @Bean
    SpanExportingPredicate noEurekaClientSpans() {
        return span -> {
            String clientName = span.getTags().get("client.name");

            // Early return for most common case
            if ("eureka".equals(clientName)) {
                return false;
            }

            String httpUrl = span.getTags().get("http.url");
            return httpUrl == null || !httpUrl.contains("/eureka/");
        };
    }
}
