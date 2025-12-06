package com.chatwolf.notification.service;

import com.chatwolf.notification.exception.TemplateLoadException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;

@Service
public class EmailTemplateService {

    private static final String TEMPLATE_BASE_PATH = "templates/email/";

    public String loadTemplate(String templateName) {
        try {
            ClassPathResource resource = new ClassPathResource(TEMPLATE_BASE_PATH + templateName);
            InputStreamReader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8);
            return FileCopyUtils.copyToString(reader);
        } catch (IOException e) {
            throw new TemplateLoadException("Failed to load template: " + templateName, e);
        }
    }

    public String processTemplate(String template, Map<String, String> variables) {
        String processedTemplate = template;

        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            processedTemplate = processedTemplate.replace(placeholder, entry.getValue());
        }

        return processedTemplate;
    }

    public String generateRegistrationOtpEmail(String otp, String userName, int expiryMinutes) {
        String template = loadTemplate("registration-otp.html");

        Map<String, String> variables = Map.of(
                "OTP_CODE",
                otp,
                "USER_NAME",
                userName,
                "EXPIRY_TIME",
                String.valueOf(expiryMinutes),
                "CURRENT_YEAR",
                String.valueOf(LocalDateTime.now().getYear()));

        return processTemplate(template, variables);
    }

    public String generateLoginOtpEmail(String otp, String location, String device, int expiryMinutes) {
        String template = loadTemplate("login-otp.html");

        String currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' hh:mm a"));

        Map<String, String> variables = Map.of(
                "OTP_CODE", otp,
                "LOCATION", location,
                "DEVICE", device,
                "LOGIN_TIME", currentTime,
                "EXPIRY_TIME", String.valueOf(expiryMinutes),
                "CURRENT_YEAR", String.valueOf(LocalDateTime.now().getYear()));

        return processTemplate(template, variables);
    }

    public String generatePasswordResetEmail(String otp, String userName, int expiryMinutes) {
        String template = loadTemplate("password-reset-otp.html");

        Map<String, String> variables = Map.of(
                "OTP_CODE",
                otp,
                "USER_NAME",
                userName,
                "EXPIRY_TIME",
                String.valueOf(expiryMinutes),
                "CURRENT_YEAR",
                String.valueOf(LocalDateTime.now().getYear()));

        return processTemplate(template, variables);
    }
}
