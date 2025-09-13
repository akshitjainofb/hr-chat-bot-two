package com.hrchatbot.service.provider;

import com.hrchatbot.dto.ChatResponse;
import com.hrchatbot.entity.ChatMessage;

import java.util.List;

/**
 * Interface for LLM provider implementations.
 * Defines the contract that all LLM providers must implement.
 */
public interface LLMProvider {
    
    /**
     * Generates a response using the LLM provider
     * 
     * @param userMessage The user's message
     * @param conversationHistory List of previous conversation messages
     * @param context Additional context for the LLM
     * @return ChatResponse containing the generated response
     */
    ChatResponse generateResponse(String userMessage, List<ChatMessage> conversationHistory, String context);
    
    /**
     * Gets the provider name
     * 
     * @return Provider name (e.g., "openai", "gemini", "huggingface")
     */
    String getProviderName();
    
    /**
     * Checks if the provider is available and properly configured
     * 
     * @return True if the provider is available
     */
    boolean isAvailable();
}
