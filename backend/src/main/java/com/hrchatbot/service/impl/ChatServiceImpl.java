package com.hrchatbot.service.impl;

import com.hrchatbot.dto.ChatMessageDto;
import com.hrchatbot.dto.ChatRequest;
import com.hrchatbot.dto.ChatResponse;
import com.hrchatbot.dto.ChatRoomDto;
import com.hrchatbot.dto.ConversationMemory;
import com.hrchatbot.entity.ChatMessage;
import com.hrchatbot.entity.ChatRoom;
import com.hrchatbot.entity.User;
import com.hrchatbot.exception.ChatRoomAlreadyExistsException;
import com.hrchatbot.exception.ChatRoomNotFoundException;
import com.hrchatbot.exception.UnauthorizedAccessException;
import com.hrchatbot.repository.ChatMessageRepository;
import com.hrchatbot.repository.ChatRoomRepository;
import com.hrchatbot.service.ConversationMemoryService;
import com.hrchatbot.service.LlmService;
import com.hrchatbot.service.PineconeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatServiceImpl {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final LlmService llmService;
    private final PineconeService pineconeService;
    private final ConversationMemoryService conversationMemoryService;

    @Transactional
    public ChatResponse sendMessage(ChatRequest request, User user) {
        long totalStartTime = System.currentTimeMillis();
        try {
            // Get chat room
            long dbStartTime = System.currentTimeMillis();
            ChatRoom chatRoom = chatRoomRepository.findById(request.getChatRoomId())
                    .orElseThrow(() -> new RuntimeException("Chat room not found"));
            
            // Verify user owns the chat room
            if (!chatRoom.getUser().getId().equals(user.getId())) {
                throw new RuntimeException("Unauthorized access to chat room");
            }
            long dbTime = System.currentTimeMillis() - dbStartTime;
            log.debug("Database operations completed in {}ms", dbTime);
            
            // Use the room's context setting, fallback to request setting if not set
            Boolean useContext = chatRoom.getIncludeContext() != null ? 
                chatRoom.getIncludeContext() : 
                (request.getIncludeContext() != null ? request.getIncludeContext() : true);
            
            log.debug("Processing message for chat room {} and user {} with context: {} (document context always included)", 
                     chatRoom.getId(), user.getEmail(), useContext);
            
            // Always retrieve document context from Pinecone
            long pineconeStartTime = System.currentTimeMillis();
            List<String> documentContext = pineconeService.searchSimilarContent(
                    request.getMessage(), user, 5); // Use a reasonable limit
            long pineconeTime = System.currentTimeMillis() - pineconeStartTime;
            log.debug("Pinecone document search completed in {}ms", pineconeTime);
            
            // Build conversation memory using hybrid approach (only if context is enabled)
            long memoryStartTime = System.currentTimeMillis();
            ConversationMemory conversationMemory = null;
            if (useContext) {
                conversationMemory = conversationMemoryService
                        .buildConversationMemory(chatRoom, request.getMessage(), user);
            } else {
                // Create a minimal conversation memory with only document context
                conversationMemory = ConversationMemory.builder()
                        .shortTermMemory(List.of()) // No conversation history
                        .longTermMemory(List.of()) // No long-term memory
                        .documentContext(documentContext) // Only document context
                        .totalTokenCount(0)
                        .build();
            }
            long memoryTime = System.currentTimeMillis() - memoryStartTime;
            log.debug("Conversation memory building completed in {}ms", memoryTime);
            
            // Generate response using the new memory-aware LLM service
            long llmStartTime = System.currentTimeMillis();
            ChatResponse response = llmService.generateResponseWithMemory(
                    request.getMessage(),
                    conversationMemory,
                    request.getLlmProvider(),
                    user
            );
            long llmTime = System.currentTimeMillis() - llmStartTime;
            log.debug("LLM response generation completed in {}ms", llmTime);
            
            // Save user message
            long saveStartTime = System.currentTimeMillis();
            ChatMessage userMessage = ChatMessage.builder()
                    .chatRoom(chatRoom)
                    .role(ChatMessage.MessageRole.USER)
                    .message(request.getMessage())
                    .build();
            chatMessageRepository.save(userMessage);
            
            // Save assistant response
            ChatMessage assistantMessage = ChatMessage.builder()
                    .chatRoom(chatRoom)
                    .role(ChatMessage.MessageRole.ASSISTANT)
                    .message(response.getMessage())
                    .contextUsed(conversationMemory != null ? conversationMemory.getCombinedContext() : "Document context only")
                    .llmProviderUsed(response.getLlmProviderUsed())
                    .build();
            chatMessageRepository.save(assistantMessage);
            long saveTime = System.currentTimeMillis() - saveStartTime;
            log.debug("Message saving completed in {}ms", saveTime);
            
            // Update response with saved message ID
            response.setMessage(assistantMessage.getMessage());
            
            long totalTime = System.currentTimeMillis() - totalStartTime;
            log.info("Total chat processing time: {}ms (DB: {}ms, Pinecone: {}ms, Memory: {}ms, LLM: {}ms, Save: {}ms)", 
                    totalTime, dbTime, pineconeTime, memoryTime, llmTime, saveTime);
            
            if (useContext) {
                log.debug("Successfully processed message with full context: {}", 
                         conversationMemory.getMemorySummary());
            } else {
                log.debug("Successfully processed message with document context only (no conversation history)");
            }
            
            return response;
            
        } catch (Exception e) {
            log.error("Error processing chat message: {}", e.getMessage());
            return ChatResponse.builder()
                    .message("I apologize, but I encountered an error processing your message. Please try again.")
                    .success(false)
                    .error(e.getMessage())
                    .build();
        }
    }

    public List<ChatRoomDto> getUserChatRooms(User user) {
        List<ChatRoom> chatRooms = chatRoomRepository.findByUserOrderByCreatedAtDesc(user);
        return chatRooms.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public ChatRoomDto createChatRoom(String name, User user) {
        if (chatRoomRepository.existsByUserAndName(user, name)) {
            throw new ChatRoomAlreadyExistsException("A chat room with the name '" + name + "' already exists");
        }
        
        ChatRoom chatRoom = ChatRoom.builder()
                .name(name)
                .user(user)
                .build();
        
        chatRoom = chatRoomRepository.save(chatRoom);
        return convertToDto(chatRoom);
    }

    @Transactional
    public ChatRoomDto updateChatRoom(Long roomId, String name, User user) {
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new ChatRoomNotFoundException("Chat room not found"));
        
        if (!chatRoom.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedAccessException("You don't have permission to modify this chat room");
        }
        
        // Check if another chat room with the same name exists (excluding current room)
        if (chatRoomRepository.existsByUserAndNameAndIdNot(user, name, roomId)) {
            throw new ChatRoomAlreadyExistsException("A chat room with the name '" + name + "' already exists");
        }
        
        chatRoom.setName(name);
        chatRoom = chatRoomRepository.save(chatRoom);
        return convertToDto(chatRoom);
    }

    @Transactional
    public void deleteChatRoom(Long roomId, User user) {
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new ChatRoomNotFoundException("Chat room not found"));
        
        if (!chatRoom.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedAccessException("You don't have permission to delete this chat room");
        }
        
        // Clear conversation memory from Pinecone before deleting the chat room
        try {
            conversationMemoryService.clearConversationMemory(chatRoom, user);
        } catch (Exception e) {
            log.warn("Failed to clear conversation memory for chat room {}: {}", roomId, e.getMessage());
            // Continue with deletion even if memory clearing fails
        }
        
        chatRoomRepository.delete(chatRoom);
    }

    public ChatRoomDto getChatRoom(Long roomId, User user) {
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Chat room not found"));
        
        if (!chatRoom.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized access to chat room");
        }
        
        return convertToDto(chatRoom);
    }

    @Transactional
    public ChatRoomDto updateContextSetting(Long roomId, Boolean includeContext, User user) {
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Chat room not found"));
        
        if (!chatRoom.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized access to chat room");
        }
        
        chatRoom.setIncludeContext(includeContext);
        chatRoom = chatRoomRepository.save(chatRoom);
        
        log.debug("Updated context setting for room {} to {}", roomId, includeContext);
        return convertToDto(chatRoom);
    }

    @Transactional
    public void clearChatMessages(Long roomId, User user) {
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Chat room not found"));
        
        if (!chatRoom.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized access to chat room");
        }
        
        // Clear conversation memory from Pinecone before clearing messages
        try {
            conversationMemoryService.clearConversationMemory(chatRoom, user);
        } catch (Exception e) {
            log.warn("Failed to clear conversation memory for chat room {}: {}", roomId, e.getMessage());
            // Continue with message clearing even if memory clearing fails
        }
        
        chatMessageRepository.deleteByChatRoom(chatRoom);
    }
    
    private ChatRoomDto convertToDto(ChatRoom chatRoom) {
        List<ChatMessageDto> messages = chatRoom.getMessages() != null ?
                chatRoom.getMessages().stream()
                        .map(ChatMessageDto::fromEntity)
                        .collect(Collectors.toList()) :
                List.of();
        
        return ChatRoomDto.builder()
                .id(chatRoom.getId())
                .name(chatRoom.getName())
                .createdAt(chatRoom.getCreatedAt())
                .messages(messages)
                .messageCount(messages.size())
                .includeContext(chatRoom.getIncludeContext())
                .build();
    }
}
