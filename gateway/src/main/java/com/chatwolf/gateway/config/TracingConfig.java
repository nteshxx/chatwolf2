package com.chatwolf.gateway.config;

import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.server.WebFilter;

@Slf4j
@Configuration
public class TracingConfig {

    @Bean
    WebFilter tracingFilter() {
        return (exchange, chain) -> {
            final String traceId =
                    Objects.isNull(exchange.getRequest().getHeaders().getFirst("X-Trace-Id"))
                            ? java.util.UUID.randomUUID().toString()
                            : exchange.getRequest().getHeaders().getFirst("X-Trace-Id");
            exchange.getAttributes().put("X-Trace-Id", traceId);

            return chain.filter(exchange).doFinally(signal -> {
                log.info(
                        "Request: path={}, method={}, status={}, traceId={}, userId={}",
                        exchange.getRequest().getPath(),
                        exchange.getRequest().getMethod(),
                        exchange.getResponse().getStatusCode(),
                        traceId,
                        exchange.getRequest().getHeaders().getFirst("X-User-Id"));
            });
        };
    }
}
