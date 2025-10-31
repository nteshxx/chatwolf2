package com.chatwolf.gateway.exception;

public class UnauthorizedException extends RuntimeException {

    private static final long serialVersionUID = -9058534859794933517L;

    public UnauthorizedException(String errorMessage) {
        super(errorMessage);
    }
}
