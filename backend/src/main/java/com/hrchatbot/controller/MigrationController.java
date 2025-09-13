package com.hrchatbot.controller;

import com.hrchatbot.dto.ApiResponse;
import com.hrchatbot.service.DatabaseMigrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/migration")
@RequiredArgsConstructor
@Slf4j
public class MigrationController {
    
    private final DatabaseMigrationService databaseMigrationService;
    
    @PostMapping("/update-pdf-statuses")
    public ResponseEntity<ApiResponse<String>> updatePdfStatuses() {
        try {
            databaseMigrationService.updatePdfDocumentStatuses();
            return ResponseEntity.ok(ApiResponse.success("PDF document statuses updated successfully", "Migration completed"));
        } catch (Exception e) {
            log.error("Error during migration: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error("Migration failed: " + e.getMessage()));
        }
    }
}
