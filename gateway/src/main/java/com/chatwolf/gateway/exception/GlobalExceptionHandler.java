package com.chatwolf.gateway.exception;

import com.chatwolf.gateway.utility.ResponseBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import reactor.core.publisher.Mono;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Mono<Object> handleInternalServerError(Exception ex) {
        return Mono.just(ResponseBuilder.build(
                        HttpStatus.INTERNAL_SERVER_ERROR, null, "Internal Server Error", ex.getMessage(), null)
                .getBody());
    }

    @ExceptionHandler(ForbiddenException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Mono<Object> handleForbiddenException(ForbiddenException ex) {
        return Mono.just(ResponseBuilder.build(HttpStatus.FORBIDDEN, null, "Forbidden", ex.getMessage(), null)
                .getBody());
    }

    @ExceptionHandler(UnauthorizedException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Mono<Object> handleUnauthorizedException(UnauthorizedException ex) {
        return Mono.just(ResponseBuilder.build(HttpStatus.UNAUTHORIZED, null, "Unauthorized", ex.getMessage(), null)
                .getBody());
    }
}
