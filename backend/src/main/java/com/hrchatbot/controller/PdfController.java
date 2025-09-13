package com.hrchatbot.controller;

import com.hrchatbot.dto.ApiResponse;
import com.hrchatbot.dto.PdfDocumentDto;
import com.hrchatbot.dto.PdfUploadResponse;
import com.hrchatbot.entity.PdfDocument;
import com.hrchatbot.entity.PdfDocumentStatus;
import com.hrchatbot.entity.User;
import com.hrchatbot.repository.PdfDocumentRepository;
import com.hrchatbot.service.PineconeService;
import com.hrchatbot.service.PdfProcessingService;
import com.hrchatbot.service.impl.UserServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/pdf")
@RequiredArgsConstructor
@Slf4j
public class PdfController {

    private final PdfDocumentRepository pdfDocumentRepository;
    private final PdfProcessingService pdfProcessingService;
    private final UserServiceImpl userService;
    private final PineconeService pineconeService;

    @Value("${file.upload-dir}")
    private String uploadDir;

    @PostMapping("/upload")
    public ResponseEntity<PdfUploadResponse> uploadPdf(@RequestParam("file") MultipartFile file, 
                                                       @RequestParam("userEmail") String userEmail) {
        try {
            User user = userService.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(PdfUploadResponse.builder()
                    .success(false)
                    .error("File is empty")
                    .build());
            }
            
            if (!file.getContentType().equals("application/pdf")) {
                return ResponseEntity.badRequest().body(PdfUploadResponse.builder()
                    .success(false)
                    .error("File must be a PDF")
                    .build());
            }
            
            // Create upload directory if it doesn't exist
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            
            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String uniqueFilename = UUID.randomUUID().toString() + fileExtension;
            Path filePath = uploadPath.resolve(uniqueFilename);
            
            // Save file
            Files.copy(file.getInputStream(), filePath);
            
            // Create PDF document entity
            PdfDocument pdfDocument = PdfDocument.builder()
                    .user(user)
                    .fileName(originalFilename)
                    .filePath(filePath.toString())
                    .fileSize(file.getSize())
                    .indexed(false)
                    .status(PdfDocumentStatus.PROCESSING)
                    .build();
            
            pdfDocument = pdfDocumentRepository.save(pdfDocument);
            
            // Process and index PDF synchronously
            try {
                pdfProcessingService.processAndIndexPdf(pdfDocument, file);
                log.info("PDF processing completed for: {}", pdfDocument.getFileName());
            } catch (Exception e) {
                log.error("Error processing PDF: {}", e.getMessage());
                // Mark as failed if processing fails
                pdfDocument.setIndexed(false);
                pdfDocument.setStatus(PdfDocumentStatus.FAILED);
                pdfDocumentRepository.save(pdfDocument);
            }
            
            return ResponseEntity.ok(PdfUploadResponse.builder()
                .success(true)
                .document(convertToDto(pdfDocument))
                .message("PDF uploaded successfully")
                .build());
            
        } catch (IOException e) {
            log.error("Error uploading PDF: {}", e.getMessage());
            return ResponseEntity.badRequest().body(PdfUploadResponse.builder()
                .success(false)
                .error("Failed to upload file")
                .build());
        } catch (Exception e) {
            log.error("Error uploading PDF: {}", e.getMessage());
            return ResponseEntity.badRequest().body(PdfUploadResponse.builder()
                .success(false)
                .error("Upload failed")
                .build());
        }
    }

    @GetMapping("/documents")
    public ResponseEntity<List<PdfDocumentDto>> getUserDocuments(@RequestParam String userEmail) {
        try {
            User user = userService.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
            List<PdfDocument> documents = pdfDocumentRepository.findByUserOrderByCreatedAtDesc(user);
            List<PdfDocumentDto> documentDtos = documents.stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(documentDtos);
        } catch (Exception e) {
            log.error("Error getting documents: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/documents/{documentId}")
    public ResponseEntity<ApiResponse<String>> deleteDocument(@PathVariable Long documentId, 
                                                              @RequestParam String userEmail) {
        try {
            User user = userService.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
            PdfDocument document = pdfDocumentRepository.findById(documentId)
                    .orElseThrow(() -> new RuntimeException("Document not found"));
            
            if (!document.getUser().getId().equals(user.getId())) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Unauthorized access"));
            }
            
            // Delete from Pinecone vector database first
            try {
                pineconeService.deletePdfDocument(document, user);
                log.info("Successfully deleted PDF document {} from Pinecone for user {}", 
                        document.getId(), user.getEmail());
            } catch (Exception e) {
                log.warn("Failed to delete PDF document from Pinecone: {}", e.getMessage());
                // Continue with deletion even if Pinecone cleanup fails
            }
            
            // Delete file from filesystem
            try {
                Files.deleteIfExists(Paths.get(document.getFilePath()));
            } catch (IOException e) {
                log.warn("Could not delete file: {}", e.getMessage());
            }
            
            // Delete from database
            pdfDocumentRepository.delete(document);
            
            return ResponseEntity.ok(ApiResponse.success("Document deleted successfully", "Document deleted successfully"));
            
        } catch (Exception e) {
            log.error("Error deleting document: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to delete document"));
        }
    }
    
    private PdfDocumentDto convertToDto(PdfDocument document) {
        return PdfDocumentDto.builder()
                .id(document.getId())
                .fileName(document.getFileName())
                .fileSize(document.getFileSize())
                .indexed(document.getIndexed())
                .status(document.getStatus())
                .summary(document.getSummary())
                .createdAt(document.getCreatedAt())
                .build();
    }
}
