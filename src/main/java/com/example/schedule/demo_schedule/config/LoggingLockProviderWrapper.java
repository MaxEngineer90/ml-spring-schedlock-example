package com.example.schedule.demo_schedule.config;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.SimpleLock;
import net.javacrumbs.shedlock.support.annotation.NonNull;

@Slf4j
public class LoggingLockProviderWrapper implements LockProvider {

    private static final ZoneId BERLIN_ZONE = ZoneId.of("Europe/Berlin");
    private static final DateTimeFormatter GERMAN_TIME_FORMAT = 
        DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

    private final LockProvider delegate;
    private final EnvironmentHelper environmentHelper = new EnvironmentHelper();

    public LoggingLockProviderWrapper(LockProvider delegate) {
        this.delegate = delegate;
    }

    @Override
    @NonNull
    public Optional<SimpleLock> lock(@NonNull LockConfiguration lockConfiguration) {
        String lockName = lockConfiguration.getName();
        
        try {
            Optional<SimpleLock> lockOptional = delegate.lock(lockConfiguration);
            
            if (lockOptional.isPresent()) {
                String berlinTime = ZonedDateTime.now(BERLIN_ZONE).format(GERMAN_TIME_FORMAT);
                log.info("ShedLock ACQUIRED: {} um {} [{}]", lockName, berlinTime, environmentHelper.getContainerInfo());
                return Optional.of(new LoggingSimpleLock(lockOptional.get(), lockName));
            } else {
                return Optional.empty();
            }
            
        } catch (Exception e) {
            throw e;
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
                String berlinTime = ZonedDateTime.now(BERLIN_ZONE).format(GERMAN_TIME_FORMAT);
                log.info("ShedLock RELEASED: {} um {} [{}]", 
                        lockName, berlinTime, environmentHelper.getContainerInfo());
            } catch (Exception e) {
                throw e;
            }
        }
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