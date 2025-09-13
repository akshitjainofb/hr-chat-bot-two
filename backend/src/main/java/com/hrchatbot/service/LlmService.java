package com.hrchatbot.service;

import com.hrchatbot.dto.ChatResponse;
import com.hrchatbot.dto.ConversationMemory;
import com.hrchatbot.entity.ChatMessage;
import com.hrchatbot.entity.User;

import java.util.List;

public interface LlmService {
    
    ChatResponse generateResponse(String userMessage, List<ChatMessage> conversationHistory, 
                                String context, String provider, User user);
    
    /**
     * Generates a response using the new hybrid memory approach
     * 
     * @param userMessage The user's message
     * @param conversationMemory The conversation memory containing short-term, long-term, and document context
     * @param provider The LLM provider to use
     * @param user The user making the request
     * @return ChatResponse with the generated response
     */
    ChatResponse generateResponseWithMemory(String userMessage, ConversationMemory conversationMemory, 
                                          String provider, User user);
    
    List<String> getAvailableProviders();
    
    String getDefaultProvider();
    
    boolean isProviderAvailable(String provider);
}
