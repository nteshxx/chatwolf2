package com.chatwolf.auth.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.chatwolf.auth.entity.RefreshToken;
import com.chatwolf.auth.entity.User;
import com.chatwolf.auth.exception.UnauthorizedException;
import java.util.Date;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private static final Logger logger = LoggerFactory.getLogger(JwtService.class);

    @Value("${jwt.issuer}")
    private String issuer;

    @Value("${jwt.access-token-secret}")
    private String accessTokenSecret;

    @Value("${jwt.refresh-token-secret}")
    private String refreshTokenSecret;

    @Value("${jwt.refresh-token-expiration-days}")
    private Long refreshTokenExpirationDays;

    @Value("${jwt.access-token-expiration-minutes}")
    private Long accessTokenExpirationMinutes;

    private static final String role = "role";
    private static final String tokenId = "tokenId";

    private long accessTokenExpirationMs;
    private long refreshTokenExpirationMs;

    private Algorithm accessTokenAlgorithm;
    private Algorithm refreshTokenAlgorithm;
    private JWTVerifier accessTokenVerifier;
    private JWTVerifier refreshTokenVerifier;

    public JwtService(
            @Value("${jwt.issuer}") String issuer,
            @Value("${jwt.access-token-secret}") String accessTokenSecret,
            @Value("${jwt.refresh-token-secret}") String refreshTokenSecret,
            @Value("${jwt.refresh-token-expiration-days}") Long refreshTokenExpirationDays,
            @Value("${jwt.access-token-expiration-minutes}") Long accessTokenExpirationMinutes) {
        this.accessTokenExpirationMs = accessTokenExpirationMinutes * 60 * 1000;
        this.refreshTokenExpirationMs = refreshTokenExpirationDays * 24 * 60 * 60 * 1000;
        this.accessTokenAlgorithm = Algorithm.HMAC512(accessTokenSecret);
        this.refreshTokenAlgorithm = Algorithm.HMAC512(refreshTokenSecret);
        this.accessTokenVerifier =
                JWT.require(accessTokenAlgorithm).withIssuer(issuer).build();
        this.refreshTokenVerifier =
                JWT.require(refreshTokenAlgorithm).withIssuer(issuer).build();
    }

    public String generateAccessToken(User user) {
        return JWT.create()
                .withIssuer(issuer)
                .withSubject(String.valueOf(user.getUserId()))
                .withClaim(role, user.getRole().toString().toUpperCase())
                .withIssuedAt(new Date())
                .withExpiresAt(new Date(new Date().getTime() + accessTokenExpirationMs))
                .sign(accessTokenAlgorithm);
    }

    public String generateRefreshToken(User user, RefreshToken refreshToken) {
        return JWT.create()
                .withIssuer(issuer)
                .withSubject(String.valueOf(user.getUserId()))
                .withClaim(tokenId, refreshToken.getTokenId())
                .withClaim(role, user.getRole().toString().toUpperCase())
                .withIssuedAt(new Date())
                .withExpiresAt(new Date((new Date()).getTime() + refreshTokenExpirationMs))
                .sign(refreshTokenAlgorithm);
    }

    public Optional<DecodedJWT> decodeAccessToken(String token) {
        try {
            return Optional.of(accessTokenVerifier.verify(token));
        } catch (JWTVerificationException jwtVerfEx) {
            logger.error("Access Token Verification failed");
            throw new UnauthorizedException("Invalid Access Token");
        }
    }

    public Optional<DecodedJWT> decodeRefreshToken(String token) {
        try {
            return Optional.of(refreshTokenVerifier.verify(token));
        } catch (JWTVerificationException e) {
            logger.error("Refresh Token Verification failed");
            throw new UnauthorizedException("Invalid Refresh Token");
        }
    }

    public Boolean validateAccessToken(String token) {
        return decodeAccessToken(token).isPresent();
    }

    public Boolean validateRefreshToken(String token) {
        return decodeRefreshToken(token).isPresent();
    }

    public Long getUserIdFromAccessToken(String token) {
        return Long.parseLong(decodeAccessToken(token).get().getSubject());
    }

    public Long getUserIdFromRefreshToken(String token) {
        return Long.parseLong(decodeRefreshToken(token).get().getSubject());
    }

    public Long getTokenIdFromRefreshToken(String token) {
        return decodeRefreshToken(token).get().getClaim(tokenId).asLong();
    }
}
