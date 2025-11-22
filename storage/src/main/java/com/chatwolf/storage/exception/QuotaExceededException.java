package com.chatwolf.storage.exception;

public class QuotaExceededException extends StorageException {

    private static final long serialVersionUID = 1937192305094503457L;

    public QuotaExceededException(String message) {
        super(message);
    }
}
