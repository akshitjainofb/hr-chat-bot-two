package com.hrchatbot.controller;

import com.hrchatbot.dto.ChatRequest;
import com.hrchatbot.dto.ChatResponse;
import com.hrchatbot.dto.ChatRoomDto;
import com.hrchatbot.entity.User;
import com.hrchatbot.service.impl.ChatServiceImpl;
import com.hrchatbot.service.impl.UserServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final ChatServiceImpl chatService;
    private final UserServiceImpl userService;

    @PostMapping("/send")
    public ResponseEntity<ChatResponse> sendMessage(@Valid @RequestBody ChatRequest request) {
        try {
            String userEmail = request.getUserEmail();
            if (userEmail == null || userEmail.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                    ChatResponse.builder()
                        .message("User email is required")
                        .success(false)
                        .error("User email is required")
                        .build()
                );
            }
            
            User user = userService.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            log.debug("Processing chat message for user: {} in room: {}", userEmail, request.getChatRoomId());
            ChatResponse response = chatService.sendMessage(request, user);
            
            if (response.isSuccess()) {
                log.debug("Successfully processed message for user: {}", userEmail);
                return ResponseEntity.ok(response);
            } else {
                log.warn("Failed to process message for user: {}, error: {}", userEmail, response.getError());
                return ResponseEntity.badRequest().body(response);
            }
            
        } catch (org.springframework.web.context.request.async.AsyncRequestTimeoutException e) {
            log.error("Request timeout for user: {}", request.getUserEmail());
            return ResponseEntity.status(408).body(
                ChatResponse.builder()
                    .message("Request timed out. Please try again.")
                    .success(false)
                    .error("Request timeout - the server took too long to respond")
                    .build()
            );
        } catch (Exception e) {
            log.error("Error sending message for user: {}: {}", request.getUserEmail(), e.getMessage(), e);
            return ResponseEntity.status(500).body(
                ChatResponse.builder()
                    .message("I apologize, but I encountered an error processing your message. Please try again.")
                    .success(false)
                    .error("Internal server error: " + e.getMessage())
                    .build()
            );
        }
    }

    @GetMapping("/rooms")
    public ResponseEntity<List<ChatRoomDto>> getUserChatRooms(@RequestParam String userEmail) {
        try {
            User user = userService.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
            List<ChatRoomDto> rooms = chatService.getUserChatRooms(user);
            return ResponseEntity.ok(rooms);
        } catch (Exception e) {
            log.error("Error getting chat rooms: ", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/rooms")
    public ResponseEntity<ChatRoomDto> createChatRoom(@RequestBody Map<String, String> request) {
        try {
            String name = request.get("name");
            String userEmail = request.get("userEmail");
            
            if (name == null || name.trim().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            
            if (userEmail == null || userEmail.trim().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            
            // Get user by email
            User user = userService.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            ChatRoomDto room = chatService.createChatRoom(name.trim(), user);
            return ResponseEntity.ok(room);
        } catch (Exception e) {
            log.error("Error creating chat room: ", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/rooms/{roomId}")
    public ResponseEntity<ChatRoomDto> getChatRoom(@PathVariable Long roomId, @RequestParam String userEmail) {
        try {
            User user = userService.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
            ChatRoomDto room = chatService.getChatRoom(roomId, user);
            return ResponseEntity.ok(room);
        } catch (Exception e) {
            log.error("Error getting chat room: ", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/rooms/{roomId}")
    public ResponseEntity<ChatRoomDto> updateChatRoom(@PathVariable Long roomId, 
                                                     @RequestBody Map<String, String> request) {
        try {
            String userEmail = request.get("userEmail");
            if (userEmail == null || userEmail.trim().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            
            User user = userService.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            String name = request.get("name");
            if (name == null || name.trim().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            
            ChatRoomDto room = chatService.updateChatRoom(roomId, name.trim(), user);
            return ResponseEntity.ok(room);
        } catch (Exception e) {
            log.error("Error updating chat room: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/rooms/{roomId}")
    public ResponseEntity<Void> deleteChatRoom(@PathVariable Long roomId, @RequestParam String userEmail) {
        try {
            User user = userService.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
            chatService.deleteChatRoom(roomId, user);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error deleting chat room: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/rooms/{roomId}/messages")
    public ResponseEntity<Void> clearChatMessages(@PathVariable Long roomId, @RequestParam String userEmail) {
        try {
            User user = userService.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
            chatService.clearChatMessages(roomId, user);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error clearing chat messages: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/rooms/{roomId}/context")
    public ResponseEntity<ChatRoomDto> updateContextSetting(@PathVariable Long roomId, 
                                                           @RequestBody Map<String, Object> request) {
        try {
            String userEmail = (String) request.get("userEmail");
            if (userEmail == null || userEmail.trim().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            
            User user = userService.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            Boolean includeContext = (Boolean) request.get("includeContext");
            if (includeContext == null) {
                return ResponseEntity.badRequest().build();
            }
            
            ChatRoomDto room = chatService.updateContextSetting(roomId, includeContext, user);
            return ResponseEntity.ok(room);
        } catch (Exception e) {
            log.error("Error updating context setting: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
}
