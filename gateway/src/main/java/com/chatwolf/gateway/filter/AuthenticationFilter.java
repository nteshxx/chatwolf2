package com.chatwolf.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.chatwolf.gateway.utility.JwtTokenProvider;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

@Slf4j
@Component
public class AuthenticationFilter implements GatewayFilter {

	private final JwtTokenProvider tokenProvider;

	public AuthenticationFilter(JwtTokenProvider tokenProvider) {
		this.tokenProvider = tokenProvider;
	}

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		ServerHttpRequest request = exchange.getRequest();

		// Extract token from Authorization header
		String token = tokenProvider.extractToken(request);

		if (token == null || !tokenProvider.validateToken(token)) {
			return onError(exchange, "Invalid or missing token", HttpStatus.UNAUTHORIZED);
		}

		DecodedJWT claims = tokenProvider.getClaimsFromToken(token);
		String userId = claims.getSubject();

		// Add user info to request headers
		ServerHttpRequest mutatedRequest = request.mutate().header("X-User-Id", userId)
				.header("X-Trace-Id", exchange.getAttribute("X-Trace-Id")).build();

		return chain.filter(exchange.mutate().request(mutatedRequest).build())
				.contextWrite(Context.of("userId", userId, "token", token));

	}

	private Mono<Void> onError(ServerWebExchange exchange, String err, HttpStatus httpStatus) {
		ServerHttpResponse response = exchange.getResponse();
		response.setStatusCode(httpStatus);
		response.getHeaders().setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
		String body = "{\"error\": \"" + err + "\", \"status\": " + httpStatus.value() + "}";
		return response.writeWith(Mono.just(response.bufferFactory().wrap(body.getBytes())));
	}

}
