package com.hrchatbot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PdfUploadResponse {
    private boolean success;
    private String message;
    private PdfDocumentDto document;
    private String error;
}
