package com.chatwolf.auth.exception;

public class TooManyRequestsException extends RuntimeException {
    private static final long serialVersionUID = -3015121763188074296L;

    public TooManyRequestsException(String message) {
        super(message);
    }
}
