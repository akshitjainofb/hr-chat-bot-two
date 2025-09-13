package com.hrchatbot.service.provider;

import com.hrchatbot.config.LLMProviderConfig;
import com.hrchatbot.dto.ChatResponse;
import com.hrchatbot.entity.ChatMessage;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiService extends BaseLLMProvider {

    private final LLMProviderConfig config;
    private final RestTemplate restTemplate;
    private final Gson gson = new Gson();

    @Override
    public ChatResponse generateResponse(String userMessage, List<ChatMessage> conversationHistory, String context) {
        try {
            String apiKey = config.getApiKey("gemini");
            String model = config.getModel("gemini");
            
            if (apiKey == null || apiKey.trim().isEmpty()) {
                return createErrorResponse("Gemini API key not configured", context);
            }
            
            String prompt = buildCompletePrompt(userMessage, conversationHistory, context);
            String response = callGeminiAPI(prompt, apiKey, model);
            
            return createResponse(response, context);
                    
        } catch (Exception e) {
            return handleException(e, context);
        }
    }
    
    private String callGeminiAPI(String prompt, String apiKey, String model) {
        try {
            String url = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + apiKey;
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            Map<String, Object> requestBody = new HashMap<>();
            Map<String, Object> content = new HashMap<>();
            Map<String, Object> part = new HashMap<>();
            part.put("text", prompt);
            content.put("parts", Arrays.asList(part));
            requestBody.put("contents", Arrays.asList(content));
            
            // Add generation config
            Map<String, Object> generationConfig = new HashMap<>();
            generationConfig.put("temperature", config.getTemperature("gemini"));
            generationConfig.put("maxOutputTokens", config.getMaxTokens("gemini"));
            requestBody.put("generationConfig", generationConfig);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonObject jsonResponse = gson.fromJson(response.getBody(), JsonObject.class);
                
                if (jsonResponse.has("candidates") && jsonResponse.getAsJsonArray("candidates").size() > 0) {
                    JsonObject candidate = jsonResponse.getAsJsonArray("candidates").get(0).getAsJsonObject();
                    if (candidate.has("content") && candidate.getAsJsonObject("content").has("parts")) {
                        JsonObject contentObj = candidate.getAsJsonObject("content");
                        if (contentObj.getAsJsonArray("parts").size() > 0) {
                            return contentObj.getAsJsonArray("parts").get(0).getAsJsonObject()
                                    .get("text").getAsString();
                        }
                    }
                }
            }
            
            return "I apologize, but I'm having trouble generating a response right now.";
            
        } catch (Exception e) {
            log.error("Error calling Gemini API: {}", e.getMessage());
            return "I apologize, but I'm experiencing technical difficulties.";
        }
    }
    
    @Override
    public String getProviderName() {
        return "gemini";
    }
    
    @Override
    public boolean isAvailable() {
        String apiKey = config.getApiKey("gemini");
        return apiKey != null && !apiKey.trim().isEmpty() && config.isProviderEnabled("gemini");
    }
}
