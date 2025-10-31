package com.chatwolf.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import reactor.core.publisher.Mono;

@Configuration
public class RateLimiterConfig {

	// Limit by authenticated principal (if you use OAuth2/security)
    @Primary
    @Bean("UserKeyResolver")
    KeyResolver userKeyResolver() {
        return exchange -> exchange.getPrincipal()
            .map(principal -> principal.getName())
            .defaultIfEmpty("anonymous");
    }
    
    // Limit by client IP (remote address)
    @Bean("IpKeyResolver")
    KeyResolver ipKeyResolver() {
        return exchange -> Mono.just(
            exchange.getRequest().getRemoteAddress() != null
                ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                : "unknown-ip");
    }
    
}
