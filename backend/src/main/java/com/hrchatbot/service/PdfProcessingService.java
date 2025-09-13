package com.hrchatbot.service;

import com.hrchatbot.entity.PdfDocument;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface PdfProcessingService {
    
    String extractTextFromPdf(MultipartFile file) throws IOException;
    
    void processAndIndexPdf(PdfDocument pdfDocument, MultipartFile file) throws IOException;
    
    String generateSummary(String content);
}
