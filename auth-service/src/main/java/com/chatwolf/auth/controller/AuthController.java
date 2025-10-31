package com.chatwolf.auth.controller;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.chatwolf.auth.constant.Role;
import com.chatwolf.auth.dto.ChangePassword;
import com.chatwolf.auth.dto.Login;
import com.chatwolf.auth.dto.Register;
import com.chatwolf.auth.dto.Token;
import com.chatwolf.auth.dto.UserAndToken;
import com.chatwolf.auth.entity.User;
import com.chatwolf.auth.service.AuthService;
import com.chatwolf.auth.utility.CookieExtractor;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Value("${jwt.refresh-token-expiration-days}")
    private Integer refreshTokenExpirationDays;

    @PostMapping("/auth/register")
    public ResponseEntity<Object> signup(@Valid @RequestBody Register signupDetails) {
        UserAndToken data = authService.register(signupDetails, Role.ADMIN);
        ResponseCookie cookie = ResponseCookie.from(
                        "refreshToken", data.getToken().getRefreshToken())
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .maxAge(refreshTokenExpirationDays * 24 * 60 * 60)
                .build();
        return ResponseBuilder.build(HttpStatus.CREATED, cookie, null, "Successfully Signed Up", data);
    }

    @PostMapping("/auth/login")
    public ResponseEntity<Object> login(@Valid @RequestBody Login loginDetails) {
        UserAndToken data = authService.login(loginDetails);
        ResponseCookie cookie = ResponseCookie.from(
                        "refreshToken", data.getToken().getRefreshToken())
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .maxAge(refreshTokenExpirationDays * 24 * 60 * 60)
                .build();
        return ResponseBuilder.build(HttpStatus.OK, cookie, null, "Successfully Logged In", data);
    }

    @GetMapping("/auth/refresh")
    public ResponseEntity<Object> getRefreshToken(HttpServletRequest request) {
        String requestRefreshToken = CookieExtractor.getCookie(request, "refreshToken");
        if (requestRefreshToken != null) {
            Token data = authService.refreshToken(requestRefreshToken);
            ResponseCookie cookie = ResponseCookie.from("refreshToken", data.getRefreshToken())
                    .httpOnly(true)
                    .secure(true)
                    .sameSite("Strict")
                    .path("/")
                    .maxAge(refreshTokenExpirationDays * 24 * 60 * 60)
                    .build();
            return ResponseBuilder.build(HttpStatus.OK, cookie, null, "Successfully Generated New Refresh Token", null);
        }
        return ResponseBuilder.build(HttpStatus.UNAUTHORIZED, null, null, "Refresh Token Cookie Not Found", null);
    }

    @PostMapping("/auth/validate")
    public ResponseEntity<?> validateToken(@RequestBody Map<String, String> body) {
        String token = body.get("token");
        Optional<DecodedJWT> claims = authService.getClaims(token);
        if (claims.isPresent()) {
            return ResponseEntity.ok(Map.of("valid", true, "claims", claims.get()));
        }
        return ResponseEntity.ok(Map.of("valid", false, "claims", "{}"));
    }

    @GetMapping("/auth/token")
    public ResponseEntity<Object> getAccessToken(HttpServletRequest request) {
        String requestRefreshToken = CookieExtractor.getCookie(request, "refreshToken");
        if (requestRefreshToken != null) {
            Token data = authService.accessToken(requestRefreshToken);
            return ResponseBuilder.build(
                    HttpStatus.OK, null, null, "Successfully Generated New Access Token", data.getAccessToken());
        }
        return ResponseBuilder.build(HttpStatus.UNAUTHORIZED, null, null, "Refresh Token Cookie Not Found", null);
    }

    @PreAuthorize("hasRole('USER')")
    @GetMapping("/auth/me")
    public ResponseEntity<Object> getUserProfile(@AuthenticationPrincipal User user) {
        User userData = authService.getUserById(user.getUserId());
        return ResponseBuilder.build(HttpStatus.OK, null, null, "Success", userData);
    }

    @PreAuthorize("hasRole('USER')")
    @PatchMapping("/auth/change-password")
    public ResponseEntity<Object> changePassword(
            @AuthenticationPrincipal User user, @Valid @RequestBody ChangePassword changePassword) {
        if (!changePassword.getNewPassword().equals(changePassword.getConfirmNewPassword())) {
            return ResponseBuilder.build(
                    HttpStatus.BAD_REQUEST,
                    null,
                    "Failure",
                    "New Password and Confirm New Password do not match",
                    null);
        }
        boolean isPasswordUpdated = authService.updatePassword(user, changePassword);
        if (!isPasswordUpdated) {
            return ResponseBuilder.build(HttpStatus.BAD_REQUEST, null, "Failure!", "Invalid Old Password", null);
        }
        return ResponseBuilder.build(HttpStatus.OK, null, null, "Password Changed Successfully", null);
    }

    @PreAuthorize("hasRole('USER')")
    @GetMapping("/auth/logout")
    public ResponseEntity<Object> logout(HttpServletRequest request) {
        String requestRefreshToken = CookieExtractor.getCookie(request, "refreshToken");
        if (requestRefreshToken != null) {
            authService.logout(requestRefreshToken);

            ResponseCookie cookie = ResponseCookie.from("refreshToken", "")
                    .httpOnly(true)
                    .secure(true)
                    .sameSite("Strict")
                    .path("/")
                    .maxAge(0)
                    .build();
            return ResponseBuilder.build(HttpStatus.OK, cookie, null, "Successfully Logged Out", null);
        }
        return ResponseBuilder.build(HttpStatus.UNAUTHORIZED, null, null, "Refresh Token Cookie Not Found", null);
    }

    @PreAuthorize("hasRole('USER')")
    @GetMapping("/auth/logout-all")
    public ResponseEntity<Object> logoutAll(HttpServletRequest request) {
        String requestRefreshToken = CookieExtractor.getCookie(request, "refreshToken");
        if (requestRefreshToken != null) {
            authService.logoutAll(requestRefreshToken);
            ResponseCookie cookie = ResponseCookie.from("refreshToken", "")
                    .httpOnly(true)
                    .secure(true)
                    .sameSite("Strict")
                    .path("/")
                    .maxAge(0)
                    .build();
            return ResponseBuilder.build(HttpStatus.OK, cookie, null, "Successfully Logged Out Of All Devices", null);
        }
        return ResponseBuilder.build(HttpStatus.UNAUTHORIZED, null, null, "Refresh Token Cookie Not Found", null);
    }
}
