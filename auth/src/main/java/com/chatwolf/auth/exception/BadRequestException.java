package com.chatwolf.auth.exception;

public class BadRequestException extends RuntimeException {

    private static final long serialVersionUID = -1165313183593911802L;

    public BadRequestException(String message) {
        super(message);
    }
}
