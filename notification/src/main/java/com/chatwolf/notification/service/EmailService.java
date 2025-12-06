package com.chatwolf.notification.service;

import com.chatwolf.notification.exception.EmailSendException;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final EmailTemplateService templateService;
    private final MeterRegistry meterRegistry;

    public void sendRegistrationOtp(String toEmail, String userName, String otp) {
        try {
            String htmlContent = templateService.generateRegistrationOtpEmail(otp, userName, 10);
            sendHtmlEmail(toEmail, "Verify Your Email Address", htmlContent);
            log.info("Registration OTP email sent successfully to: {}", toEmail);
            meterRegistry.counter("notifications.email.sent", "type", "EMAIL").increment();
        } catch (Exception e) {
            log.error("Failed to send registration OTP email to: {}", toEmail, e);
            meterRegistry.counter("notifications.email.failed", "type", "EMAIL").increment();
            throw new EmailSendException("Failed to send registration OTP email", e);
        }
    }

    public void sendLoginOtp(String toEmail, String otp, String location, String device) {
        try {
            String htmlContent = templateService.generateLoginOtpEmail(otp, location, device, 5);
            sendHtmlEmail(toEmail, "Login Verification Code", htmlContent);
            log.info("Login OTP email sent successfully to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send login OTP email to: {}", toEmail, e);
            throw new EmailSendException("Failed to send login OTP email", e);
        }
    }

    public void sendPasswordResetOtp(String toEmail, String userName, String otp) {
        try {
            String htmlContent = templateService.generatePasswordResetEmail(otp, userName, 15);
            sendHtmlEmail(toEmail, "Reset Your Password", htmlContent);
            log.info("Password reset OTP email sent successfully to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send password reset OTP email to: {}", toEmail, e);
            throw new EmailSendException("Failed to send password reset OTP email", e);
        }
    }

    private void sendHtmlEmail(String to, String subject, String htmlContent) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom("onboarding@resend.dev");
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);

        mailSender.send(message);
    }
}
