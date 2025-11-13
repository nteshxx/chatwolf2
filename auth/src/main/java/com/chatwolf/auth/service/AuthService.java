package com.chatwolf.auth.service;

import com.chatwolf.auth.constant.Role;
import com.chatwolf.auth.dto.AuthRespone;
import com.chatwolf.auth.dto.ChangePassword;
import com.chatwolf.auth.dto.Login;
import com.chatwolf.auth.dto.Register;
import com.chatwolf.auth.dto.Token;
import com.chatwolf.auth.entity.RefreshToken;
import com.chatwolf.auth.entity.User;
import com.chatwolf.auth.exception.BadRequestException;
import com.chatwolf.auth.exception.UnauthorizedException;
import com.chatwolf.auth.utility.TokenHasher;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class AuthService {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserService userService;

    @Transactional
    public AuthRespone register(Register signupDetails) {
        Optional<User> existingUser = userService.checkIfUserExists(signupDetails.getEmail());
        if (existingUser.isPresent()) {
            throw new BadRequestException("email already registered");
        }
        User user = User.builder()
                .firstName(signupDetails.getFirstName())
                .lastName(signupDetails.getLastName())
                .email(signupDetails.getEmail())
                .password(passwordEncoder.encode(signupDetails.getPassword()))
                .roles(List.of(Role.USER))
                .build();
        userService.saveUser(user);
        RefreshToken refreshToken = RefreshToken.builder().user(user).build();
        refreshToken = refreshTokenService.saveRefreshToken(refreshToken); // save to get tokenId
        String refreshTokenString = jwtService.generateRefreshToken(user, refreshToken);
        String accessToken = jwtService.generateAccessToken(user);
        refreshToken.setTokenHash(TokenHasher.hashToken(refreshTokenString));
        refreshTokenService.saveRefreshToken(refreshToken); // update tokenHash in database
        return new AuthRespone(user, new Token(accessToken, refreshTokenString));
    }

    @Transactional
    public AuthRespone login(Login loginDetails) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginDetails.getEmail(), loginDetails.getPassword()));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            User user = (User) authentication.getPrincipal();
            RefreshToken refreshToken = RefreshToken.builder().user(user).build();
            refreshToken = refreshTokenService.saveRefreshToken(refreshToken); // save to get tokenId
            String refreshTokenString = jwtService.generateRefreshToken(user, refreshToken);
            String accessToken = jwtService.generateAccessToken(user);
            refreshToken.setTokenHash(TokenHasher.hashToken(refreshTokenString));
            refreshTokenService.saveRefreshToken(refreshToken); // update tokenHash in database
            return new AuthRespone(user, new Token(accessToken, refreshTokenString));

        } catch (BadCredentialsException badCredEx) {
            throw new UnauthorizedException("incorrect username or password");
        } catch (InternalAuthenticationServiceException e) {
            log.error("login error: {}", e.getMessage());
            throw new UnauthorizedException("incorrect username or password");
        }
    }

    @Transactional
    public Token refreshToken(String bearerToken) {
        String cookieRefreshToken = bearerToken.replace("Bearer ", "").trim();
        Long tokenId = jwtService.getTokenIdFromRefreshToken(cookieRefreshToken);
        Optional<RefreshToken> dbRefreshToken = refreshTokenService.findRefreshToken(tokenId);
        if (jwtService.validateJwtToken(cookieRefreshToken)
                && dbRefreshToken.isPresent()
                && TokenHasher.verifyToken(
                        cookieRefreshToken, dbRefreshToken.get().getTokenHash())) {
            // valid and exists in database
            RefreshToken existingRefreshToken = dbRefreshToken.get();
            User user = userService.findById(existingRefreshToken.getUser().getUserId());
            String accessToken = jwtService.generateAccessToken(user);
            String newRefreshToken = jwtService.generateRefreshToken(user, existingRefreshToken);
            existingRefreshToken.setTokenHash(TokenHasher.hashToken(newRefreshToken));
            refreshTokenService.saveRefreshToken(existingRefreshToken); // update tokenHash in database
            Token responseData = new Token(accessToken, newRefreshToken);
            return responseData;
        }

        throw new UnauthorizedException("invalid refresh token");
    }

    public Token accessToken(String bearerToken) {
        String cookieRefreshToken = bearerToken.replace("Bearer ", "").trim();
        Long tokenId = jwtService.getTokenIdFromRefreshToken(cookieRefreshToken);
        Optional<RefreshToken> dbRefreshToken = refreshTokenService.findRefreshToken(tokenId);
        if (jwtService.validateJwtToken(cookieRefreshToken)
                && dbRefreshToken.isPresent()
                && TokenHasher.verifyToken(
                        cookieRefreshToken, dbRefreshToken.get().getTokenHash())) {
            // valid and exists in database
            RefreshToken existingRefreshToken = dbRefreshToken.get();
            User user = userService.findById(existingRefreshToken.getUser().getUserId());
            String accessToken = jwtService.generateAccessToken(user);
            Token responseData = new Token(accessToken, null);
            return responseData;
        }
        throw new UnauthorizedException("invalid access token");
    }

    @Transactional
    public void logout(String bearerToken) {
        String cookieRefreshToken = bearerToken.replace("Bearer ", "").trim();
        Long tokenId = jwtService.getTokenIdFromRefreshToken(cookieRefreshToken);
        Optional<RefreshToken> dbRefreshToken = refreshTokenService.findRefreshToken(tokenId);
        if (jwtService.validateJwtToken(cookieRefreshToken)
                && dbRefreshToken.isPresent()
                && TokenHasher.verifyToken(
                        cookieRefreshToken, dbRefreshToken.get().getTokenHash())) {
            // valid and exists in database
            refreshTokenService.deleteRefreshToken(dbRefreshToken.get().getTokenId());
            return;
        }
        throw new UnauthorizedException("invalid refresh token");
    }

    @Transactional
    public void logoutAll(String bearerToken) {
        String cookieRefreshToken = bearerToken.replace("Bearer ", "").trim();
        Long tokenId = jwtService.getTokenIdFromRefreshToken(cookieRefreshToken);
        Optional<RefreshToken> dbRefreshToken = refreshTokenService.findRefreshToken(tokenId);
        if (jwtService.validateJwtToken(cookieRefreshToken)
                && dbRefreshToken.isPresent()
                && TokenHasher.verifyToken(
                        cookieRefreshToken, dbRefreshToken.get().getTokenHash())) {
            // valid and exists in database
            refreshTokenService.deleteAllUserRefreshToken(
                    dbRefreshToken.get().getUser().getUserId());
            return;
        }
        throw new UnauthorizedException("invalid refresh token");
    }

    @Transactional
    public Boolean updatePassword(User user, ChangePassword changePasswordDto) {
        boolean isPasswordValid = passwordEncoder.matches(changePasswordDto.getCurrentPassword(), user.getPassword());
        if (isPasswordValid) {
            user.setPassword(passwordEncoder.encode(changePasswordDto.getNewPassword()));
            userService.saveUser(user);
            return true;
        }
        return false;
    }

    @Transactional
    public Long createUser(Register newUser) {
        User user = User.builder()
                .firstName(newUser.getFirstName())
                .lastName(newUser.getLastName())
                .email(newUser.getEmail())
                .password(passwordEncoder.encode(newUser.getPassword()))
                .roles(newUser.getRoles())
                .build();

        user = userService.saveUser(user);
        return user.getUserId();
    }

    public User getUserById(Long userId) {
        return userService.findById(userId);
    }

    public Optional<Jwt> getClaims(String token) {
        if (token == null) {
            throw new BadRequestException("token is missing");
        }
        return jwtService.decodeJwtToken(token);
    }

    public Map<String, Object> getJwk() {
        return jwtService.getJwkSet();
    }
}
