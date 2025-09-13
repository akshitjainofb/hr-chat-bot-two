package com.hrchatbot.util;

import com.hrchatbot.config.MemoryConfig;
import com.hrchatbot.entity.ChatMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for managing conversation history based on message count.
 * Implements a sliding window approach to keep conversation context within message limits.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class MessageLimiter {
    
    private final MemoryConfig memoryConfig;
    
    /**
     * Trims conversation history to fit within the message limit.
     * Always includes the most recent messages that fit within the limit.
     * 
     * @param messages List of messages ordered from oldest to newest
     * @return Trimmed list of messages that fit within message budget
     */
    public List<ChatMessage> trimToMessageLimit(List<ChatMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return new ArrayList<>();
        }
        
        int messageLimit = memoryConfig.getTokenLimit();
        
        // If we have fewer messages than the limit, return all messages
        if (messages.size() <= messageLimit) {
            return new ArrayList<>(messages);
        }
        
        // Take the most recent messages up to the limit
        int startIndex = messages.size() - messageLimit;
        List<ChatMessage> trimmedMessages = new ArrayList<>(messages.subList(startIndex, messages.size()));
        
        log.debug("Trimmed conversation from {} to {} messages (limit: {})", 
                 messages.size(), trimmedMessages.size(), messageLimit);
        
        return trimmedMessages;
    }
    
    /**
     * Gets the current message limit
     * 
     * @return Message limit
     */
    public int getMessageLimit() {
        return memoryConfig.getTokenLimit();
    }
    
    /**
     * Checks if adding a message would exceed the current message limit
     * 
     * @param currentMessageCount Current message count in the conversation
     * @return True if adding one more message would exceed the limit
     */
    public boolean wouldExceedLimit(int currentMessageCount) {
        return currentMessageCount >= memoryConfig.getTokenLimit();
    }
    
    /**
     * Gets the number of messages that would be archived
     * 
     * @param totalMessages Total number of messages
     * @return Number of messages that would be archived
     */
    public int getArchiveCount(int totalMessages) {
        int messageLimit = memoryConfig.getTokenLimit();
        return Math.max(0, totalMessages - messageLimit);
    }
}
