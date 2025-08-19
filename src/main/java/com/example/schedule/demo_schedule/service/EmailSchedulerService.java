package com.example.schedule.demo_schedule.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;

/**
 * Service for scheduled email tasks with ShedLock coordination.
 * 
 * Ensures that scheduled jobs run only once across multiple application instances
 * using ShedLock distributed coordination.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
    value = "app.scheduling.enabled", 
    havingValue = "true", 
    matchIfMissing = true
)
public class EmailSchedulerService {

    private final EmailService emailService;

    /**
     * Scheduled job for sending test emails.
     * 
     * Protected by ShedLock to prevent multiple executions across containers.
     * Runs every minute with distributed coordination.
     */
    @Scheduled(cron = "0 * * * * *")
    @SchedulerLock(
        name = "send-test-email",
        lockAtMostFor = "9m",
        lockAtLeastFor = "1m"
    )
    public void sendScheduledTestEmailWithShedLock() {
        log.info("Scheduled email job starting");
        
        try {
            emailService.sendTestEmail();
            log.info("Email sent successfully");
            
        } catch (Exception e) {
            log.error("Email sending failed: {}", e.getMessage(), e);
        }
    }

    /**
     * Heartbeat check job for monitoring application health.
     * 
     * Runs every 30 seconds with ShedLock coordination to ensure
     * only one instance logs the heartbeat.
     */
    @Scheduled(fixedRate = 30_000)
    @SchedulerLock(
        name = "heartbeat-check",
        lockAtMostFor = "25s",
        lockAtLeastFor = "10s"
    )
    public void heartbeatCheckWithShedLock() {
        log.info("Application heartbeat - system operational");
    }
}