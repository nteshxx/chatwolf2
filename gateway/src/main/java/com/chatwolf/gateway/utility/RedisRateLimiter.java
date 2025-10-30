package com.chatwolf.gateway.utility;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.ratelimit.AbstractRateLimiter;
import org.springframework.cloud.gateway.support.ConfigurationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Redis-backed rate limiter using token bucket algorithm.
 * Distributes rate limiting across multiple gateway instances.
 */
@Slf4j
@Component
public class RedisRateLimiter implements 

@Component
class UserKeyResolver implements org.springframework.cloud.gateway.filter.ratelimit.KeyResolver {
    @Override
    public Mono<String> resolve(ServerWebExchange exchange) {
        String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
        return Mono.just(userId != null ? userId : "anonymous");
    }
}

@Component
class IpKeyResolver implements org.springframework.cloud.gateway.filter.ratelimit.KeyResolver {
    @Override
    public Mono<String> resolve(ServerWebExchange exchange) {
        return Mono.just(
                Objects.requireNonNull(exchange.getRequest().getRemoteAddress()).getAddress().getHostAddress()
        );
    }
}
	
	@Autowired
    private RedisRateLimiter redisRateLimiter;

    public static final String REPLENISH_RATE_KEY = "replenish-rate";
    public static final String BURSTCAPACITY_KEY = "burst-capacity";
    public static final String REQUESTED_TOKENS_KEY = "requested-tokens";

    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final RedisScript<List<Long>> script;
    
    public RedisRateLimiter(ReactiveRedisTemplate<String, String> redisTemplate,
                           RedisScript<List<Long>> rateLimiterScript) {
        this.redisTemplate = redisTemplate;
        this.script = rateLimiterScript;
    }

    @Override
    public Mono<Response> isAllowed(String routeId, String id) {
        return isAllowed(routeId, id, 1);
    }

    public Mono<Response> isAllowed(String routeId, String id, int tokens) {
        List<String> keys = getKeys(id);

        // Script arguments: [replenishRate, burstCapacity, currentTime, requestedTokens]
        int replenishRate = getConfig().getReplenishRate();
        int burstCapacity = getConfig().getBurstCapacity();
        long now = System.currentTimeMillis();

        List<String> scriptArgs = new ArrayList<>();
        scriptArgs.add(String.valueOf(replenishRate));
        scriptArgs.add(String.valueOf(burstCapacity));
        scriptArgs.add(String.valueOf(now));
        scriptArgs.add(String.valueOf(tokens));

        return redisTemplate.execute(script, keys, scriptArgs)
                .onErrorResume(e -> {
                    log.error("Redis error in rate limiter for key: {}", id, e);
                    // Fail open - allow request if Redis is down
                    return Mono.just(List.of(1L, -1L));
                })
                .map(result -> {
                    boolean allowed = result.get(0) == 1L;
                    long tokensLeft = result.get(1);

                    log.debug("Rate limit check - Key: {}, Allowed: {}, TokensLeft: {}", 
                            id, allowed, tokensLeft);

                    return new Response(allowed, getHeaders(tokensLeft));
                });
    }

    private List<String> getKeys(String id) {
        String prefix = "rate-limiter:";
        String tokenKey = prefix + id + ":tokens";
        String timestampKey = prefix + id + ":timestamp";

        List<String> keys = new ArrayList<>();
        keys.add(tokenKey);
        keys.add(timestampKey);
        return keys;
    }

    private com.google.common.collect.ImmutableMap<String, String> getHeaders(long tokensLeft) {
        return com.google.common.collect.ImmutableMap.of(
                "X-RateLimit-Remaining", String.valueOf(Math.max(tokensLeft, 0)),
                "X-RateLimit-Limit", String.valueOf(getConfig().getBurstCapacity())
        );
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Config {
        private int replenishRate = 100; // tokens per second
        private int burstCapacity = 200; // max tokens in bucket
        private int requestedTokens = 1;
    }
}

/**
 * Lua script for atomic token bucket rate limiting
 * Returns: [allowed (1/0), tokens_left]
 */
@Configuration
class RedisScriptConfig {

    @Bean
    public RedisScript<List<Long>> rateLimiterScript() {
        String script = "local key = KEYS[1]\n" +
                "local timestamp_key = KEYS[2]\n" +
                "local replenish_rate = tonumber(ARGV[1])\n" +
                "local burst_capacity = tonumber(ARGV[2])\n" +
                "local current_time = tonumber(ARGV[3])\n" +
                "local requested_tokens = tonumber(ARGV[4])\n" +
                "\n" +
                "local current_tokens = tonumber(redis.call('GET', key)) or burst_capacity\n" +
                "local last_refill_time = tonumber(redis.call('GET', timestamp_key)) or current_time\n" +
                "\n" +
                "-- Calculate tokens to add based on time elapsed\n" +
                "local time_passed = math.max(current_time - last_refill_time, 0) / 1000\n" +
                "local tokens_to_add = time_passed * replenish_rate\n" +
                "current_tokens = math.min(current_tokens + tokens_to_add, burst_capacity)\n" +
                "\n" +
                "-- Check if request is allowed\n" +
                "local allowed = 0\n" +
                "if current_tokens >= requested_tokens then\n" +
                "  allowed = 1\n" +
                "  current_tokens = current_tokens - requested_tokens\n" +
                "end\n" +
                "\n" +
                "-- Update Redis with new token count and timestamp\n" +
                "redis.call('SET', key, current_tokens)\n" +
                "redis.call('SET', timestamp_key, current_time)\n" +
                "redis.call('EXPIRE', key, 3600)\n" +
                "redis.call('EXPIRE', timestamp_key, 3600)\n" +
                "\n" +
                "return {allowed, math.floor(current_tokens)}\n";

        return RedisScript.of(script, List.class);
    }
}