package com.chatwolf.auth.repository;

import com.chatwolf.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    @Transactional
    @Modifying
    void deleteByUser_UserId(Long userId);
}
