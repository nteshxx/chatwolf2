package com.chatwolf.auth.exception;

public class ResourceNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 3580349493796783852L;

    public ResourceNotFoundException(String errorMessage) {
        super(errorMessage);
    }
}
