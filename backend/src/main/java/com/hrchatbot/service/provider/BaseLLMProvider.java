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
        "You are an HR virtual assistant for the company. " +
            "Answer exactly what is asked using concise, accurate guidance on company policies, HR processes, and workplace questions. " +
            "Use short paragraphs or bullet points. Context and history are for your use only—never mention them. " +
            "For non-company questions, answer concisely using general HR knowledge without extra assumptions.";

    protected static final String PROMPT_WITH_CONTEXT_AND_HISTORY =
        "SYSTEM:\n" + BASE_SYSTEM_PROMPT + "\n\n" +
            "CONTEXT:\n{retrieved_context}\n\n" +
            "HISTORY:\n{conversation_history}\n\n" +
            "QUERY:\n{user_query}\n\n" +
            "INSTRUCTIONS:\n- Use context as the primary source. Never mention it.\n" +
            "- Use history only for continuity. Never mention it.\n" +
            "- Answer strictly only what is asked.\n" +
            "- If not answerable using company context, state: 'Out of scope.'\n" +
            "- and, answer concisely using general HR knowledge.";

    protected static final String PROMPT_WITHOUT_CONTEXT_WITH_HISTORY =
        "SYSTEM:\n" + BASE_SYSTEM_PROMPT + "\n\n" +
            "HISTORY:\n{conversation_history}\n\n" +
            "QUERY:\n{user_query}\n\n" +
            "INSTRUCTIONS:\n- Use history only for continuity. Never mention it.\n" +
            "- Answer strictly only what is asked.\n" +
            "- If not answerable using company context, state: 'Out of scope.'\n" +
            "- and, answer concisely using general HR knowledge.";

    protected static final String PROMPT_NEW_CHAT_WITHOUT_CONTEXT =
        "SYSTEM:\n" + BASE_SYSTEM_PROMPT + "\n\n" +
            "QUERY:\n{user_query}\n\n" +
            "INSTRUCTIONS:\n- Since no company data is available, answer strictly only using general HR knowledge.\n" +
            "- state: 'Out of scope.'\n" +
            "- and, answer concisely using general HR knowledge." +
            "- Do not invent company-specific answers.";

    protected static final String PROMPT_NEW_CHAT_WITH_CONTEXT =
        "SYSTEM:\n" + BASE_SYSTEM_PROMPT + "\n\n" +
            "CONTEXT:\n{retrieved_context}\n\n" +
            "QUERY:\n{user_query}\n\n" +
            "INSTRUCTIONS:\n- Use context as the source. Never mention it.\n" +
            "- Answer strictly only what is asked.\n" +
            "- If not answerable using company context, state: 'Out of scope.'\n" +
            "- and, answer concisely using general HR knowledge.";

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
     * Formats conversation history into a readable string with token optimization
     * 
     * @param conversationHistory List of conversation messages
     * @return Formatted conversation string (limited to last 6 messages for performance)
     */
    protected String formatConversationHistory(List<ChatMessage> conversationHistory) {
        if (conversationHistory == null || conversationHistory.isEmpty()) {
            return "";
        }
        
        // Limit to last 6 messages to reduce token usage and improve performance
        List<ChatMessage> recentMessages = conversationHistory.size() > 6 ? 
            conversationHistory.subList(conversationHistory.size() - 6, conversationHistory.size()) : 
            conversationHistory;
        
        StringBuilder formatted = new StringBuilder();
        for (ChatMessage msg : recentMessages) {
            String role = msg.getRole().name().equals("USER") ? "User" : "Assistant";
            String message = msg.getMessage();
            // Truncate very long messages to reduce token usage
            if (message.length() > 200) {
                message = message.substring(0, 200) + "...";
            }
            formatted.append(role).append(": ").append(message).append("\n");
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
        
        // Truncate context if too long to improve performance
        String truncatedContext = context;
        if (hasContext && context.length() > 3000) {
            truncatedContext = context.substring(0, 3000) + "...\n[Context truncated for performance]";
            log.debug("Context truncated from {} to {} characters", context.length(), truncatedContext.length());
        }
        
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
            .replace("{retrieved_context}", hasContext ? truncatedContext : "")
            .replace("{conversation_history}", hasHistory ? history : "")
            .replace("{user_query}", userMessage);
        
        log.debug("Built prompt with {} characters (context: {}, history: {})", 
                 prompt.length(), hasContext ? truncatedContext.length() : 0, hasHistory ? history.length() : 0);
        
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
