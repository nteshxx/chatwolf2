package com.chatwolf.gateway.exception;

import com.chatwolf.gateway.utility.ResponseBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.MethodNotAllowedException;
import reactor.core.publisher.Mono;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Mono<Object> handleInternalServerError(Exception ex) {
        return Mono.just(
                ResponseBuilder.build(HttpStatus.INTERNAL_SERVER_ERROR, "something went wrong", ex.getMessage()));
    }

    @ExceptionHandler(ForbiddenException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Mono<Object> handleForbiddenException(ForbiddenException ex) {
        return Mono.just(ResponseBuilder.build(HttpStatus.FORBIDDEN, "forbidden", ex.getMessage()));
    }

    @ExceptionHandler(UnauthorizedException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Mono<Object> handleUnauthorizedException(UnauthorizedException ex) {
        return Mono.just(ResponseBuilder.build(HttpStatus.UNAUTHORIZED, "unauthorized", ex.getMessage()));
    }

    @ExceptionHandler(MethodNotAllowedException.class)
    public Mono<Object> handleMethodNotAllowed(MethodNotAllowedException ex) {
        return Mono.just(ResponseBuilder.build(HttpStatus.METHOD_NOT_ALLOWED, "method not allowed", ex.getMessage()));
    }
}
