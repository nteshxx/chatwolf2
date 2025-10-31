package com.chatwolf.auth.exception;

import com.chatwolf.auth.utility.ResponseBuilder;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
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
        return ResponseBuilder.build(
                HttpStatus.INTERNAL_SERVER_ERROR, null, ex.getMessage(), "Internal Server Error", null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        Map<String, String> errorMessages = new HashMap<>();
        // build error message
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String message = error.getDefaultMessage();
            errorMessages.put(fieldName, message);
        });
        String message = errorMessages.values().stream().collect(Collectors.joining(", "));
        return ResponseBuilder.build(HttpStatus.BAD_REQUEST, null, "Bad Request", message, null);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<Object> handleBadRequestException(BadRequestException ex) {
        return ResponseBuilder.build(HttpStatus.BAD_REQUEST, null, "Bad Request", ex.getMessage(), null);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Object> handleIncorrectLoginCredentialsException(UnauthorizedException ex) {
        return ResponseBuilder.build(HttpStatus.UNAUTHORIZED, null, "Unauthorized", ex.getMessage(), null);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Object> handleResourceNotFoundException(ResourceNotFoundException ex) {
        return ResponseBuilder.build(HttpStatus.NOT_FOUND, null, "Not Found", ex.getMessage(), null);
    }
}
