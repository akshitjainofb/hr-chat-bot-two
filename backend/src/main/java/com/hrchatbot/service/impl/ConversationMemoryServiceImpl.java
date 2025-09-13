package com.hrchatbot.service.impl;

import com.hrchatbot.config.MemoryConfig;
import com.hrchatbot.dto.ConversationMemory;
import com.hrchatbot.entity.ChatMessage;
import com.hrchatbot.entity.ChatRoom;
import com.hrchatbot.entity.User;
import com.hrchatbot.repository.ChatMessageRepository;
import com.hrchatbot.service.ConversationMemoryService;
import com.hrchatbot.service.PineconeService;
import com.hrchatbot.util.TokenLimiter;
import com.hrchatbot.util.Tokenizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of ConversationMemoryService that manages hybrid memory approach.
 * Combines database sliding window with Pinecone long-term memory.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ConversationMemoryServiceImpl implements ConversationMemoryService {
    
    private final ChatMessageRepository chatMessageRepository;
    private final PineconeService pineconeService;
    private final TokenLimiter tokenLimiter;
    private final Tokenizer tokenizer;
    private final MemoryConfig memoryConfig;
    
    @Override
    public ConversationMemory buildConversationMemory(ChatRoom chatRoom, String userQuery, User user) {
        log.debug("Building conversation memory for chat room {} and user {}", 
                 chatRoom.getId(), user.getEmail());
        
        // 1. Get all recent messages from database
        List<ChatMessage> allRecentMessages = chatMessageRepository
                .findByChatRoomOrderByCreatedAtAsc(chatRoom);
        
        // 2. Apply sliding window based on token count
        List<ChatMessage> shortTermMemory = tokenLimiter.trimToTokenLimit(allRecentMessages);
        
        // 3. Get messages that fell outside the sliding window
        List<ChatMessage> messagesToArchive = getMessagesToArchive(allRecentMessages, shortTermMemory);
        
        // 4. Archive old messages to long-term memory if needed
        if (!messagesToArchive.isEmpty()) {
            archiveToLongTermMemory(chatRoom, user, messagesToArchive);
        }
        
        // 5. Retrieve relevant long-term memory from Pinecone
        List<String> longTermMemory = pineconeService.searchConversationMemory(
                userQuery, user, memoryConfig.getLongTermMemoryLimit());
        
        // 6. Retrieve relevant document context from Pinecone
        List<String> documentContext = pineconeService.searchSimilarContent(
                userQuery, user, memoryConfig.getDocumentContextLimit());
        
        // 7. Calculate total token count
        int totalTokenCount = calculateTotalTokenCount(shortTermMemory, longTermMemory, documentContext);
        
        // 8. Build and return conversation memory
        ConversationMemory memory = ConversationMemory.builder()
                .shortTermMemory(shortTermMemory)
                .longTermMemory(longTermMemory)
                .documentContext(documentContext)
                .totalTokenCount(totalTokenCount)
                .build();
        
        log.debug("Built conversation memory: {}", memory.getMemorySummary());
        return memory;
    }
    
    @Override
    @Transactional
    public void archiveToLongTermMemory(ChatRoom chatRoom, User user, List<ChatMessage> messagesToArchive) {
        if (messagesToArchive == null || messagesToArchive.isEmpty()) {
            return;
        }
        
        log.debug("Archiving {} messages to long-term memory for chat room {}", 
                 messagesToArchive.size(), chatRoom.getId());
        
        // Archive in batches to avoid overwhelming Pinecone
        List<List<ChatMessage>> batches = partitionList(messagesToArchive, memoryConfig.getArchiveBatchSize());
        
        for (List<ChatMessage> batch : batches) {
            try {
                pineconeService.indexConversationMemory(batch, user);
                log.debug("Successfully archived batch of {} messages", batch.size());
            } catch (Exception e) {
                log.error("Failed to archive batch of messages: {}", e.getMessage());
                // Continue with next batch even if one fails
            }
        }
    }
    
    @Override
    public void clearConversationMemory(ChatRoom chatRoom, User user) {
        log.debug("Clearing conversation memory for chat room {} and user {}", 
                 chatRoom.getId(), user.getEmail());
        
        try {
            pineconeService.deleteConversationMemory(chatRoom.getId(), user);
            log.info("Successfully cleared conversation memory for chat room {}", chatRoom.getId());
        } catch (Exception e) {
            log.error("Failed to clear conversation memory: {}", e.getMessage());
            throw new RuntimeException("Failed to clear conversation memory", e);
        }
    }
    
    /**
     * Gets messages that should be archived to long-term memory.
     * These are messages that are in the full list but not in the short-term memory.
     * 
     * @param allMessages All recent messages from database
     * @param shortTermMemory Messages that fit in the sliding window
     * @return Messages that should be archived
     */
    private List<ChatMessage> getMessagesToArchive(List<ChatMessage> allMessages, 
                                                  List<ChatMessage> shortTermMemory) {
        if (allMessages.size() <= shortTermMemory.size()) {
            return new ArrayList<>();
        }
        
        // Get the oldest messages that didn't fit in the sliding window
        int archiveCount = allMessages.size() - shortTermMemory.size();
        return allMessages.subList(0, archiveCount);
    }
    
    /**
     * Calculates total token count for all memory components
     * 
     * @param shortTermMemory Short-term memory messages
     * @param longTermMemory Long-term memory strings
     * @param documentContext Document context strings
     * @return Total estimated token count
     */
    private int calculateTotalTokenCount(List<ChatMessage> shortTermMemory, 
                                       List<String> longTermMemory, 
                                       List<String> documentContext) {
        int totalTokens = 0;
        
        // Count short-term memory tokens
        if (shortTermMemory != null) {
            for (ChatMessage message : shortTermMemory) {
                totalTokens += tokenizer.countTokens(message.getMessage());
            }
        }
        
        // Count long-term memory tokens
        if (longTermMemory != null) {
            for (String memory : longTermMemory) {
                totalTokens += tokenizer.countTokens(memory);
            }
        }
        
        // Count document context tokens
        if (documentContext != null) {
            for (String doc : documentContext) {
                totalTokens += tokenizer.countTokens(doc);
            }
        }
        
        return totalTokens;
    }
    
    /**
     * Partitions a list into smaller batches
     * 
     * @param list The list to partition
     * @param batchSize The size of each batch
     * @return List of batches
     */
    private <T> List<List<T>> partitionList(List<T> list, int batchSize) {
        List<List<T>> batches = new ArrayList<>();
        
        for (int i = 0; i < list.size(); i += batchSize) {
            int end = Math.min(i + batchSize, list.size());
            batches.add(new ArrayList<>(list.subList(i, end)));
        }
        
        return batches;
    }
}
