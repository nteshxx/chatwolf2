package com.chatwolf.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

@Configuration
public class RouteConfig {

    @Primary
    @Bean("UserIdKeyResolver")
    KeyResolver userIdKeyResolver() {
        return exchange ->
                exchange.getPrincipal().map(principal -> principal.getName()).defaultIfEmpty("anonymous");
    }

    @Bean("IpAddressKeyResolver")
    KeyResolver ipAddressKeyResolver() {
        return exchange -> Mono.just(
                exchange.getRequest().getRemoteAddress() != null
                        ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                        : "unknown-ip");
    }

    @Primary
    @Bean
    RedisRateLimiter loginRateLimiter() {
        return new RedisRateLimiter(20, 100, 1);
    }

    @Bean
    RedisRateLimiter socketRateLimiter() {
        return new RedisRateLimiter(100, 200, 1);
    }

    @Bean
    RouteLocator routes(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("auth", r -> r.path("/api/v1/auth/**")
                        .filters(f -> f.stripPrefix(2)
                                .requestRateLimiter(config -> {
                                    config.setKeyResolver(ipAddressKeyResolver());
                                    config.setRateLimiter(loginRateLimiter());
                                })
                                .circuitBreaker(cb ->
                                        cb.setName("authCircuitBreaker").setFallbackUri("forward:/fallback/auth")))
                        .uri("lb://auth"))
                .route("socket", r -> r.path("/api/v1/socket/connect/**")
                        .filters(f -> f.stripPrefix(2)
                                .preserveHostHeader()
                                .requestRateLimiter(config -> {
                                    config.setKeyResolver(userIdKeyResolver());
                                    config.setRateLimiter(socketRateLimiter());
                                })
                                .circuitBreaker(cb ->
                                        cb.setName("socketCircuitBreaker").setFallbackUri("forward:/fallback/socket")))
                        .uri("lb:ws://socket"))
                .build();
    }
}
