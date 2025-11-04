package com.chatwolf.gateway.config;

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
            return chain.filter(exchange).doFinally(signal -> {
                log.info(
                        "Request: path={}, method={}, status={}, traceId={}, userId={}",
                        exchange.getRequest().getPath(),
                        exchange.getRequest().getMethod(),
                        exchange.getResponse().getStatusCode(),
                        exchange.getRequest().getHeaders().getFirst("X-B3-TraceId"),
                        exchange.getRequest().getHeaders().getFirst("X-User-Id"));
            });
        };
    }
}
