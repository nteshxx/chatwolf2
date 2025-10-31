package com.chatwolf.gateway.utility;

import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

	@Value("${jwt.secret}")
    private String jwtSecret;

    private Algorithm getAlgorithm() {
        return Algorithm.HMAC256(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String extractToken(ServerHttpRequest request) {
        String bearer = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }

    public boolean validateToken(String token) {
        try {
            JWTVerifier verifier = JWT.require(getAlgorithm()).build();
            verifier.verify(token);
            return true;
        } catch (JWTVerificationException e) {
            log.error("JWT validation error: {}", e.getMessage());
            return false;
        }
    }

    public DecodedJWT getClaimsFromToken(String token) {
        try {
            JWTVerifier verifier = JWT.require(getAlgorithm()).build();
            DecodedJWT decoded = verifier.verify(token);
            return decoded;
        } catch (JWTVerificationException e) {
            log.error("Error extracting claims: {}", e.getMessage());
            return null;
        }
    }
}

