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

@Service
@RequiredArgsConstructor
@Slf4j
public class HuggingFaceService extends BaseLLMProvider {

    private final LLMProviderConfig config;
    private final RestTemplate restTemplate;

    @Override
    public ChatResponse generateResponse(String userMessage, List<ChatMessage> conversationHistory, String context) {
        try {
            String apiKey = config.getApiKey("huggingface");
            String model = config.getModel("huggingface");
            
            if (apiKey == null || apiKey.trim().isEmpty()) {
                return createErrorResponse("HuggingFace API key not configured", context);
            }
            
            String prompt = buildCompletePrompt(userMessage, conversationHistory, context);
            String response = callHuggingFaceAPI(prompt, apiKey, model);
            
            return createResponse(response, context);
                    
        } catch (Exception e) {
            return handleException(e, context);
        }
    }
    
    private String callHuggingFaceAPI(String prompt, String apiKey, String model) {
        try {
            String url = "https://api-inference.huggingface.co/models/" + model;
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("inputs", prompt);
            requestBody.put("parameters", Map.of(
                "max_new_tokens", config.getMaxTokens("huggingface"),
                "temperature", config.getTemperature("huggingface"),
                "return_full_text", false
            ));
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            
            // Parse the response (simplified - in production, use proper JSON parsing)
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                // Extract text from the response array
                String responseBody = response.getBody();
                if (responseBody.startsWith("[") && responseBody.endsWith("]")) {
                    // Remove brackets and quotes
                    responseBody = responseBody.substring(1, responseBody.length() - 1);
                    if (responseBody.startsWith("\"") && responseBody.endsWith("\"")) {
                        responseBody = responseBody.substring(1, responseBody.length() - 1);
                    }
                }
                return responseBody;
            }
            
            return "I apologize, but I'm having trouble generating a response right now.";
            
        } catch (Exception e) {
            log.error("Error calling Hugging Face API: {}", e.getMessage());
            return "I apologize, but I'm experiencing technical difficulties.";
        }
    }
    
    @Override
    public String getProviderName() {
        return "huggingface";
    }
    
    @Override
    public boolean isAvailable() {
        String apiKey = config.getApiKey("huggingface");
        return apiKey != null && !apiKey.trim().isEmpty() && config.isProviderEnabled("huggingface");
    }
}
