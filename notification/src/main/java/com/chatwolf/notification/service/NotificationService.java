package com.chatwolf.notification.service;

import com.chatwolf.notification.dto.NotificationEvent;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final MeterRegistry meterRegistry;
    private final EmailService emailService;
    private final SmsService smsService;

    public void process(NotificationEvent event) {
        meterRegistry
                .counter(
                        "notifications.received",
                        "type",
                        event.getType().toString().toUpperCase())
                .increment();
        switch (event.getType()) {
            case REGISTRATION_OTP_EMAIL -> emailService.sendRegistrationOtp(
                    event.getRecipient(), event.getUsername(), event.getOtp());
            case PASSWORD_RESET_OTP_EMAIL -> emailService.sendPasswordResetOtp(
                    event.getRecipient(), event.getUsername(), event.getOtp());
            case LOGIN_OTP_SMS -> smsService.sendSmsLoginOtp(event);
            default -> {
                log.warn("Unknown notification type: {}", event.getType());
                meterRegistry.counter("notifications.unknown").increment();
            }
        }
    }
}
