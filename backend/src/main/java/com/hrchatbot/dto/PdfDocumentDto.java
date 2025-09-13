package com.hrchatbot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PdfDocumentDto {
    
    private Long id;
    private String fileName;
    private Long fileSize;
    private Boolean indexed;
    private String summary;
    private LocalDateTime createdAt;
}
