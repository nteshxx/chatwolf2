package com.chatwolf.gateway.utility;

import org.springframework.http.HttpStatus;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@org.springframework.web.bind.annotation.RestController
@org.springframework.web.bind.annotation.RequestMapping("/fallback")
public class FallbackController {

	@org.springframework.web.bind.annotation.GetMapping("/auth")
	public Mono<org.springframework.http.ResponseEntity<?>> authFallback() {
		log.warn("Auth service fallback triggered");
		return Mono.just(org.springframework.http.ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
				.body(java.util.Map.of("error", "Auth service temporarily unavailable")));
	}

	@org.springframework.web.bind.annotation.GetMapping("/users")
	public Mono<org.springframework.http.ResponseEntity<?>> usersFallback() {
		log.warn("Users service fallback triggered");
		return Mono.just(org.springframework.http.ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
				.body(java.util.Map.of("error", "User service temporarily unavailable")));
	}

	@org.springframework.web.bind.annotation.GetMapping("/messages")
	public Mono<org.springframework.http.ResponseEntity<?>> messagesFallback() {
		log.warn("Message service fallback triggered");
		return Mono.just(org.springframework.http.ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
				.body(java.util.Map.of("error", "Message service temporarily unavailable")));
	}
}