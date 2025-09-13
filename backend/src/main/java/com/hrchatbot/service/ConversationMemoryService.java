package com.hrchatbot.service;

import com.hrchatbot.dto.ConversationMemory;
import com.hrchatbot.entity.ChatMessage;
import com.hrchatbot.entity.ChatRoom;
import com.hrchatbot.entity.User;

import java.util.List;

/**
 * Service interface for managing hybrid conversation memory.
 * Combines short-term memory (database sliding window) with long-term memory (Pinecone).
 */
public interface ConversationMemoryService {
    
    /**
     * Builds conversation memory for a given chat room and user query.
     * 
     * @param chatRoom The chat room to build memory for
     * @param userQuery The current user query
     * @param user The user making the query
     * @return ConversationMemory containing short-term, long-term, and document context
     */
    ConversationMemory buildConversationMemory(ChatRoom chatRoom, String userQuery, User user);
    
    /**
     * Archives old messages to long-term memory when they fall outside the sliding window.
     * 
     * @param chatRoom The chat room to archive messages for
     * @param user The user who owns the chat room
     * @param messagesToArchive Messages that should be moved to long-term memory
     */
    void archiveToLongTermMemory(ChatRoom chatRoom, User user, List<ChatMessage> messagesToArchive);
    
    /**
     * Clears all conversation memory for a chat room.
     * 
     * @param chatRoom The chat room to clear memory for
     * @param user The user who owns the chat room
     */
    void clearConversationMemory(ChatRoom chatRoom, User user);
}
