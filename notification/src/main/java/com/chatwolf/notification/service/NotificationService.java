package com.chatwolf.notification.service;

import com.chatwolf.notification.dto.NotificationEvent;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final JavaMailSender mailSender;
    private final MeterRegistry meterRegistry;

    public void process(NotificationEvent ev) {
        meterRegistry.counter("notifications.received", "type", ev.getType()).increment();
        if ("EMAIL".equalsIgnoreCase(ev.getType())) sendEmail(ev);
        else if ("SMS".equalsIgnoreCase(ev.getType())) sendSms(ev);
        else {
            log.warn("unknown notification type {}", ev.getType());
            meterRegistry.counter("notifications.unknown").increment();
        }
    }

    private void sendEmail(NotificationEvent ev) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(ev.getRecipient());
            msg.setSubject(ev.getSubject());
            msg.setText(ev.getBody());
            mailSender.send(msg);
            meterRegistry.counter("notifications.sent", "type", "EMAIL").increment();
        } catch (Exception ex) {
            meterRegistry.counter("notifications.failed", "type", "EMAIL").increment();
            throw ex;
        }
    }

    private void sendSms(NotificationEvent ev) {
        // integrate Twilio client here; for now log
        log.info("sendSms to {}: {}", ev.getRecipient(), ev.getBody());
        meterRegistry.counter("notifications.sent", "type", "SMS").increment();
    }
}
