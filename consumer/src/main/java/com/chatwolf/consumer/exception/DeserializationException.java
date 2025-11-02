package com.chatwolf.consumer.exception;

public class DeserializationException extends RuntimeException {

    private static final long serialVersionUID = 6364152580655552124L;

    public DeserializationException(String message) {
        super(message);
    }

    public DeserializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
