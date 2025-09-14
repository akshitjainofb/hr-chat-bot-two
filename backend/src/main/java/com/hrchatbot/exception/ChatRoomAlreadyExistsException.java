package com.hrchatbot.exception;

/**
 * Exception thrown when trying to create a chat room with a name that already exists
 */
public class ChatRoomAlreadyExistsException extends RuntimeException {
    public ChatRoomAlreadyExistsException(String message) {
        super(message);
    }
    
    public ChatRoomAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }
}
