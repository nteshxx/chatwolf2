package com.chatwolf.auth.controller;

import com.chatwolf.auth.dto.AuthRespone;
import com.chatwolf.auth.dto.ChangePassword;
import com.chatwolf.auth.dto.Login;
import com.chatwolf.auth.dto.Register;
import com.chatwolf.auth.dto.Token;
import com.chatwolf.auth.entity.User;
import com.chatwolf.auth.service.AuthService;
import com.chatwolf.auth.utility.RequestUtils;
import com.chatwolf.auth.utility.ResponseBuilder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Value("${jwt.refresh-token-expiration-days}")
    private Integer refreshTokenExpirationDays;

    @PostMapping("/initiate-register")
    public ResponseEntity<Object> initiateRegister(
            HttpServletRequest httpRequest, @Valid @RequestBody Register registerDetails) {
        String ip = RequestUtils.getClientIP(httpRequest);
        String userAgent = RequestUtils.getUserAgent(httpRequest);
        registerDetails.setIp(ip);
        registerDetails.setUserAgent(userAgent);

        Boolean isOtpSent = authService.initiateRegister(registerDetails);
        if (isOtpSent) {
            return ResponseBuilder.build(HttpStatus.CREATED, null, "otp sent to email", null);
        }
        return ResponseBuilder.build(HttpStatus.INTERNAL_SERVER_ERROR, null, "registration failed", null);
    }

    @PostMapping("/complete-register")
    public ResponseEntity<Object> completeRegister(
            HttpServletRequest httpRequest, @Valid @RequestBody Register registerDetails) {
        String ip = RequestUtils.getClientIP(httpRequest);
        String userAgent = RequestUtils.getUserAgent(httpRequest);
        registerDetails.setIp(ip);
        registerDetails.setUserAgent(userAgent);

        AuthRespone data = authService.completeRegister(registerDetails);
        ResponseCookie cookie = ResponseCookie.from(
                        "REFRESHTOKEN", data.getToken().getRefreshToken())
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .maxAge(refreshTokenExpirationDays * 24 * 60 * 60)
                .build();
        return ResponseBuilder.build(HttpStatus.CREATED, cookie, "registered", data);
    }

    @PostMapping("/login")
    public ResponseEntity<Object> login(@Valid @RequestBody Login loginDetails) {
        AuthRespone data = authService.login(loginDetails);
        ResponseCookie cookie = ResponseCookie.from(
                        "REFRESHTOKEN", data.getToken().getRefreshToken())
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .maxAge(refreshTokenExpirationDays * 24 * 60 * 60)
                .build();
        return ResponseBuilder.build(HttpStatus.OK, cookie, "logged in", data);
    }

    @GetMapping("/refresh")
    public ResponseEntity<Object> getRefreshToken(
            @CookieValue(name = "REFRESHTOKEN", required = false) String refreshToken) {
        if (refreshToken != null) {
            Token data = authService.refreshToken(refreshToken);
            ResponseCookie cookie = ResponseCookie.from("REFRESHTOKEN", data.getRefreshToken())
                    .httpOnly(true)
                    .secure(true)
                    .sameSite("Strict")
                    .path("/")
                    .maxAge(refreshTokenExpirationDays * 24 * 60 * 60)
                    .build();
            return ResponseBuilder.build(HttpStatus.OK, cookie, "new refresh token generated", null);
        }
        return ResponseBuilder.build(HttpStatus.UNAUTHORIZED, null, "invalid cookie", null);
    }

    @GetMapping("/token")
    public ResponseEntity<Object> getAccessToken(
            @CookieValue(name = "REFRESHTOKEN", required = false) String refreshToken) {
        if (refreshToken != null) {
            Token data = authService.accessToken(refreshToken);
            return ResponseBuilder.build(HttpStatus.OK, null, "new access token generated", data.getAccessToken());
        }
        return ResponseBuilder.build(HttpStatus.UNAUTHORIZED, null, "invalid cookie", null);
    }

    @PostMapping("/validate")
    public ResponseEntity<?> validateToken(@RequestBody Map<String, String> body) {
        String token = body.get("token");
        Optional<Jwt> claims = authService.getClaims(token);
        if (claims.isPresent()) {
            return ResponseEntity.ok(Map.of("valid", true, "claims", claims.get()));
        }
        return ResponseEntity.ok(Map.of("valid", false, "claims", "{}"));
    }

    @GetMapping("/.well-known/jwks.json")
    public Map<String, Object> getJwkSet() {
        return authService.getJwk();
    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @GetMapping("/profile")
    public ResponseEntity<Object> getUserProfile(@AuthenticationPrincipal User user) {
        User userData = authService.getUserById(user.getUserId());
        return ResponseBuilder.build(HttpStatus.OK, null, "success", userData);
    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @PatchMapping("/change-password")
    public ResponseEntity<Object> changePassword(
            @AuthenticationPrincipal User user, @Valid @RequestBody ChangePassword changePassword) {
        if (!changePassword.getNewPassword().equals(changePassword.getConfirmNewPassword())) {
            return ResponseBuilder.build(
                    HttpStatus.BAD_REQUEST, null, "new password and confirm new password mismatch", null);
        }
        boolean isPasswordUpdated = authService.updatePassword(user, changePassword);
        if (!isPasswordUpdated) {
            return ResponseBuilder.build(HttpStatus.BAD_REQUEST, null, "current password is invalid", null);
        }
        return ResponseBuilder.build(HttpStatus.OK, null, "password updated", null);
    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @GetMapping("/logout")
    public ResponseEntity<Object> logout(@CookieValue(name = "REFRESHTOKEN", required = false) String refreshToken) {
        if (refreshToken != null) {
            authService.logout(refreshToken);

            ResponseCookie cookie = ResponseCookie.from("REFRESHTOKEN", "")
                    .httpOnly(true)
                    .secure(true)
                    .sameSite("Strict")
                    .path("/")
                    .maxAge(0)
                    .build();
            return ResponseBuilder.build(HttpStatus.OK, cookie, "logged out", null);
        }
        return ResponseBuilder.build(HttpStatus.UNAUTHORIZED, null, "invalid cookie", null);
    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @GetMapping("/logout-all")
    public ResponseEntity<Object> logoutAll(@CookieValue(name = "REFRESHTOKEN", required = false) String refreshToken) {
        if (refreshToken != null) {
            authService.logoutAll(refreshToken);
            ResponseCookie cookie = ResponseCookie.from("REFRESHTOKEN", "")
                    .httpOnly(true)
                    .secure(true)
                    .sameSite("Strict")
                    .path("/")
                    .maxAge(0)
                    .build();
            return ResponseBuilder.build(HttpStatus.OK, cookie, "logged out of all devices", null);
        }
        return ResponseBuilder.build(HttpStatus.UNAUTHORIZED, null, "invalid cookie", null);
    }
}
