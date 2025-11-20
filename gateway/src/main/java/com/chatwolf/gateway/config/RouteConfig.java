package com.chatwolf.gateway.config;

import java.time.Duration;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
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
    RedisRateLimiter moderateRateLimiter() {
        return new RedisRateLimiter(20, 100, 1);
    }

    @Bean
    RedisRateLimiter lenientRateLimiter() {
        return new RedisRateLimiter(100, 200, 1);
    }

    @Bean
    RouteLocator routes(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("auth", r -> r.path("/api/auth/**")
                        .filters(f -> f.requestRateLimiter(config -> {
                                    config.setKeyResolver(ipAddressKeyResolver());
                                    config.setRateLimiter(moderateRateLimiter());
                                })
                                .circuitBreaker(cb -> cb.setName("auth-circuit-breaker")
                                        .setFallbackUri("forward:/api/fallback/auth")))
                        .uri("lb://auth"))
                .route("socket", r -> r.path("/api/socket/**")
                        .filters(f -> f.preserveHostHeader()
                                .requestRateLimiter(config -> {
                                    config.setKeyResolver(userIdKeyResolver());
                                    config.setRateLimiter(lenientRateLimiter());
                                })
                                .circuitBreaker(cb -> cb.setName("socket-circuit-breaker")
                                        .setFallbackUri("forward:/api/fallback/socket")))
                        .uri("lb:ws://socket"))
                .route("presence", r -> r.path("/api/presence/**")
                        .filters(f -> f.requestRateLimiter(config -> {
                                    config.setKeyResolver(userIdKeyResolver());
                                    config.setRateLimiter(lenientRateLimiter());
                                })
                                .retry(config -> {
                                    config.setRetries(3);
                                    config.setStatuses(HttpStatus.BAD_GATEWAY, HttpStatus.SERVICE_UNAVAILABLE);
                                    config.setMethods(HttpMethod.GET);
                                    config.setBackoff(Duration.ofMillis(50), Duration.ofMillis(500), 2, false);
                                })
                                .circuitBreaker(cb -> cb.setName("presence-circuit-breaker")
                                        .setFallbackUri("forward:/api/fallback/presence")))
                        .uri("lb://presence"))
                .build();
    }
}
