package com.example.schedule.demo_schedule.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;

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
     * Scheduled Job MIT ShedLock: Nur ein Container sendet eine Email!
     * 
     * ✅ LÖSUNG: ShedLock verhindert mehrfache Ausführung in Docker/K8S
     * - lockAtMostFor: Max. Lock-Dauer (falls Container abstürzt)
     * - lockAtLeastFor: Min. Lock-Dauer (verhindert zu schnelle Wiederholung)
     */
    @Scheduled(fixedRate = 120_000) // Alle 2 Minuten
    @SchedulerLock(
        name = "send-test-email",
        lockAtMostFor = "9m",       // Max 9 Minuten locked (falls Container crashed)
        lockAtLeastFor = "1m"       // Min 1 Minute locked
    )
    public void sendScheduledTestEmailWithShedLock() {
        String containerInfo = getContainerInfo();
        log.info("🔒 {} - Scheduled job MIT ShedLock startet! (Koordiniert zwischen Containern)", containerInfo);
        
        try {
            emailService.sendTestEmail();
            log.info("✅ {} - Email gesendet (MIT ShedLock Schutz - nur dieser Container!)", containerInfo);
            
        } catch (Exception e) {
            log.error("❌ {} - Email failed: {}", containerInfo, e.getMessage(), e);
        }
    }

    /**
     * Demo Job: Läuft alle 30 Sekunden MIT ShedLock
     */
    @Scheduled(fixedRate = 30_000) // Alle 30 Sekunden
    @SchedulerLock(
        name = "heartbeat-check",
        lockAtMostFor = "25s",
        lockAtLeastFor = "10s"
    )
    public void heartbeatCheckWithShedLock() {
        String containerInfo = getContainerInfo();
        log.info("💓 {} - Heartbeat (MIT ShedLock - koordiniert!)", containerInfo);
    }

    /**
     * Generiert Container-Identifikation für Logs
     */
    private String getContainerInfo() {
        String containerName = getContainerName();
        String containerId = getContainerId();
        return String.format("%s %s", containerName.toUpperCase(), containerId);
    }

    /**
     * Container-Name aus Environment Variable
     */
    private String getContainerName() {
        String containerName = System.getenv("CONTAINER_NAME");
        if (containerName != null && !containerName.isEmpty()) {
            return containerName;
        }
        
        // Fallback: aus Hostname ableiten
        String hostname = getPodName();
        if (hostname.contains("container")) {
            return hostname;
        }
        
        return "unknown-container";
    }

    /**
     * Container-ID aus Environment Variable
     */
    private String getContainerId() {
        String containerId = System.getenv("CONTAINER_ID");
        return containerId != null ? "#" + containerId : "#?";
    }

    /**
     * Pod-Name ermitteln
     */
    private String getPodName() {
        try {
            String hostname = System.getenv("HOSTNAME");
            if (hostname != null && !hostname.isEmpty()) {
                return hostname;
            }
            return java.net.InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "unknown-pod";
        }
    }
}