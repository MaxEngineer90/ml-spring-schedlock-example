package com.example.schedule.demo_schedule.config;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.SimpleLock;
import net.javacrumbs.shedlock.support.annotation.NonNull;

@Slf4j
public class LoggingLockProviderWrapper implements LockProvider {

    private static final Duration CLEANUP_INTERVAL = Duration.ofMinutes(5);
    private static final Duration MAX_LOCK_AGE = Duration.ofHours(1);

    private final LockProvider delegate;
    private final EnvironmentHelper environmentHelper = new EnvironmentHelper();
    private final ConcurrentMap<String, Instant> lockStartTimes = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleanupExecutor = 
        Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "shedlock-cleanup");
            t.setDaemon(true);
            return t;
        });

    public LoggingLockProviderWrapper(LockProvider delegate) {
        this.delegate = delegate;
        startCleanupTask();
    }

    @Override
    @NonNull
    public Optional<SimpleLock> lock(@NonNull LockConfiguration lockConfiguration) {
        String lockName = lockConfiguration.getName();
        
        try {
            Optional<SimpleLock> lockOptional = delegate.lock(lockConfiguration);
            
            if (lockOptional.isPresent()) {
                handleLockAcquisition(lockName);
                return Optional.of(new LoggingSimpleLock(lockOptional.get(), lockName));
            } else {
                handleLockFailure(lockName);
                return Optional.empty();
            }
            
        } catch (Exception e) {
            handleLockError(lockName, e);
            throw e;
        }
    }

    private void handleLockAcquisition(String lockName) {
        lockStartTimes.put(lockName, Instant.now());
        log.info("ShedLock ACQUIRED: {} [{}]", lockName, environmentHelper.getContainerInfo());
    }

    private void handleLockFailure(String lockName) {
        log.debug("ShedLock FAILED: {} [{}] - already locked by another instance", 
                lockName, environmentHelper.getContainerInfo());
    }

    private void handleLockError(String lockName, Exception e) {
        log.error("ShedLock ERROR: {} [{}] - {}", 
                lockName, environmentHelper.getContainerInfo(), e.getMessage());
    }

    private void startCleanupTask() {
        cleanupExecutor.scheduleWithFixedDelay(
            this::cleanupOldLockEntries,
            CLEANUP_INTERVAL.toMinutes(),
            CLEANUP_INTERVAL.toMinutes(),
            TimeUnit.MINUTES
        );
    }

    private void cleanupOldLockEntries() {
        try {
            Instant cutoff = Instant.now().minus(MAX_LOCK_AGE);
            
            var removedEntries = lockStartTimes.entrySet().removeIf(entry -> 
                entry.getValue().isBefore(cutoff)
            );
            
            if (removedEntries) {
                log.debug("Cleaned up old lock entries");
            }
        } catch (Exception e) {
            log.warn("Failed to cleanup old lock entries: {}", e.getMessage());
        }
    }

    private class LoggingSimpleLock implements SimpleLock {
        
        private final SimpleLock delegate;
        private final String lockName;
        
        LoggingSimpleLock(SimpleLock delegate, String lockName) {
            this.delegate = delegate;
            this.lockName = lockName;
        }

        @Override
        public void unlock() {
            try {
                delegate.unlock();
                handleLockRelease();
            } catch (Exception e) {
                handleUnlockError(e);
                throw e;
            }
        }

        private void handleLockRelease() {
            String duration = calculateLockDuration();
            log.debug("ShedLock RELEASED: {} ({}) [{}]", 
                    lockName, duration, environmentHelper.getContainerInfo());
        }

        private void handleUnlockError(Exception e) {
            log.error("ShedLock RELEASE ERROR: {} [{}] - {}", 
                    lockName, environmentHelper.getContainerInfo(), e.getMessage());
        }

        private String calculateLockDuration() {
            Instant startTime = lockStartTimes.remove(lockName);
            if (startTime == null) {
                return "unknown";
            }
            
            Duration duration = Duration.between(startTime, Instant.now());
            return formatDuration(duration);
        }
    }

    private String formatDuration(Duration duration) {
        long seconds = duration.getSeconds();
        if (seconds < 60) {
            return seconds + "s";
        }
        return (seconds / 60) + "m " + (seconds % 60) + "s";
    }

    private static class EnvironmentHelper {
        
        String getContainerInfo() {
            String containerName = getEnvironmentVariable("CONTAINER_NAME", "UNKNOWN");
            String containerId = getEnvironmentVariable("CONTAINER_ID", "?");
            return String.format("%s #%s", containerName, containerId);
        }

        private String getEnvironmentVariable(String name, String defaultValue) {
            String value = System.getenv(name);
            return (value != null && !value.trim().isEmpty()) ? value : defaultValue;
        }
    }
}