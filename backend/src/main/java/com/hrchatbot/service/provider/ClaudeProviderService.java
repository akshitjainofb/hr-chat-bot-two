package com.hrchatbot.service.provider;

import com.hrchatbot.config.LLMProviderConfig;
import com.hrchatbot.dto.ChatResponse;
import com.hrchatbot.entity.ChatMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Example implementation of a new LLM provider using the generic architecture.
 * This demonstrates how easy it is to add new providers without code duplication.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ClaudeProviderService extends BaseLLMProvider {

    private final LLMProviderConfig config;
    private final RestTemplate restTemplate;

    @Override
    public ChatResponse generateResponse(String userMessage, List<ChatMessage> conversationHistory, String context) {
        try {
            String apiKey = config.getApiKey("claude");
            String model = config.getModel("claude");
            
            if (apiKey == null || apiKey.trim().isEmpty()) {
                return createErrorResponse("Claude API key not configured", context);
            }
            
            String prompt = buildCompletePrompt(userMessage, conversationHistory, context);
            String response = callClaudeAPI(prompt, apiKey, model);
            
            return createResponse(response, context);
                    
        } catch (Exception e) {
            return handleException(e, context);
        }
    }
    
    private String callClaudeAPI(String prompt, String apiKey, String model) {
        try {
            String url = "https://api.anthropic.com/v1/messages";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-api-key", apiKey);
            headers.set("anthropic-version", "2023-06-01");
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("max_tokens", config.getMaxTokens("claude"));
            requestBody.put("temperature", config.getTemperature("claude"));
            requestBody.put("messages", List.of(Map.of(
                "role", "user",
                "content", prompt
            )));
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                if (responseBody.containsKey("content")) {
                    List<Map<String, Object>> content = (List<Map<String, Object>>) responseBody.get("content");
                    if (!content.isEmpty() && content.get(0).containsKey("text")) {
                        return (String) content.get(0).get("text");
                    }
                }
            }
            
            return "I apologize, but I'm having trouble generating a response right now.";
            
        } catch (Exception e) {
            log.error("Error calling Claude API: {}", e.getMessage());
            return "I apologize, but I'm experiencing technical difficulties.";
        }
    }
    
    @Override
    public String getProviderName() {
        return "claude";
    }
    
    @Override
    public boolean isAvailable() {
        String apiKey = config.getApiKey("claude");
        return apiKey != null && !apiKey.trim().isEmpty() && config.isProviderEnabled("claude");
    }
}
