package com.chatwolf.storage.exception;

public class StorageException extends RuntimeException {

    private static final long serialVersionUID = -6696614332978563407L;

    public StorageException(String message) {
        super(message);
    }

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
