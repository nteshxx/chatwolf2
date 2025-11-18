package com.chatwolf.presence.controller;

import com.chatwolf.presence.dto.PresenceEvent;
import com.chatwolf.presence.dto.PresenceResponse;
import com.chatwolf.presence.service.PresenceService;
import java.time.Duration;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/api/presence")
@AllArgsConstructor
public class PresenceController {

    private final PresenceService presenceService;

    /**
     * Get presence status for a specific user
     * GET /api/presence/{userId}
     */
    @GetMapping("/{userId}")
    public Mono<PresenceResponse> getPresence(@PathVariable String userId) {
        log.debug("Fetching presence for userId={}", userId);
        return presenceService.getPresence(userId).map(status -> new PresenceResponse(userId, status));
    }

    /**
     * Server-Sent Events endpoint for real-time presence updates
     * GET /api/presence/stream
     */
    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<PresenceEvent>> streamPresence() {
        log.debug("New SSE connection established");

        // Presence events
        Flux<ServerSentEvent<PresenceEvent>> events = presenceService
                .subscribeToPresence()
                .map(event -> ServerSentEvent.<PresenceEvent>builder()
                        .id(event.getEventId())
                        .event("presence")
                        .data(event)
                        .build());

        // Keepalive comments every 30 seconds
        Flux<ServerSentEvent<PresenceEvent>> keepalive = Flux.interval(Duration.ofSeconds(30))
                .map(tick -> ServerSentEvent.<PresenceEvent>builder()
                        .comment("keepalive")
                        .build());

        return Flux.merge(events, keepalive)
                .doOnCancel(() -> log.debug("SSE connection closed"))
                .doOnError(ex -> log.error("SSE connection error", ex));
    }
}
