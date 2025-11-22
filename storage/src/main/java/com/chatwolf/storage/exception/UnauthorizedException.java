package com.chatwolf.storage.exception;

public class UnauthorizedException extends StorageException {

    private static final long serialVersionUID = 7013087234293834818L;

    public UnauthorizedException(String message) {
        super(message);
    }
}
