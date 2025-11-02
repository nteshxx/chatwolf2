package com.chatwolf.consumer.exception;

public class NonRecoverableException extends RuntimeException {

    private static final long serialVersionUID = -8807121955547580084L;

    public NonRecoverableException(String message) {
        super(message);
    }

    public NonRecoverableException(String message, Throwable cause) {
        super(message, cause);
    }
}
