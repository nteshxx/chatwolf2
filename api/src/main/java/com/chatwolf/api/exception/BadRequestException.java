package com.chatwolf.api.exception;

public class BadRequestException extends RuntimeException {
    private static final long serialVersionUID = 6028024395632470697L;

    public BadRequestException(String message) {
        super(message);
    }
}
