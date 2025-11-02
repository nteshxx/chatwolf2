package com.chatwolf.api.service;

import com.chatwolf.api.dto.UserDTO;
import com.chatwolf.api.repository.AuthClient;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthClient authClient;

    @Retry(name = "authService")
    @CircuitBreaker(name = "authService", fallbackMethod = "fallbackUserDetails")
    public ResponseEntity<UserDTO> getUserById(String userId) {
        log.debug("Attempting to get user: {}", userId);
        return authClient.getUserById(userId);
    }

    public ResponseEntity<UserDTO> fallbackUserDetails(String userId, Throwable t) {
        log.error("Failed to get user {} after retries. Circuit breaker activated. Error: {}", userId, t.getMessage());

        UserDTO fallbackUser = createFallbackUser(userId);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(fallbackUser);
    }

    private UserDTO createFallbackUser(String userId) {
        UserDTO user = new UserDTO();
        user.setId(userId);
        user.setUsername("Guest_" + userId);
        return user;
    }
}
