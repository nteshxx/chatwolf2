package com.chatwolf.api.exception;

public class ServiceUnavailableException extends RuntimeException {

    private static final long serialVersionUID = -1697893777416837204L;

    public ServiceUnavailableException(String message) {
        super(message);
    }
}
