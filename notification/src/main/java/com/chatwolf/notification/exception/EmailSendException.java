package com.chatwolf.notification.exception;

public class EmailSendException extends RuntimeException {

    private static final long serialVersionUID = -3994074431644135169L;

    public EmailSendException(String message, Throwable cause) {
        super(message, cause);
    }
}
