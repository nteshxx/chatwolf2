package com.chatwolf.auth.exception;

public class RateLimitExceededException extends RuntimeException {
    private static final long serialVersionUID = -2765340727516303488L;

    public RateLimitExceededException(String message) {
        super(message);
    }
}
