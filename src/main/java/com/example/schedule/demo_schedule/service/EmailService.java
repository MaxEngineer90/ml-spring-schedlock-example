package com.example.schedule.demo_schedule.service;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for sending emails with logging capabilities.
 * 
 * Handles email sending via configured mail server and logs
 * all sent emails to the database for tracking purposes.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final JavaMailSender mailSender;
    private final EmailLogService emailLogService;

    @Value("${app.email.from}")
    private String fromEmail;

    @Value("${app.email.test-recipient}")
    private String testRecipient;

    /**
     * Sends a test email to the configured test recipient.
     */
    public void sendTestEmail() {
        sendTestEmailTo(testRecipient);
    }

    /**
     * Sends a test email to the specified recipient.
     * 
     * @param recipient the email address to send the test email to
     * @throws RuntimeException if email sending fails
     */
    public void sendTestEmailTo(String recipient) {
        try {
            SimpleMailMessage message = createTestMessage(recipient);
            
            mailSender.send(message);
            emailLogService.logEmail(recipient, message.getSubject(), message.getText(), getHostname());
            
            log.info("Email sent successfully to: {}", recipient);
            
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", recipient, e.getMessage(), e);
            throw new RuntimeException("Email sending failed", e);
        }
    }

    private SimpleMailMessage createTestMessage(String recipient) {
        String hostname = getHostname();
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
        
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(recipient);
        message.setSubject(String.format("Test Email from %s", hostname));
        message.setText(createEmailContent(hostname, timestamp, recipient));
        
        return message;
    }

    private String createEmailContent(String hostname, String timestamp, String recipient) {
        return String.format(
            "Hello from: %s%n" +
            "Sent at: %s%n" +
            "Recipient: %s%n" +
            "This email was sent via scheduled job protected by ShedLock.",
            hostname, timestamp, recipient
        );
    }

    private String getHostname() {
        try {
            String hostname = System.getenv("HOSTNAME");
            if (hostname != null && !hostname.trim().isEmpty()) {
                return hostname;
            }
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            log.warn("Could not determine hostname: {}", e.getMessage());
            return "unknown-host";
        }
    }
}