package com.chatwolf.gateway.exception;

public class ForbiddenException extends RuntimeException {

    private static final long serialVersionUID = 776605793608148079L;

    public ForbiddenException(String errorMessage) {
        super(errorMessage);
    }
}
