package com.chatwolf.auth.entity;

import com.chatwolf.auth.constant.OtpStatus;
import com.chatwolf.auth.constant.OtpType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "t_otp",
        indexes = {
            @Index(name = "idx_email_type", columnList = "email,otp_type"),
            @Index(name = "idx_created_at", columnList = "created_at")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Otp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false, length = 6)
    private String otpCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OtpType otpType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OtpStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    private LocalDateTime verifiedAt;

    @Column(nullable = false)
    private Integer attemptCount;

    private String ipAddress;

    private String userAgent;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        attemptCount = 0;
        status = OtpStatus.ACTIVE;
    }
}
