package com.chatwolf.auth.exception;

public class MaxAttemptsExceededException extends RuntimeException {
    private static final long serialVersionUID = 8276240734636413492L;

    public MaxAttemptsExceededException(String message) {
        super(message);
    }
}
