package com.chatwolf.storage.exception;

public class NotFoundException extends StorageException {

    private static final long serialVersionUID = -1067330833229971935L;

    public NotFoundException(String message) {
        super(message);
    }
}
