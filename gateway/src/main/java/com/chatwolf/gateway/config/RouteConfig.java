package com.chatwolf.gateway.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;

@Configuration
public class RouteConfig {
	
	private final GatewayFilter authenticationFilter;
	private final KeyResolver userKeyResolver;

    public RouteConfig(GatewayFilter authenticationFilter, @Qualifier("UserKeyResolver") KeyResolver userKeyResolver) {
        this.authenticationFilter = authenticationFilter;
        this.userKeyResolver = userKeyResolver;
    }
	
    @Bean
    RouteLocator routes(RouteLocatorBuilder builder) {
        return builder.routes()
                // ==================== AUTH SERVICE ROUTES ====================
        		 .route("AUTH-SERVICE", r -> r.path("/api/v1/auth/**")
                         .filters(f -> f.filter(authenticationFilter))
                         .uri("lb://AUTH-SERVICE"))

                // ==================== USER SERVICE ROUTES ====================
                .route("user-profile", r -> r
                        .path("/api/v1/users/profile")
                        .filters(f -> f
                                .filter(authenticationFilter)
                                .requestRateLimiter(config -> {
                                    config.setKeyResolver(userKeyResolver);
                                    config.setRateLimiter(new RedisRateLimiter(100, 200));
                                }))
                        .uri("http://user-service:8002"))

                .route("user-update", r -> r
                        .path("/api/v1/users/profile")
                        .and().method("PUT")
                        .filters(f -> f
                                .filter(authenticationFilter)
                                .requestRateLimiter(config -> {
                                    config.setKeyResolver(userKeyResolver);
                                    config.setRateLimiter(new RedisRateLimiter(50, 100));
                                }))
                        .uri("http://user-service:8002"))

                .route("user-search", r -> r
                        .path("/api/v1/users/search")
                        .filters(f -> f
                                .filter(authenticationFilter)
                                .requestRateLimiter(config -> {
                                    config.setKeyResolver(userKeyResolver);
                                    config.setRateLimiter(new RedisRateLimiter(100, 200));
                                }))
                        .uri("http://user-service:8002"))

                .route("user-friends-list", r -> r
                        .path("/api/v1/users/friends")
                        .filters(f -> f
                                .filter(authenticationFilter)
                                .requestRateLimiter(config -> {
                                    config.setKeyResolver(userKeyResolver);
                                    config.setRateLimiter(new RedisRateLimiter(100, 200));
                                }))
                        .uri("http://user-service:8002"))

                .route("user-add-friend", r -> r
                        .path("/api/v1/users/friends/add")
                        .and().method("POST")
                        .filters(f -> f
                                .filter(authenticationFilter)
                                .requestRateLimiter(config -> {
                                    config.setKeyResolver(userKeyResolver);
                                    config.setRateLimiter(new RedisRateLimiter(50, 100));
                                }))
                        .uri("http://user-service:8002"))

                .route("user-presence", r -> r
                        .path("/api/v1/users/presence")
                        .filters(f -> f
                                .filter(authenticationFilter)
                                .requestRateLimiter(config -> {
                                    config.setKeyResolver(userKeyResolver);
                                    config.setRateLimiter(new RedisRateLimiter(500, 1000));
                                }))
                        .uri("http://user-service:8002"))

                // ==================== CHAT SERVICE ROUTES ====================
                .route("chat-create", r -> r
                        .path("/api/v1/chat/create")
                        .and().method("POST")
                        .filters(f -> f
                                .filter(authenticationFilter)
                                .requestRateLimiter(config -> {
                                    config.setKeyResolver(userKeyResolver);
                                    config.setRateLimiter(new RedisRateLimiter(50, 100));
                                }))
                        .uri("http://chat-service:8003"))

                .route("chat-websocket", r -> r
                        .path("/api/v1/chat/ws")
                        .filters(f -> f
                                .filter(authenticationFilter)
                                .requestRateLimiter(config -> {
                                    config.setKeyResolver(userKeyResolver);
                                    config.setRateLimiter(new RedisRateLimiter(1000, 2000));
                                }))
                        .uri("ws://chat-service:8003"))

                .route("chat-groups", r -> r
                        .path("/api/v1/chat/groups/**")
                        .filters(f -> f
                                .filter(authenticationFilter)
                                .requestRateLimiter(config -> {
                                    config.setKeyResolver(userKeyResolver);
                                    config.setRateLimiter(new RedisRateLimiter(100, 200));
                                }))
                        .uri("http://chat-service:8003"))

                .route("chat-list", r -> r
                        .path("/api/v1/chat/list")
                        .filters(f -> f
                                .filter(authenticationFilter)
                                .requestRateLimiter(config -> {
                                    config.setKeyResolver(userKeyResolver);
                                    config.setRateLimiter(new RedisRateLimiter(100, 200));
                                }))
                        .uri("http://chat-service:8003"))

                // ==================== MESSAGE SERVICE ROUTES ====================
                .route("message-send", r -> r
                        .path("/api/v1/messages/send")
                        .and().method("POST")
                        .filters(f -> f
                                .filter(authenticationFilter)
                                .requestRateLimiter(config -> {
                                    config.setKeyResolver(userKeyResolver);
                                    config.setRateLimiter(new RedisRateLimiter(300, 600));
                                }))
                        .uri("http://message-service:8004"))

                .route("message-history", r -> r
                        .path("/api/v1/messages/history/**")
                        .filters(f -> f
                                .filter(authenticationFilter)
                                .requestRateLimiter(config -> {
                                    config.setKeyResolver(userKeyResolver);
                                    config.setRateLimiter(new RedisRateLimiter(100, 200));
                                }))
                        .uri("http://message-service:8004"))

                .route("message-search", r -> r
                        .path("/api/v1/messages/search")
                        .filters(f -> f
                                .filter(authenticationFilter)
                                .requestRateLimiter(config -> {
                                    config.setKeyResolver(userKeyResolver);
                                    config.setRateLimiter(new RedisRateLimiter(50, 100));
                                }))
                        .uri("http://message-service:8004"))

                .route("message-delete", r -> r
                        .path("/api/v1/messages/{id}")
                        .and().method("DELETE")
                        .filters(f -> f
                                .filter(authenticationFilter)
                                .requestRateLimiter(config -> {
                                    config.setKeyResolver(userKeyResolver);
                                    config.setRateLimiter(new RedisRateLimiter(100, 200));
                                }))
                        .uri("http://message-service:8004"))

                .route("message-update", r -> r
                        .path("/api/v1/messages/{id}")
                        .and().method("PUT")
                        .filters(f -> f
                                .filter(authenticationFilter)
                                .requestRateLimiter(config -> {
                                    config.setKeyResolver(userKeyResolver);
                                    config.setRateLimiter(new RedisRateLimiter(100, 200));
                                }))
                        .uri("http://message-service:8004"))

                .route("message-reactions", r -> r
                        .path("/api/v1/messages/reactions/**")
                        .filters(f -> f
                                .filter(authenticationFilter)
                                .requestRateLimiter(config -> {
                                    config.setKeyResolver(userKeyResolver);
                                    config.setRateLimiter(new RedisRateLimiter(200, 400));
                                }))
                        .uri("http://message-service:8004"))

                // ==================== HEALTH & METRICS ROUTES ====================
                .route("health", r -> r
                        .path("/health")
                        .uri("http://localhost:8080"))

                .route("metrics", r -> r
                        .path("/metrics")
                        .uri("http://localhost:8080"))

                .route("prometheus", r -> r
                        .path("/prometheus")
                        .uri("http://localhost:8080"))

                .build();
    }
}
