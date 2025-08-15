package com.example.schedule.demo_schedule.controller;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.schedule.demo_schedule.service.EmailLogService;
import com.example.schedule.demo_schedule.service.EmailService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
@Slf4j
public class TestController {

    private final EmailService emailService;
    private final EmailLogService emailLogService;

    /**
     * Manueller Email-Test
     */
    @GetMapping("/send-email")
    public Map<String, Object> sendTestEmail() {
        try {
            emailService.sendTestEmail();
            return Map.of(
                "status", "success",
                "message", "Email sent successfully",
                "timestamp", LocalDateTime.now(),
                "pod", getPodName()
            );
        } catch (Exception e) {
            log.error("Manual email test failed", e);
            return Map.of(
                "status", "error",
                "message", e.getMessage(),
                "timestamp", LocalDateTime.now(),
                "pod", getPodName()
            );
        }
    }

    /**
     * Health Check + Email Statistics
     */
    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
            "status", "healthy",
            "pod", getPodName(),
            "timestamp", LocalDateTime.now(),
            "emailsSentLastHour", emailLogService.countEmailsSentLastHour(),
            "scheduling", "enabled"
        );
    }

    /**
     * Info über aktuellen Pod/Container
     */
    @GetMapping("/info")
    public Map<String, Object> info() {
        return Map.of(
            "pod", getPodName(),
            "timestamp", LocalDateTime.now(),
            "javaVersion", System.getProperty("java.version"),
            "environment", System.getenv("SPRING_PROFILES_ACTIVE") != null 
                ? System.getenv("SPRING_PROFILES_ACTIVE") 
                : "default"
        );
    }

    private String getPodName() {
        try {
            String hostname = System.getenv("HOSTNAME");
            return hostname != null ? hostname : InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "unknown";
        }
    }
}