package com.chatwolf.auth.utility;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class TokenHasher {

    private static final String HASH_ALGORITHM = "SHA-256";

    public static String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] hashedBytes = digest.digest(token.getBytes());
            return Base64.getEncoder().encodeToString(hashedBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error hashing token", e);
        }
    }

    public static boolean verifyToken(String token, String hashedToken) {
        String hashedInputToken = hashToken(token);
        return hashedInputToken.equals(hashedToken);
    }
}
