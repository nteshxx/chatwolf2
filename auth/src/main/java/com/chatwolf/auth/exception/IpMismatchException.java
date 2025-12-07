package com.chatwolf.auth.exception;

public class IpMismatchException extends RuntimeException {
    private static final long serialVersionUID = -499895821775035643L;

    public IpMismatchException(String message) {
        super(message);
    }
}
