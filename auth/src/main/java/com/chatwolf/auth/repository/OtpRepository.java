package com.chatwolf.auth.repository;

import com.chatwolf.auth.constant.OtpStatus;
import com.chatwolf.auth.constant.OtpType;
import com.chatwolf.auth.entity.Otp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OtpRepository extends JpaRepository<Otp, Long> {

    Optional<Otp> findTopByEmailAndOtpTypeAndStatusOrderByCreatedAtDesc(
            String email, OtpType otpType, OtpStatus status);

    Optional<Otp> findByEmailAndOtpCodeAndOtpTypeAndStatus(
            String email, String otpCode, OtpType otpType, OtpStatus status);

    List<Otp> findByEmailAndOtpTypeAndCreatedAtAfter(String email, OtpType otpType, LocalDateTime since);

    long countByEmailAndCreatedAtAfter(String email, LocalDateTime since);

    @Modifying
    @Query(
            "UPDATE t_otp o SET o.status = :status WHERE o.email = :email AND o.otpType = :otpType AND o.status = 'ACTIVE'")
    void revokeActiveOtps(String email, OtpType otpType, OtpStatus status);

    @Modifying
    @Query("UPDATE t_otp o SET o.status = 'EXPIRED' WHERE o.status = 'ACTIVE' AND o.expiresAt < :now")
    int expireOldOtps(LocalDateTime now);

    @Modifying
    @Query("DELETE FROM t_otp o WHERE o.createdAt < :cutoffDate")
    int deleteOldOtpRecords(@Param("cutoffDate") LocalDateTime cutoffDate);
}
