package com.chatwolf.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

@Configuration
public class RateLimiterConfig {

    @Primary
    @Bean("UserKeyResolver")
    KeyResolver userKeyResolver() {
        // Limit by authenticated principal (if you use OAuth2/security)
        return exchange ->
                exchange.getPrincipal().map(principal -> principal.getName()).defaultIfEmpty("anonymous");
    }

    @Bean("IpKeyResolver")
    KeyResolver ipKeyResolver() {
        // Limit by client IP (remote address)
        return exchange -> Mono.just(
                exchange.getRequest().getRemoteAddress() != null
                        ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                        : "unknown-ip");
    }
}
