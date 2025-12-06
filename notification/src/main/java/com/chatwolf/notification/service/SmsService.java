package com.chatwolf.notification.service;

import com.chatwolf.notification.dto.NotificationEvent;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmsService {

    private final MeterRegistry meterRegistry;

    public void sendSmsLoginOtp(NotificationEvent ev) {
        // integrate sms client here; for now log
        String body = "Login OTP: " + ev.getOtp() + ". Valid for 10 minutes.";
        log.info("sendSms to {}: {}", ev.getRecipient(), body);
        meterRegistry.counter("notifications.sms.sent", "type", "SMS").increment();
    }
}
