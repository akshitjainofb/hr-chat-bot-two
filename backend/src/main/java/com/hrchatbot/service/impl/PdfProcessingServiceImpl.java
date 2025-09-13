package com.hrchatbot.service.impl;

import com.hrchatbot.entity.PdfDocument;
import com.hrchatbot.entity.PdfDocumentStatus;
import com.hrchatbot.repository.PdfDocumentRepository;
import com.hrchatbot.service.PdfProcessingService;
import com.hrchatbot.service.PineconeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class PdfProcessingServiceImpl implements PdfProcessingService {

    private final PineconeService pineconeService;
    private final PdfDocumentRepository pdfDocumentRepository;

    @Override
    public String extractTextFromPdf(MultipartFile file) throws IOException {
        try (InputStream inputStream = file.getInputStream();
             PDDocument document = PDDocument.load(inputStream)) {
            
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
            
        } catch (IOException e) {
            log.error("Error extracting text from PDF: {}", e.getMessage());
            throw e;
        }
    }

    @Override
    public void processAndIndexPdf(PdfDocument pdfDocument, MultipartFile file) throws IOException {
        log.info("Starting processing of PDF: {} (ID: {})", pdfDocument.getFileName(), pdfDocument.getId());
        try {
            // Extract text from PDF
            log.info("Extracting text from PDF: {}", pdfDocument.getFileName());
            String content = extractTextFromPdf(file);
            log.info("Extracted {} characters from PDF", content.length());
            
            // Generate summary
            log.info("Generating summary for PDF: {}", pdfDocument.getFileName());
            String summary = generateSummary(content);
            pdfDocument.setSummary(summary);
            
            // Index in Pinecone
            log.info("Indexing PDF in Pinecone: {}", pdfDocument.getFileName());
            pineconeService.indexPdfDocument(pdfDocument, content);
            
            // Mark as indexed and save to database
            log.info("Marking PDF as indexed and saving to database: {}", pdfDocument.getFileName());
            pdfDocument.setIndexed(true);
            pdfDocument.setStatus(PdfDocumentStatus.INDEXED);
            pdfDocumentRepository.save(pdfDocument);
            
            log.info("Successfully processed and indexed PDF: {}", pdfDocument.getFileName());
            
        } catch (Exception e) {
            log.error("Error processing PDF: {}", e.getMessage());
            // Mark as failed if processing fails
            pdfDocument.setIndexed(false);
            pdfDocument.setStatus(PdfDocumentStatus.FAILED);
            pdfDocumentRepository.save(pdfDocument);
            throw e;
        }
    }

    @Override
    public String generateSummary(String content) {
        // Simple summary generation - in production, use an LLM for better summaries
        if (content.length() <= 200) {
            return content;
        }
        
        String[] sentences = content.split("[.!?]+");
        StringBuilder summary = new StringBuilder();
        
        for (int i = 0; i < Math.min(3, sentences.length); i++) {
            summary.append(sentences[i].trim()).append(". ");
        }
        
        return summary.toString().trim();
    }
}
