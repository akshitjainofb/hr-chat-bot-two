package com.hrchatbot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {
    
    @NotNull(message = "Chat room ID is required")
    private Long chatRoomId;
    
    @NotBlank(message = "Message cannot be empty")
    private String message;
    
    private String llmProvider;
    
    @NotBlank(message = "User email is required")
    private String userEmail;
    
    private Boolean includeContext;
}
