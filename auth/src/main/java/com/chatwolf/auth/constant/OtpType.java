package com.chatwolf.auth.constant;

public enum OtpType {
    REGISTRATION("Registration", 10),
    LOGIN("Login", 5),
    PASSWORD_RESET("Password Reset", 15);

    private final String displayName;
    private final int expiryMinutes;

    OtpType(String displayName, int expiryMinutes) {
        this.displayName = displayName;
        this.expiryMinutes = expiryMinutes;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getExpiryMinutes() {
        return expiryMinutes;
    }
}
