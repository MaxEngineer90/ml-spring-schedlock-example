package com.example.schedule.demo_schedule.service;

import java.net.InetAddress;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final EmailLogService emailLogService;

    @Value("${app.email.from}")
    private String fromEmail;

    @Value("${app.email.test-recipient}")
    private String testRecipient;

    /**
     * Sendet eine Test-Email via MailHog
     */
    public void sendTestEmail() {
        sendTestEmailTo(testRecipient);
    }

    /**
     * Sendet eine Test-Email an eine spezifische Email-Adresse
     */
    public void sendTestEmailTo(String recipient) {
        try {
            String podName = getPodName();
            String subject = String.format("Scheduled Email from Pod: %s", podName);
            String content = String.format(
                    "Hello from Pod: %s%n"
                    + "Sent at: %s%n"
                    + "Recipient: %s%n"
                    + "This email was sent via scheduled job protected by ShedLock!",
                    podName,
                    LocalDateTime.now(),
                    recipient
            );

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(recipient);
            message.setSubject(subject);
            message.setText(content);

            // Email über MailHog senden
            mailSender.send(message);

            // In Datenbank loggen
            emailLogService.logEmail(recipient, subject, content, podName);

            log.info("✅ Email successfully sent from pod: {} to: {}", podName, recipient);

        } catch (Exception e) {
            log.error("❌ Failed to send email: {}", e.getMessage(), e);
            throw new RuntimeException("Email sending failed", e);
        }
    }

    /**
     * Ermittelt den Pod-Namen (für K8S) oder Hostname
     */
    private String getPodName() {
        try {
            // In K8S ist das der Pod-Name
            String podName = System.getenv("HOSTNAME");
            if (podName != null && !podName.isEmpty()) {
                return podName;
            }

            // Fallback: lokaler Hostname
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "unknown-pod";
        }
    }
}
