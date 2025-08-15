package com.example.schedule.demo_schedule.config;

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;

@Configuration
public class ShedLockConfig {

    /**
     * ShedLock LockProvider für MySQL
     * 
     * ✅ AKTIVIERT! Koordiniert Scheduled Jobs zwischen K8S Pods/Containern
     * Verwendet die "shedlock" Tabelle zur Synchronisation
     */
    @Bean
    public LockProvider lockProvider(DataSource dataSource) {
        return new JdbcTemplateLockProvider(
            JdbcTemplateLockProvider.Configuration.builder()
                .withJdbcTemplate(new JdbcTemplate(dataSource))
                .withTableName("shedlock")  // Unsere Liquibase-Tabelle
                .build()
        );
    }
}