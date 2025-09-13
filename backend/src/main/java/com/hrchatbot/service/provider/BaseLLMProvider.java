package com.hrchatbot.service.provider;

import com.hrchatbot.dto.ChatResponse;
import com.hrchatbot.entity.ChatMessage;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Abstract base class for LLM providers that provides common functionality
 * and reduces code duplication across different provider implementations.
 */
@Slf4j
public abstract class BaseLLMProvider implements LLMProvider {
    
    /**
     * Base system prompt for HR assistant
     */
    protected static final String BASE_SYSTEM_PROMPT = 
        "You are an HR virtual assistant that provides employees with clear, accurate, and professional guidance on " +
        "company policies, HR processes, and workplace-related questions. Your role is to answer in a friendly but " +
        "professional tone, making complex policies easy to understand without altering their meaning. Always keep " +
        "answers concise, factual, and aligned with company guidelines. If a policy detail is not available or " +
        "outside your scope, politely state this and suggest the employee contact the HR department for clarification. " +
        "Structure answers for readability, avoid jargon where possible, and ensure employees feel supported and " +
        "respected in every interaction.";
    
    /**
     * Template for when context is available from Pinecone
     */
    protected static final String PROMPT_WITH_CONTEXT_AND_HISTORY = 
        "SYSTEM:\n" + BASE_SYSTEM_PROMPT + "\n\n" +
        "CONTEXT (from Pinecone):\n{retrieved_context}\n\n" +
        "CONVERSATION HISTORY (if available):\n{conversation_history}\n\n" +
        "USER QUERY:\n{user_query}\n\n" +
        "INSTRUCTION:\n" +
        "Answer the user's question using the provided context. If multiple pieces of context are provided, " +
        "merge them into a single, clear answer. Do not invent policies not present in the context.";
    
    /**
     * Template for when no context is available but conversation history exists
     */
    protected static final String PROMPT_WITHOUT_CONTEXT_WITH_HISTORY = 
        "SYSTEM:\n" + BASE_SYSTEM_PROMPT + "\n\n" +
        "CONVERSATION HISTORY (if available):\n{conversation_history}\n\n" +
        "USER QUERY:\n{user_query}\n\n" +
        "INSTRUCTION:\n" +
        "Since no policy context is available, answer using general HR best practices, But always mention it"
        + ". Clearly state that you may " +
        "not have access to specific company details, and suggest contacting HR for confirmation if needed.";
    
    /**
     * Template for new chat without context
     */
    protected static final String PROMPT_NEW_CHAT_WITHOUT_CONTEXT = 
        "SYSTEM:\n" + BASE_SYSTEM_PROMPT + "\n\n" +
        "USER QUERY:\n{user_query}\n\n" +
        "INSTRUCTION:\n" +
        "Provide a clear, standalone answer since this is the start of a new conversation, and mention that this is "
        + "out of context of company's policies . Do not reference any prior chat.";
    
    /**
     * Template for new chat with context
     */
    protected static final String PROMPT_NEW_CHAT_WITH_CONTEXT = 
        "SYSTEM:\n" + BASE_SYSTEM_PROMPT + "\n\n" +
        "CONTEXT (if available):\n{retrieved_context}\n\n" +
        "USER QUERY:\n{user_query}\n\n" +
        "INSTRUCTION:\n" +
        "Provide a clear, standalone answer since this is the start of a new conversation. Do not reference any prior"
        + " chat. And answer with the context provided only, if not available then answer it yourself but do mention "
        + "it that it is not within the company policies";
    
    /**
     * Template for ongoing conversation with context
     */
    protected static final String PROMPT_ONGOING_CONVERSATION_WITH_CONTEXT = 
        "SYSTEM:\n" + BASE_SYSTEM_PROMPT + "\n\n" +
        "CONTEXT (if available):\n{retrieved_context}\n\n" +
        "CONVERSATION HISTORY:\n{conversation_history}\n\n" +
        "USER QUERY:\n{user_query}\n\n" +
        "INSTRUCTION:\n" +
        "Answer the user's query in the context of the ongoing conversation. Use both the provided context and " +
        "conversation history to maintain continuity and avoid repeating information unnecessarily.";
    
    /**
     * Builds the system prompt based on whether context is provided
     * 
     * @param context Additional context for the LLM
     * @return Formatted system prompt
     */
    protected String buildSystemPrompt(String context) {
        return BASE_SYSTEM_PROMPT;
    }
    
    /**
     * Formats conversation history into a readable string
     * 
     * @param conversationHistory List of conversation messages
     * @return Formatted conversation string
     */
    protected String formatConversationHistory(List<ChatMessage> conversationHistory) {
        if (conversationHistory == null || conversationHistory.isEmpty()) {
            return "";
        }
        
        StringBuilder formatted = new StringBuilder();
        for (ChatMessage msg : conversationHistory) {
            formatted.append(msg.getRole().name())
                    .append(": ")
                    .append(msg.getMessage())
                    .append("\n");
        }
        return formatted.toString();
    }
    
    /**
     * Builds a complete prompt using the new structured format
     * 
     * @param userMessage The user's current message
     * @param conversationHistory Previous conversation messages
     * @param context Additional context from Pinecone
     * @return Complete formatted prompt
     */
    protected String buildCompletePrompt(String userMessage, List<ChatMessage> conversationHistory, String context) {
        String history = formatConversationHistory(conversationHistory);
        boolean hasContext = context != null && !context.trim().isEmpty();
        boolean hasHistory = history != null && !history.trim().isEmpty();
        
        // Determine which template to use based on context and history availability
        String template;
        if (hasContext && hasHistory) {
            // Case 1: Has context from Pinecone and conversation history
            template = PROMPT_WITH_CONTEXT_AND_HISTORY;
        } else if (!hasContext && hasHistory) {
            // Case 2: No context but has conversation history
            template = PROMPT_WITHOUT_CONTEXT_WITH_HISTORY;
        } else if (hasContext && !hasHistory) {
            // Case 3: Has context but no conversation history (new chat)
            template = PROMPT_NEW_CHAT_WITH_CONTEXT;
        } else {
            // Case 4: No context and no history (completely new chat)
            template = PROMPT_NEW_CHAT_WITHOUT_CONTEXT;
        }
        
        // Replace placeholders in the template
        String prompt = template
            .replace("{retrieved_context}", hasContext ? context : "")
            .replace("{conversation_history}", hasHistory ? history : "")
            .replace("{user_query}", userMessage);
        
        return prompt;
    }
    
    /**
     * Creates a standard ChatResponse with error handling
     * 
     * @param message The response message
     * @param context The context used
     * @return ChatResponse object
     */
    protected ChatResponse createResponse(String message, String context) {
        return ChatResponse.builder()
                .message(message)
                .contextUsed(context)
                .build();
    }
    
    /**
     * Creates an error response
     * 
     * @param errorMessage The error message
     * @param context The context used
     * @return ChatResponse with error information
     */
    protected ChatResponse createErrorResponse(String errorMessage, String context) {
        return ChatResponse.builder()
                .message("I apologize, but I'm experiencing technical difficulties. Please try again later.")
                .contextUsed(context)
                .success(false)
                .error(errorMessage)
                .build();
    }
    
    /**
     * Handles exceptions and creates appropriate error responses
     * 
     * @param e The exception that occurred
     * @param context The context used
     * @return ChatResponse with error information
     */
    protected ChatResponse handleException(Exception e, String context) {
        log.error("Error in {} provider: {}", getProviderName(), e.getMessage());
        return createErrorResponse("Provider error: " + e.getMessage(), context);
    }
}
