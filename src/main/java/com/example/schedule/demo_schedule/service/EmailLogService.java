package com.example.schedule.demo_schedule.service;


import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailLogService {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Loggt gesendete Emails in die Datenbank
     */
    public void logEmail(String recipient, String subject, String content, String podName) {
        try {
            String jobExecutionId = UUID.randomUUID().toString();
            
            String sql = """
                INSERT INTO email_log (recipient, subject, content, sent_at, sent_by_pod, job_execution_id)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

            jdbcTemplate.update(sql, 
                recipient, 
                subject, 
                content, 
                LocalDateTime.now(), 
                podName, 
                jobExecutionId
            );

            log.debug("📝 Email logged to database: recipient={}, pod={}, jobId={}", 
                recipient, podName, jobExecutionId);

        } catch (Exception e) {
            log.error("❌ Failed to log email to database: {}", e.getMessage(), e);
            // Wir werfen hier keine Exception, damit der Email-Versand nicht fehlschlägt
        }
    }

    /**
     * Zählt gesendete Emails der letzten Stunde (für Monitoring)
     */
    public int countEmailsSentLastHour() {
        String sql = """
            SELECT COUNT(*) FROM email_log 
            WHERE sent_at >= DATE_SUB(NOW(), INTERVAL 1 HOUR)
            """;
        
        return jdbcTemplate.queryForObject(sql, Integer.class);
    }
}