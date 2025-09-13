package com.hrchatbot.dto;

import com.hrchatbot.entity.ChatMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDto {
    
    private Long id;
    private String role;
    private String message;
    private String contextUsed;
    private String llmProviderUsed;
    private LocalDateTime createdAt;
    
    public static ChatMessageDto fromEntity(ChatMessage message) {
        return ChatMessageDto.builder()
                .id(message.getId())
                .role(message.getRole().name())
                .message(message.getMessage())
                .contextUsed(message.getContextUsed())
                .llmProviderUsed(message.getLlmProviderUsed())
                .createdAt(message.getCreatedAt())
                .build();
    }
}
