package com.example.schedule.demo_schedule.config;

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;

/**
 * Configuration for ShedLock distributed scheduling coordination.
 * 
 * Enables coordination between multiple application instances to ensure
 * that scheduled jobs run only once across all instances.
 */
@Configuration
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "PT10M")
public class ShedLockConfig {

    /**
     * Creates LockProvider for MySQL-based coordination with comprehensive logging.
     * 
     * Uses the 'shedlock' table created by Liquibase for lock coordination.
     * Database time is used for consistent timing across all instances.
     * 
     * @param dataSource the configured MySQL DataSource
     * @return LockProvider wrapped with logging capabilities
     */
    @Bean
    public LockProvider lockProvider(DataSource dataSource) {
        JdbcTemplateLockProvider provider = new JdbcTemplateLockProvider(
            JdbcTemplateLockProvider.Configuration.builder()
                .withJdbcTemplate(new JdbcTemplate(dataSource))
                .withTableName("shedlock")
                .usingDbTime()
                .build()
        );

        return new LoggingLockProviderWrapper(provider);
    }
}