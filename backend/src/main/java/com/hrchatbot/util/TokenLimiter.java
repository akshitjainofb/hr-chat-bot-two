package com.hrchatbot.util;

import com.hrchatbot.config.MemoryConfig;
import com.hrchatbot.entity.ChatMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for managing conversation history based on token count rather than message count.
 * Implements a sliding window approach to keep conversation context within token limits.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class TokenLimiter {
    
    private static final int BUFFER_TOKENS = 100; // Buffer to account for system prompts
    
    private final Tokenizer tokenizer;
    private final MemoryConfig memoryConfig;
    
    /**
     * Trims conversation history to fit within the token budget.
     * Always includes the most recent messages that fit within the limit.
     * 
     * @param messages List of messages ordered from oldest to newest
     * @return Trimmed list of messages that fit within token budget
     */
    public List<ChatMessage> trimToTokenLimit(List<ChatMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return new ArrayList<>();
        }
        
        int availableTokens = memoryConfig.getTokenLimit() - BUFFER_TOKENS;
        List<ChatMessage> trimmedMessages = new ArrayList<>();
        int currentTokenCount = 0;
        
        // Start from the most recent messages and work backwards
        for (int i = messages.size() - 1; i >= 0; i--) {
            ChatMessage message = messages.get(i);
            int messageTokens = estimateTokenCount(message.getMessage());
            
            // If adding this message would exceed the limit, stop
            if (currentTokenCount + messageTokens > availableTokens) {
                break;
            }
            
            trimmedMessages.add(0, message); // Add to beginning to maintain chronological order
            currentTokenCount += messageTokens;
        }
        
        log.debug("Trimmed conversation from {} to {} messages ({} tokens)", 
                 messages.size(), trimmedMessages.size(), currentTokenCount);
        
        return trimmedMessages;
    }
    
    /**
     * Estimates token count for a given text using the tokenizer.
     * 
     * @param text The text to count tokens for
     * @return Estimated token count
     */
    private int estimateTokenCount(String text) {
        return tokenizer.countTokens(text);
    }
    
    /**
     * Gets the current token limit
     * 
     * @return Token limit
     */
    public int getTokenLimit() {
        return memoryConfig.getTokenLimit();
    }
    
    /**
     * Checks if a message would fit within the current token budget
     * 
     * @param message The message to check
     * @param currentTokenCount Current token count in the conversation
     * @return True if the message would fit
     */
    public boolean wouldFit(ChatMessage message, int currentTokenCount) {
        int messageTokens = estimateTokenCount(message.getMessage());
        return currentTokenCount + messageTokens <= (memoryConfig.getTokenLimit() - BUFFER_TOKENS);
    }
    
    /**
     * Estimates total token count for a list of messages
     * 
     * @param messages List of messages
     * @return Total estimated token count
     */
    public int estimateTotalTokens(List<ChatMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return 0;
        }
        
        return messages.stream()
                .mapToInt(msg -> estimateTokenCount(msg.getMessage()))
                .sum();
    }
}
