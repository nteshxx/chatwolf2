package com.chatwolf.api.exception;

public class UnauthorizedException extends RuntimeException {
    private static final long serialVersionUID = -3672622460523118572L;

    public UnauthorizedException(String message) {
        super(message);
    }
}
