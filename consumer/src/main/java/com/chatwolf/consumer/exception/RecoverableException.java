package com.chatwolf.consumer.exception;

public class RecoverableException extends RuntimeException {

    private static final long serialVersionUID = 3759999577953853999L;

    public RecoverableException(String message) {
        super(message);
    }

    public RecoverableException(String message, Throwable cause) {
        super(message, cause);
    }
}
