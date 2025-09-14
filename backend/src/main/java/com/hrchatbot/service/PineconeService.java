package com.hrchatbot.service;

import com.hrchatbot.entity.ChatMessage;
import com.hrchatbot.entity.PdfDocument;
import com.hrchatbot.entity.User;

import java.util.List;

public interface PineconeService {
    
    void indexPdfDocument(PdfDocument pdfDocument, String content);
    
    List<String> searchSimilarContent(String query, User user, int topK);
    
    /**
     * Indexes chat messages for long-term memory storage
     * 
     * @param messages List of chat messages to index
     * @param user The user who owns these messages
     */
    void indexConversationMemory(List<ChatMessage> messages, User user);
    
    /**
     * Searches for relevant conversation history based on current query
     * 
     * @param query Current user query
     * @param user The user to search for
     * @param chatRoomId The chat room ID to search within
     * @param topK Number of relevant messages to retrieve
     * @return List of relevant conversation snippets
     */
    List<String> searchConversationMemory(String query, User user, Long chatRoomId, int topK);
    
    /**
     * Deletes conversation memory for a specific chat room
     * 
     * @param chatRoomId The chat room ID to delete memory for
     * @param user The user who owns the chat room
     */
    void deleteConversationMemory(Long chatRoomId, User user);
    
    /**
     * Deletes a PDF document from Pinecone vector database
     * 
     * @param pdfDocument The PDF document to delete from Pinecone
     * @param user The user who owns the document
     */
    void deletePdfDocument(PdfDocument pdfDocument, User user);

}
