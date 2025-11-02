package com.chatwolf.auth.service;

import com.chatwolf.auth.entity.RefreshToken;
import com.chatwolf.auth.entity.User;
import com.chatwolf.auth.exception.UnauthorizedException;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class JwtService {

    private static final String role = "role";
    private static final String tokenId = "tokenId";

    private final JWKSet jwkSet;
    private final RSAKey rsaKey;
    private final String issuer;
    private final Long accessTokenExpirationMs;
    private final Long refreshTokenExpirationMs;

    public JwtService(
            JWKSet jwkSet,
            RSAKey rsaKey,
            @Value("${jwt.issuer}") String issuer,
            @Value("${jwt.refresh-token-expiration-days}") Long refreshTokenExpirationDays,
            @Value("${jwt.access-token-expiration-minutes}") Long accessTokenExpirationMinutes) {
        this.jwkSet = jwkSet;
        this.rsaKey = rsaKey;
        this.issuer = issuer;
        this.accessTokenExpirationMs = accessTokenExpirationMinutes * 60 * 1000;
        this.refreshTokenExpirationMs = refreshTokenExpirationDays * 24 * 60 * 60 * 1000;
    }

    public String generateAccessToken(User user) {
        try {
            // Create JWT claims
            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .subject(String.valueOf(user.getUserId()))
                    .issuer(issuer)
                    .audience("chatwolf-user")
                    .expirationTime(new Date(System.currentTimeMillis() + accessTokenExpirationMs))
                    .notBeforeTime(new Date())
                    .issueTime(new Date())
                    .claim(role, user.getRoles())
                    .build();

            // Create JWT header with key ID
            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                    .keyID(rsaKey.getKeyID())
                    .build();

            // Create signed JWT
            SignedJWT signedJWT = new SignedJWT(header, claimsSet);

            // Sign with private key
            JWSSigner signer = new RSASSASigner(rsaKey.toPrivateKey());
            signedJWT.sign(signer);

            return signedJWT.serialize();

        } catch (Exception e) {
            throw new RuntimeException("Error generating JWT Access token", e);
        }
    }

    public String generateRefreshToken(User user, RefreshToken refreshToken) {
        try {
            // Create JWT claims
            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .subject(String.valueOf(user.getUserId()))
                    .issuer(issuer)
                    .audience("chatwolf-user")
                    .expirationTime(new Date(System.currentTimeMillis() + refreshTokenExpirationMs))
                    .notBeforeTime(new Date())
                    .issueTime(new Date())
                    .claim(role, user.getRoles())
                    .claim(tokenId, refreshToken.getTokenId())
                    .build();

            // Create JWT header with key ID
            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                    .keyID(rsaKey.getKeyID())
                    .build();

            // Create signed JWT
            SignedJWT signedJWT = new SignedJWT(header, claimsSet);

            // Sign with private key
            JWSSigner signer = new RSASSASigner(rsaKey.toPrivateKey());
            signedJWT.sign(signer);

            return signedJWT.serialize();

        } catch (Exception e) {
            throw new RuntimeException("Error generating JWT Refresh token", e);
        }
    }

    public Optional<Jwt> decodeJwtToken(String token) {
        try {
            RSAPublicKey publicKey = rsaKey.toRSAPublicKey();
            JwtDecoder jwtDecoder = NimbusJwtDecoder.withPublicKey(publicKey).build();
            return Optional.of(jwtDecoder.decode(token));
        } catch (JwtException jwtVerfEx) {
            log.error("Jwt Token Verification failed");
            throw new UnauthorizedException("Invalid Jwt Token");
        } catch (JOSEException e) {
            log.error("Jwt Token Verification failed");
            throw new UnauthorizedException("JWT Key Not SUpported");
        } catch (Exception e) {
            throw new UnauthorizedException("Decode JWT Token failed");
        }
    }

    public Boolean validateJwtToken(String token) {
        return decodeJwtToken(token).isPresent();
    }

    public Long getUserIdFromJwtToken(String token) {
        return Long.parseLong(decodeJwtToken(token).get().getSubject());
    }

    public Long getTokenIdFromRefreshToken(String token) {
        return decodeJwtToken(token).get().getClaim(tokenId);
    }

    public Map<String, Object> getJwkSet() {
        return jwkSet.toJSONObject();
    }
}
