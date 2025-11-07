package com.chatwolf.gateway.controller;

import com.chatwolf.gateway.utility.ResponseBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @RequestMapping(
            value = "/auth",
            method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PATCH})
    public Mono<ResponseEntity<Object>> authFallback() {
        log.warn("Auth service fallback triggered");
        return Mono.just(
                ResponseBuilder.build(HttpStatus.SERVICE_UNAVAILABLE, "auth service temporarily unavailable", null));
    }
}
