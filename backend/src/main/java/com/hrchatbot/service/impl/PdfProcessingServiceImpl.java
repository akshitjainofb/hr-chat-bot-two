package com.hrchatbot.service.impl;

import com.hrchatbot.entity.PdfDocument;
import com.hrchatbot.service.PdfProcessingService;
import com.hrchatbot.service.PineconeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class PdfProcessingServiceImpl implements PdfProcessingService {

    private final PineconeService pineconeService;

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
        try {
            // Extract text from PDF
            String content = extractTextFromPdf(file);
            
            // Generate summary
            String summary = generateSummary(content);
            pdfDocument.setSummary(summary);
            
            // Index in Pinecone
            pineconeService.indexPdfDocument(pdfDocument, content);
            
            // Mark as indexed
            pdfDocument.setIndexed(true);
            
            log.info("Successfully processed and indexed PDF: {}", pdfDocument.getFileName());
            
        } catch (Exception e) {
            log.error("Error processing PDF: {}", e.getMessage());
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
