package com.chatwolf.presence.controller;

import com.chatwolf.presence.dto.PresenceEvent;
import com.chatwolf.presence.service.PresenceService;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/presence")
@RequiredArgsConstructor
public class PresenceController {

    private final PresenceService presenceService;

    @GetMapping("/{userId}")
    public Mono<String> getPresence(@PathVariable String userId) {
        return Mono.justOrEmpty(presenceService.getPresence(userId)).defaultIfEmpty("offline");
    }

    /**
     * SSE endpoint that streams presence events globally.
     * Clients can connect and filter on userId on the client side.
     */
    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<PresenceEvent> streamPresence() {
        // keep alive and also forward events
        Flux<PresenceEvent> events = presenceService.subscribeAll();
        // optional keepalive ping every 30s — map to no-op event or comment
        Flux<PresenceEvent> keepalive = Flux.interval(Duration.ofSeconds(30)).flatMap(t -> Flux.empty());
        return Flux.merge(events, keepalive);
    }
}
