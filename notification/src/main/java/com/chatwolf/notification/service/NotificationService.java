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

    public void process(NotificationEvent ev) {
        meterRegistry
                .counter(
                        "notifications.received",
                        "type",
                        ev.getType().toString().toUpperCase())
                .increment();
        switch (ev.getType()) {
            case REGISTRATION_OTP_EMAIL -> emailService.sendRegistrationOtp(
                    ev.getRecipient(), ev.getUsername(), ev.getOtp());
            case PASSWORD_RESET_OTP_EMAIL -> emailService.sendPasswordResetOtp(
                    ev.getRecipient(), ev.getUsername(), ev.getOtp());
            case LOGIN_OTP_SMS -> smsService.sendSmsLoginOtp(ev);
            default -> {
                log.warn("Unknown notification type: {}", ev.getType());
                meterRegistry.counter("notifications.unknown").increment();
            }
        }
    }
}
