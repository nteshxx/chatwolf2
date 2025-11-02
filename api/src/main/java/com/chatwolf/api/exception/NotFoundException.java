package com.chatwolf.api.exception;

public class NotFoundException extends RuntimeException {

    private static final long serialVersionUID = 4015000695173385569L;

    public NotFoundException(String message) {
        super(message);
    }
}
