package com.chatwolf.api.service;

import com.chatwolf.api.repository.NotificationClient;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationClient notificationClient;

    /**
     * Send notification with retry and circuit breaker
     * If fails, just log and continue (don't block main flow)
     */
    @Async
    @Retry(name = "notificationService")
    @CircuitBreaker(name = "notificationService", fallbackMethod = "sendNotificationFallback")
    public void sendNotification(Map<String, Object> notification) {
        log.debug("Sending notification: {}", notification);
        notificationClient.sendNotification(notification);
        log.info("Notification sent successfully");
    }

    /**
     * Fallback method - just log and continue
     * Don't throw exception to avoid blocking main business logic
     */
    public void sendNotificationFallback(Map<String, Object> notification, Throwable t) {
        log.error(
                "Failed to send notification after retries. Notification: {}. Error: {}", notification, t.getMessage());
        // Notification failed but don't break the main flow
        // Could store in DB for manual retry later if needed
    }

    /**
     * Helper method to create notification payload
     */
    public Map<String, Object> createNotification(String userId, String type, String title, String message) {
        return Map.of(
                "userId", userId,
                "type", type,
                "title", title,
                "message", message,
                "timestamp", System.currentTimeMillis());
    }
}
