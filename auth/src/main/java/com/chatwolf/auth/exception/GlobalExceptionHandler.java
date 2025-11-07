package com.chatwolf.auth.exception;

import com.chatwolf.auth.utility.ResponseBuilder;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleInternalServerError(Exception ex) {
        return ResponseBuilder.build(HttpStatus.INTERNAL_SERVER_ERROR, null, "something went wrong", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        Map<String, String> errorMessages = new HashMap<>();
        // build error message
        ex.getBindingResult().getAllErrors().forEach(error -> {
            if (error instanceof FieldError fieldError) {
                errorMessages.put(fieldError.getField(), fieldError.getDefaultMessage());
            } else {
                errorMessages.put(error.getObjectName(), error.getDefaultMessage());
            }
        });

        return ResponseBuilder.build(HttpStatus.BAD_REQUEST, null, "validation failed", errorMessages);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<Object> handleBadRequestException(BadRequestException ex) {
        return ResponseBuilder.build(HttpStatus.BAD_REQUEST, null, "bad request", ex.getMessage());
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Object> handleIncorrectLoginCredentialsException(UnauthorizedException ex) {
        return ResponseBuilder.build(HttpStatus.UNAUTHORIZED, null, "unauthorized", ex.getMessage());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Object> handleResourceNotFoundException(ResourceNotFoundException ex) {
        return ResponseBuilder.build(HttpStatus.NOT_FOUND, null, "not found", ex.getMessage());
    }
}
