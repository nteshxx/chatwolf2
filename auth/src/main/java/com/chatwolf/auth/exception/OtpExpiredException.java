package com.chatwolf.auth.exception;

public class OtpExpiredException extends RuntimeException {
    private static final long serialVersionUID = 7076209204551571328L;

    public OtpExpiredException(String message) {
        super(message);
    }
}
