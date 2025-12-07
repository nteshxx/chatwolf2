package com.chatwolf.auth.exception;

public class InvalidOtpException extends RuntimeException {
    private static final long serialVersionUID = -2812048498752400929L;

    public InvalidOtpException(String message) {
        super(message);
    }
}
