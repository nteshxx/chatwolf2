package com.chatwolf.auth.service;

import com.chatwolf.auth.entity.RefreshToken;
import com.chatwolf.auth.repository.RefreshTokenRepository;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RefreshTokenService {

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    public RefreshToken saveRefreshToken(RefreshToken refreshToken) {
        return refreshTokenRepository.save(refreshToken);
    }

    public Optional<RefreshToken> findRefreshToken(Long tokenId) {
        return refreshTokenRepository.findById(tokenId);
    }

    public void deleteRefreshToken(Long tokenId) {
        refreshTokenRepository.deleteById(tokenId);
    }

    public void deleteAllUserRefreshToken(Long userId) {
        refreshTokenRepository.deleteByUser_UserId(userId);
    }
}
