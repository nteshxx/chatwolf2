package com.chatwolf.api.exception;

public class StorageServiceUnavailableException extends RuntimeException {

    private static final long serialVersionUID = -1958943066369536185L;

    public StorageServiceUnavailableException(String message) {
        super(message);
    }

    public StorageServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
