package com.hrchatbot.exception;

/**
 * Exception thrown when a chat room is not found
 */
public class ChatRoomNotFoundException extends RuntimeException {
    public ChatRoomNotFoundException(String message) {
        super(message);
    }
    
    public ChatRoomNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
