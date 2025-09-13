package com.hrchatbot.config;

import com.hrchatbot.service.DatabaseMigrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class StartupMigrationListener {
    
    private final DatabaseMigrationService databaseMigrationService;
    
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("Application started, running database migrations...");
        try {
            databaseMigrationService.updatePdfDocumentStatuses();
            log.info("Database migrations completed successfully");
        } catch (Exception e) {
            log.error("Error running database migrations: {}", e.getMessage());
        }
    }
}
