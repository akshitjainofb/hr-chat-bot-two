package com.hrchatbot.dto;

import com.hrchatbot.entity.ChatMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for managing conversation memory including both short-term and long-term memory.
 * This encapsulates the hybrid memory approach combining database sliding window and Pinecone retrieval.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationMemory {
    
    /**
     * Short-term memory: Recent messages from database within token limit
     */
    private List<ChatMessage> shortTermMemory;
    
    /**
     * Long-term memory: Relevant older messages retrieved from Pinecone
     */
    private List<String> longTermMemory;
    
    /**
     * Document context: Relevant HR documents retrieved from Pinecone
     */
    private List<String> documentContext;
    
    /**
     * Total estimated token count for all memory components
     */
    private int totalTokenCount;
    
    /**
     * Combines all memory components into a single context string
     * 
     * @return Combined context string
     */
    public String getCombinedContext() {
        StringBuilder context = new StringBuilder();
        
        // Add long-term memory first (older, more general context)
        if (longTermMemory != null && !longTermMemory.isEmpty()) {
            context.append("Previous conversation context:\n");
            for (String memory : longTermMemory) {
                context.append("- ").append(memory).append("\n");
            }
            context.append("\n");
        }
        
        // Add document context
        if (documentContext != null && !documentContext.isEmpty()) {
            context.append("Relevant HR documents:\n");
            for (String doc : documentContext) {
                context.append(doc).append("\n\n");
            }
        }
        
        return context.toString().trim();
    }
    
    /**
     * Gets the conversation history as formatted strings for LLM context
     * 
     * @return List of formatted conversation messages
     */
    public List<String> getFormattedConversationHistory() {
        if (shortTermMemory == null || shortTermMemory.isEmpty()) {
            return List.of();
        }
        
        return shortTermMemory.stream()
                .map(msg -> String.format("%s: %s", 
                    msg.getRole().name(), 
                    msg.getMessage()))
                .toList();
    }
    
    /**
     * Checks if the memory is empty
     * 
     * @return True if no memory components have content
     */
    public boolean isEmpty() {
        boolean shortTermEmpty = shortTermMemory == null || shortTermMemory.isEmpty();
        boolean longTermEmpty = longTermMemory == null || longTermMemory.isEmpty();
        boolean docEmpty = documentContext == null || documentContext.isEmpty();
        
        return shortTermEmpty && longTermEmpty && docEmpty;
    }
    
    /**
     * Gets a summary of memory usage for logging
     * 
     * @return Memory usage summary
     */
    public String getMemorySummary() {
        int shortTermCount = shortTermMemory != null ? shortTermMemory.size() : 0;
        int longTermCount = longTermMemory != null ? longTermMemory.size() : 0;
        int docCount = documentContext != null ? documentContext.size() : 0;
        
        return String.format("Memory: %d short-term, %d long-term, %d docs, %d tokens", 
                           shortTermCount, longTermCount, docCount, totalTokenCount);
    }
}
