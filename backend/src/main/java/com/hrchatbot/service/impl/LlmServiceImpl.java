package com.hrchatbot.service.impl;

import com.hrchatbot.dto.ChatResponse;
import com.hrchatbot.dto.ConversationMemory;
import com.hrchatbot.entity.ChatMessage;
import com.hrchatbot.entity.User;
import com.hrchatbot.service.LlmService;
import com.hrchatbot.service.provider.LLMProvider;
import com.hrchatbot.service.provider.LLMProviderFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LlmServiceImpl implements LlmService {

    private final LLMProviderFactory providerFactory;

    @Override
    public ChatResponse generateResponse(String userMessage, List<ChatMessage> conversationHistory, 
                                       String context, String provider, User user) {
        
        String selectedProvider = provider != null ? provider : 
                                (user.getPreferredLlmProvider() != null ? user.getPreferredLlmProvider() : 
                                 providerFactory.getDefaultProvider().getProviderName());
        
        try {
            LLMProvider llmProvider = providerFactory.getProvider(selectedProvider);
            
            if (llmProvider == null) {
                log.error("Provider {} not found", selectedProvider);
                return createErrorResponse("LLM provider '" + selectedProvider + "' not found");
            }
            
            if (!llmProvider.isAvailable()) {
                log.error("Provider {} is not available", selectedProvider);
                return createErrorResponse("LLM provider '" + selectedProvider + "' is not available");
            }
            
            ChatResponse response = llmProvider.generateResponse(userMessage, conversationHistory, context);
            response.setLlmProviderUsed(selectedProvider);
            response.setTimestamp(LocalDateTime.now());
            response.setSuccess(true);
            
            return response;
            
        } catch (Exception e) {
            log.error("Error generating response with provider {}: {}", selectedProvider, e.getMessage());
            return createErrorResponse("Failed to generate response with provider '" + selectedProvider + "': " + e.getMessage());
        }
    }

    @Override
    public ChatResponse generateResponseWithMemory(String userMessage, ConversationMemory conversationMemory, 
                                                  String provider, User user) {
        // Convert conversation memory to the format expected by providers
        List<ChatMessage> conversationHistory = null;
        String context = null;
        
        if (conversationMemory != null) {
            conversationHistory = conversationMemory.getShortTermMemory();
            context = conversationMemory.getCombinedContext();
            
            log.debug("Generating response with memory: {} tokens, {} short-term messages", 
                     conversationMemory.getTotalTokenCount(), 
                     conversationHistory != null ? conversationHistory.size() : 0);
        } else {
            log.debug("Generating response without conversation context");
        }
        
        return generateResponse(userMessage, conversationHistory, context, provider, user);
    }

    @Override
    public List<String> getAvailableProviders() {
        return providerFactory.getEnabledProviderNames();
    }

    @Override
    public String getDefaultProvider() {
        return providerFactory.getDefaultProvider().getProviderName();
    }

    @Override
    public boolean isProviderAvailable(String provider) {
        return providerFactory.isProviderAvailable(provider);
    }
    
    /**
     * Creates an error response
     * 
     * @param errorMessage The error message
     * @return ChatResponse with error information
     */
    private ChatResponse createErrorResponse(String errorMessage) {
        return ChatResponse.builder()
                .message("I apologize, but I'm experiencing technical difficulties. Please try again later.")
                .timestamp(LocalDateTime.now())
                .success(false)
                .error(errorMessage)
                .build();
    }
}
