package com.hrchatbot.service;

import com.hrchatbot.entity.PdfDocument;
import com.hrchatbot.entity.PdfDocumentStatus;
import com.hrchatbot.repository.PdfDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DatabaseMigrationService {
    
    private final PdfDocumentRepository pdfDocumentRepository;
    
    public void updatePdfDocumentStatuses() {
        try {
            List<PdfDocument> documents = pdfDocumentRepository.findAll();
            int updatedCount = 0;
            
            for (PdfDocument document : documents) {
                if (document.getStatus() == null) {
                    // Set status based on indexed field
                    if (document.getIndexed() != null && document.getIndexed()) {
                        document.setStatus(PdfDocumentStatus.INDEXED);
                    } else {
                        document.setStatus(PdfDocumentStatus.PROCESSING);
                    }
                    pdfDocumentRepository.save(document);
                    updatedCount++;
                }
            }
            
            log.info("Updated {} PDF documents with status field", updatedCount);
            
        } catch (Exception e) {
            log.error("Error updating PDF document statuses: {}", e.getMessage());
        }
    }
}
