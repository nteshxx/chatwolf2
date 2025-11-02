package com.chatwolf.api.exception;

public class InternalServerErrorException extends RuntimeException {

    private static final long serialVersionUID = -8985937236500174357L;

    public InternalServerErrorException(String message) {
        super(message);
    }
}
