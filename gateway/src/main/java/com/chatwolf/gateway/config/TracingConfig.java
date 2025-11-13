package com.chatwolf.gateway.config;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.exporter.SpanExportingPredicate;
import io.micrometer.tracing.propagation.Propagator;
import java.util.HashMap;
import java.util.Map;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.observation.SecurityObservationSettings;

@Configuration
public class TracingConfig {

    @Bean
    GlobalFilter webocketTracingFilter(Tracer tracer, Propagator propagator) {
        return (exchange, chain) -> {
            if ("websocket".equalsIgnoreCase(exchange.getRequest().getHeaders().getUpgrade())) {
                Span currentSpan = tracer.currentSpan();
                if (currentSpan != null) {
                    Map<String, String> headers = new HashMap<>();
                    propagator.inject(tracer.currentTraceContext().context(), headers, Map::put);

                    headers.forEach((k, v) -> exchange.getRequest().mutate().header(k, v));
                }
            }
            return chain.filter(exchange);
        };
    }

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
