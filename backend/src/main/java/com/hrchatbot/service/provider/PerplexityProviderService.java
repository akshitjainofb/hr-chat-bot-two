package com.hrchatbot.service.provider;

import com.hrchatbot.config.LLMProviderConfig;
import com.hrchatbot.dto.ChatResponse;
import com.hrchatbot.entity.ChatMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Perplexity AI provider service implementation.
 * Provides efficient AI responses using Perplexity's API.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PerplexityProviderService implements LLMProvider {

    private final RestTemplate restTemplate;
    private final LLMProviderConfig config;
    
    private static final String PROVIDER_NAME = "perplexity";
    private static final String API_URL = "https://api.perplexity.ai/chat/completions";
    
    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }
    
    @Override
    public boolean isAvailable() {
        try {
            LLMProviderConfig.ProviderConfig providerConfig = config.getProviderConfig(PROVIDER_NAME);
            return providerConfig != null && 
                   providerConfig.isEnabled() && 
                   providerConfig.getApiKey() != null && 
                   !providerConfig.getApiKey().trim().isEmpty();
        } catch (Exception e) {
            log.error("Error checking Perplexity availability: {}", e.getMessage());
            return false;
        }
    }
    
    @Override
    public ChatResponse generateResponse(String userMessage, List<ChatMessage> conversationHistory, String context) {
        long startTime = System.currentTimeMillis();
        try {
            log.debug("Starting Perplexity API call for user message: {}", userMessage.substring(0, Math.min(50, userMessage.length())));
            
            LLMProviderConfig.ProviderConfig providerConfig = config.getProviderConfig(PROVIDER_NAME);
            if (providerConfig == null) {
                throw new RuntimeException("Perplexity configuration not found");
            }
            
            String model = providerConfig.getModel();
            String apiKey = providerConfig.getApiKey();
            
            // Build the prompt using the base provider logic
            String prompt = buildPrompt(userMessage, conversationHistory, context);
            log.debug("Built prompt with {} characters", prompt.length());
            
            // Prepare headers
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + apiKey);
            headers.set("Content-Type", "application/json");
            headers.set("User-Agent", "HR-Chatbot/1.0");
            
            // Build request body
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", List.of(
                Map.of("role", "user", "content", prompt)
            ));
            
            // Add optimized parameters for performance
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("max_tokens", Math.min(providerConfig.getMaxTokens(), 500)); // Cap max tokens
            parameters.put("temperature", Math.min(providerConfig.getTemperature(), 0.7)); // Cap temperature
            parameters.put("top_p", 0.8); // Add top_p for better performance
            parameters.put("top_k", 40); // Add top_k for better performance
            parameters.put("stream", false); // Disable streaming for better performance
            requestBody.putAll(parameters);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            long apiStartTime = System.currentTimeMillis();
            log.debug("Sending request to Perplexity API: {}", API_URL);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                API_URL, 
                HttpMethod.POST, 
                request, 
                Map.class
            );
            
            long apiDuration = System.currentTimeMillis() - apiStartTime;
            long duration = System.currentTimeMillis() - startTime;
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                
                // Extract the response content
                String responseText = extractResponseText(responseBody);
                
                log.debug("Perplexity API HTTP call completed in {}ms", apiDuration);
                log.info("Perplexity API call completed in {}ms", duration);
                
                return createResponse(responseText, context);
            } else {
                throw new RuntimeException("Perplexity API returned error: " + response.getStatusCode());
            }
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Perplexity API call failed after {}ms: {}", duration, e.getMessage());
            return handleException(e, context);
        }
    }
    
    /**
     * Builds a complete prompt using the structured format
     */
    private String buildPrompt(String userMessage, List<ChatMessage> conversationHistory, String context) {
        String history = formatConversationHistory(conversationHistory);
        boolean hasContext = context != null && !context.trim().isEmpty();
        boolean hasHistory = history != null && !history.trim().isEmpty();
        
        // Truncate context if too long to improve performance
        String truncatedContext = context;
        if (hasContext && context.length() > 3000) {
            truncatedContext = context.substring(0, 3000) + "...\n[Context truncated for performance]";
            log.debug("Context truncated from {} to {} characters", context.length(), truncatedContext.length());
        }
        
        // Build prompt based on available context and history
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are an HR virtual assistant for the company. Your job is to provide employees with clear, accurate, ");
        prompt.append("and professional guidance on company policies, HR processes, and workplace-related questions. ");
        prompt.append("Always respond in a concise, supportive, and professional tone, making policies easy to understand ");
        prompt.append("without altering their meaning. Do not add unnecessary explanations or filler text. ");
        prompt.append("If query is not present or outside company scope only then explicitly state this in your answer. ");
        prompt.append("Always format such cases with a bold heading: **Not in Company Scope**. ");
        prompt.append("In this case, provide a very concise best-practice answer from general HR knowledge.");
        prompt.append("Structure responses for readability using short paragraphs or bullet points when appropriate.\n\n");
        
        if (hasContext) {
            prompt.append("CONTEXT:\n").append(truncatedContext).append("\n\n");
        }
        
        if (hasHistory) {
            prompt.append("HISTORY:\n").append(history).append("\n\n");
        }
        
        prompt.append("QUERY:\n").append(userMessage);
        
        if (!hasContext) {
            prompt.append("\n\nBegin with **Not in Company Scope**. Answer using general HR best practices.");
        }
        
        return prompt.toString();
    }
    
    /**
     * Formats conversation history into a readable string with token optimization
     */
    private String formatConversationHistory(List<ChatMessage> conversationHistory) {
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
     * Extracts the response text from Perplexity API response
     */
    private String extractResponseText(Map<String, Object> responseBody) {
        try {
            if (responseBody.containsKey("choices") && responseBody.get("choices") instanceof List) {
                List<?> choices = (List<?>) responseBody.get("choices");
                if (!choices.isEmpty() && choices.get(0) instanceof Map) {
                    Map<String, Object> firstChoice = (Map<String, Object>) choices.get(0);
                    if (firstChoice.containsKey("message") && firstChoice.get("message") instanceof Map) {
                        Map<String, Object> message = (Map<String, Object>) firstChoice.get("message");
                        if (message.containsKey("content")) {
                            return (String) message.get("content");
                        }
                    }
                }
            }
            throw new RuntimeException("Invalid response format from Perplexity API");
        } catch (Exception e) {
            log.error("Error extracting response from Perplexity API: {}", e.getMessage());
            throw new RuntimeException("Failed to extract response from Perplexity API", e);
        }
    }
    
    /**
     * Creates a successful response
     */
    private ChatResponse createResponse(String response, String context) {
        return ChatResponse.builder()
                .message(response)
                .timestamp(LocalDateTime.now())
                .success(true)
                .llmProviderUsed(PROVIDER_NAME)
                .contextUsed(String.valueOf(context != null && !context.trim().isEmpty()))
                .build();
    }
    
    /**
     * Handles exceptions and creates error response
     */
    private ChatResponse handleException(Exception e, String context) {
        log.error("Perplexity API error: {}", e.getMessage());
        return ChatResponse.builder()
                .message("I apologize, but I'm experiencing technical difficulties with the AI service. Please try again later.")
                .timestamp(LocalDateTime.now())
                .success(false)
                .error("Perplexity API error: " + e.getMessage())
                .llmProviderUsed(PROVIDER_NAME)
                .contextUsed(String.valueOf(context != null && !context.trim().isEmpty()))
                .build();
    }
}