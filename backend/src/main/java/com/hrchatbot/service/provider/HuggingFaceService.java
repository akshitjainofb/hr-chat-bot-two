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
        long startTime = System.currentTimeMillis();
        try {
            log.debug("Starting HuggingFace API call for user message: {}", userMessage.substring(0, Math.min(50, userMessage.length())));
            
            String apiKey = config.getApiKey("huggingface");
            String model = config.getModel("huggingface");
            
            if (apiKey == null || apiKey.trim().isEmpty()) {
                return createErrorResponse("HuggingFace API key not configured", context);
            }
            
            String prompt = buildCompletePrompt(userMessage, conversationHistory, context);
            log.debug("Built prompt with {} characters", prompt.length());
            
            String response = callHuggingFaceAPI(prompt, apiKey, model);
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("HuggingFace API call completed in {}ms", duration);
            
            return createResponse(response, context);
                    
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("HuggingFace API call failed after {}ms: {}", duration, e.getMessage());
            return handleException(e, context);
        }
    }
    
    private String callHuggingFaceAPI(String prompt, String apiKey, String model) {
        long apiStartTime = System.currentTimeMillis();
        try {
            String url = "https://api-inference.huggingface.co/models/" + model;
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);
            headers.set("User-Agent", "HR-Chatbot/1.0");
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("inputs", prompt);
            
            // Optimized parameters for better performance
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("max_new_tokens", Math.min(config.getMaxTokens("huggingface"), 500)); // Cap max tokens
            parameters.put("temperature", Math.min(config.getTemperature("huggingface"), 0.7)); // Cap temperature
            parameters.put("top_p", 0.8); // Add top_p for better performance
            parameters.put("top_k", 40); // Add top_k for better performance
            parameters.put("return_full_text", false);
            parameters.put("do_sample", true);
            parameters.put("repetition_penalty", 1.1); // Prevent repetition
            
            requestBody.put("parameters", parameters);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            
            long apiDuration = System.currentTimeMillis() - apiStartTime;
            log.debug("HuggingFace API HTTP call completed in {}ms", apiDuration);
            
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
                log.debug("HuggingFace API returned response with {} characters", responseBody.length());
                return responseBody;
            }
            
            return "I apologize, but I'm having trouble generating a response right now.";
            
        } catch (Exception e) {
            long apiDuration = System.currentTimeMillis() - apiStartTime;
            log.error("Error calling Hugging Face API after {}ms: {}", apiDuration, e.getMessage());
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
