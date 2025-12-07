package com.chatwolf.auth.service;

import com.chatwolf.auth.constant.OtpStatus;
import com.chatwolf.auth.constant.OtpType;
import com.chatwolf.auth.entity.Otp;
import com.chatwolf.auth.exception.InvalidOtpException;
import com.chatwolf.auth.exception.IpMismatchException;
import com.chatwolf.auth.exception.MaxAttemptsExceededException;
import com.chatwolf.auth.exception.OtpExpiredException;
import com.chatwolf.auth.exception.RateLimitExceededException;
import com.chatwolf.auth.exception.TooManyRequestsException;
import com.chatwolf.auth.repository.OtpRepository;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {

    private int length = 6;
    private int maxAttempts = 3;
    private int rateLimitPerHour = 5;
    private int cleanupDays = 7;
    private boolean numericOnly = true;
    private Security security = new Security();

    @Data
    public static class Security {
        private boolean requireIpMatch = true;
        private boolean checkBlacklist = true;
        // Min time between OTP requests
        private int minIntervalSeconds = 60;
    }

    private final OtpRepository otpRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public String generateOtp(String email, OtpType otpType, String ipAddress, String userAgent) {
        log.info("Generating OTP for email: {}, type: {}", maskEmail(email), otpType);

        // 1. Validate rate limiting
        validateRateLimit(email, otpType);

        // 2. Check minimum interval between requests
        validateMinimumInterval(email, otpType);

        // 3. Revoke any active OTPs for this email/type
        revokeActiveOtps(email, otpType);

        // 4. Generate new OTP
        String otpCode = generateSecureOtp();

        // 5. Calculate expiry time
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(otpType.getExpiryMinutes());

        // 6. Create and save OTP record
        Otp otpRecord = Otp.builder()
                .email(email)
                .otpCode(otpCode)
                .otpType(otpType)
                .status(OtpStatus.ACTIVE)
                .expiresAt(expiresAt)
                .attemptCount(0)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();

        otpRepository.save(otpRecord);

        log.info("OTP generated successfully for email: {}, expires at: {}", maskEmail(email), expiresAt);

        return otpCode;
    }

    @Transactional
    public Boolean validateOtp(String email, String otpCode, OtpType otpType, String ipAddress, String userAgent) {
        log.info("Validating OTP for email: {}, type: {}", maskEmail(email), otpType);

        // 1. Find active OTP record
        Otp otpRecord = otpRepository
                .findByEmailAndOtpCodeAndOtpTypeAndStatus(email, otpCode, otpType, OtpStatus.ACTIVE)
                .orElseThrow(() -> new InvalidOtpException("Invalid or expired OTP"));

        // 2. Check if expired
        if (LocalDateTime.now().isAfter(otpRecord.getExpiresAt())) {
            otpRecord.setStatus(OtpStatus.EXPIRED);
            otpRepository.save(otpRecord);
            throw new OtpExpiredException("OTP has expired");
        }

        // 3. Check max attempts
        if (otpRecord.getAttemptCount() >= maxAttempts) {
            otpRecord.setStatus(OtpStatus.MAX_ATTEMPTS);
            otpRepository.save(otpRecord);
            throw new MaxAttemptsExceededException("Maximum verification attempts exceeded");
        }

        // 4. Validate IP address if required
        if (security.isRequireIpMatch()) {
            if (!otpRecord.getIpAddress().equals(ipAddress)) {
                throw new IpMismatchException("IP address mismatch");
            }
        }

        // 5. Increment attempt count
        otpRecord.setAttemptCount(otpRecord.getAttemptCount() + 1);

        // 6. Mark as verified
        otpRecord.setStatus(OtpStatus.VERIFIED);
        otpRecord.setVerifiedAt(LocalDateTime.now());
        otpRepository.save(otpRecord);

        log.info("OTP verified successfully for email: {}", maskEmail(email));
        return true;
    }

    @Transactional
    public String resendOtp(String email, OtpType otpType, String ipAddress, String userAgent) {
        log.info("Resending OTP for email: {}, type: {}", maskEmail(email), otpType);

        // Check if there's an active OTP
        otpRepository
                .findTopByEmailAndOtpTypeAndStatusOrderByCreatedAtDesc(email, otpType, OtpStatus.ACTIVE)
                .ifPresent(otp -> {
                    // If OTP was created less than minimum interval, reject
                    if (otp.getCreatedAt()
                            .plusSeconds(security.getMinIntervalSeconds())
                            .isAfter(LocalDateTime.now())) {
                        throw new TooManyRequestsException("Please wait before requesting a new OTP");
                    }
                });

        return generateOtp(email, otpType, ipAddress, userAgent);
    }

    @Transactional
    public void revokeOtp(String email, OtpType otpType) {
        log.info("Revoking OTP for email: {}, type: {}", maskEmail(email), otpType);
        otpRepository.revokeActiveOtps(email, otpType, OtpStatus.REVOKED);
    }

    private String generateSecureOtp() {
        StringBuilder otp = new StringBuilder();

        if (numericOnly) {
            for (int i = 0; i < length; i++) {
                otp.append(secureRandom.nextInt(10));
            }
        } else {
            String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
            for (int i = 0; i < length; i++) {
                otp.append(characters.charAt(secureRandom.nextInt(characters.length())));
            }
        }

        return otp.toString();
    }

    private void validateRateLimit(String email, OtpType otpType) {
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        long count = otpRepository.countByEmailAndCreatedAtAfter(email, oneHourAgo);

        if (count >= rateLimitPerHour) {
            throw new RateLimitExceededException("Too many OTP requests. Please try again later.");
        }
    }

    private void validateMinimumInterval(String email, OtpType otpType) {
        List<Otp> recentOtps = otpRepository.findByEmailAndOtpTypeAndCreatedAtAfter(
                email, otpType, LocalDateTime.now().minusSeconds(security.getMinIntervalSeconds()));

        if (!recentOtps.isEmpty()) {
            throw new TooManyRequestsException(
                    "Please wait " + security.getMinIntervalSeconds() + " seconds before requesting a new OTP");
        }
    }

    private void revokeActiveOtps(String email, OtpType otpType) {
        otpRepository.revokeActiveOtps(email, otpType, OtpStatus.REVOKED);
    }

    @Transactional
    public int cleanupExpiredOtps() {
        log.info("Running OTP cleanup task");
        int count = otpRepository.expireOldOtps(LocalDateTime.now());
        log.info("Expired {} old OTPs", count);
        return count;
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***";
        }
        String[] parts = email.split("@");
        return parts[0].substring(0, Math.min(2, parts[0].length())) + "***@" + parts[1];
    }

    @Async
    @Scheduled(cron = "0 0 3 * * SUN")
    public void scheduledWeeklyCleanup() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(cleanupDays);
        log.info("Starting cleanup of OTP records older than: {}", cutoffDate);
        int deletedCount = otpRepository.deleteOldOtpRecords(cutoffDate);
        log.info("Successfully deleted {} old OTP records", deletedCount);
    }
}
