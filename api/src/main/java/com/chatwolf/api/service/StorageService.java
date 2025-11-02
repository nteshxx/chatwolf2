package com.chatwolf.api.service;

import com.chatwolf.api.exception.StorageServiceUnavailableException;
import com.chatwolf.api.repository.StorageClient;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class StorageService {

    private final StorageClient storageClient;

    @Retry(name = "storageService", fallbackMethod = "retryFallback")
    @CircuitBreaker(name = "storageService", fallbackMethod = "circuitBreakerFallback")
    public String getPresignedUrl(String objectId) {
        log.debug("Fetching presigned URL for objectId: {}", objectId);
        return storageClient.getPresignedUrl(objectId);
    }

    /**
     * Fallback for retry failures
     * This is called after all retry attempts are exhausted
     * @throws Exception
     */
    public String retryFallback(String objectId, Exception ex) throws Exception {
        log.warn("Retry exhausted for objectId: {}. Error: {}", objectId, ex.getMessage());
        throw new StorageServiceUnavailableException(
                ex.getMessage(), ex); // Re-throw to trigger circuit breaker fallback
    }

    /**
     * Fallback for circuit breaker
     * This is the final fallback when circuit is open or after retry failures
     */
    public String circuitBreakerFallback(String objectId, Throwable t) {
        log.error("Circuit breaker activated for storage service. ObjectId: {}. Error: {}", objectId, t.getMessage());
        // Return a default/error URL or throw custom exception
        return generateFallbackUrl(objectId);
    }

    /**
     * Generate a fallback URL when storage service is unavailable
     */
    private String generateFallbackUrl(String objectId) {
        // Option 1: Return a placeholder/error URL
        return "https://fallback.storage.url/unavailable?objectId=" + objectId;

        // Option 2: Return null and handle in controller
        // return null;

        // Option 3: Throw custom exception
        // throw new StorageServiceUnavailableException("Storage service is currently unavailable");
    }
}
