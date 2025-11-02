package com.chatwolf.api.exception;

public class ForbiddenException extends RuntimeException {
    private static final long serialVersionUID = -8118352236608396060L;

    public ForbiddenException(String message) {
        super(message);
    }
}
