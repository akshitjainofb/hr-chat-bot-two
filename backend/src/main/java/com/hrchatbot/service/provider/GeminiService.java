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
        long startTime = System.currentTimeMillis();
        try {
            String apiKey = config.getApiKey("gemini");
            String model = config.getModel("gemini");
            
            if (apiKey == null || apiKey.trim().isEmpty()) {
                return createErrorResponse("Gemini API key not configured", context);
            }
            
            log.debug("Starting Gemini API call for user message: {}", userMessage.substring(0, Math.min(50, userMessage.length())));
            
            String prompt = buildCompletePrompt(userMessage, conversationHistory, context);
            log.debug("Built prompt with {} characters", prompt.length());
            
            String response = callGeminiAPI(prompt, apiKey, model);
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("Gemini API call completed in {}ms", duration);
            
            return createResponse(response, context);
                    
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Gemini API call failed after {}ms: {}", duration, e.getMessage());
            return handleException(e, context);
        }
    }
    
    private String callGeminiAPI(String prompt, String apiKey, String model) {
        long apiStartTime = System.currentTimeMillis();
        try {
            String url = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + apiKey;
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("User-Agent", "HR-Chatbot/1.0");
            
            // Optimize request payload - reduce token usage
            Map<String, Object> requestBody = new HashMap<>();
            Map<String, Object> content = new HashMap<>();
            Map<String, Object> part = new HashMap<>();
            part.put("text", prompt);
            content.put("parts", Arrays.asList(part));
            requestBody.put("contents", Arrays.asList(content));
            
            // Add generation config with optimized settings
            Map<String, Object> generationConfig = new HashMap<>();
            generationConfig.put("temperature", Math.min(config.getTemperature("gemini"), 0.7)); // Cap temperature for faster response
            generationConfig.put("maxOutputTokens", Math.min(config.getMaxTokens("gemini"), 500)); // Reduce max tokens for faster response
            generationConfig.put("topP", 0.8); // Add topP for better performance
            generationConfig.put("topK", 40); // Add topK for better performance
            requestBody.put("generationConfig", generationConfig);
            
            // Add safety settings to avoid content filtering delays
            Map<String, Object> safetySettings = new HashMap<>();
            safetySettings.put("category", "HARM_CATEGORY_HARASSMENT");
            safetySettings.put("threshold", "BLOCK_MEDIUM_AND_ABOVE");
            requestBody.put("safetySettings", Arrays.asList(safetySettings));
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            log.debug("Sending request to Gemini API: {}", url);
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            
            long apiDuration = System.currentTimeMillis() - apiStartTime;
            log.debug("Gemini API HTTP call completed in {}ms", apiDuration);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonObject jsonResponse = gson.fromJson(response.getBody(), JsonObject.class);
                
                if (jsonResponse.has("candidates") && jsonResponse.getAsJsonArray("candidates").size() > 0) {
                    JsonObject candidate = jsonResponse.getAsJsonArray("candidates").get(0).getAsJsonObject();
                    
                    // Check for finish reason to avoid blocked content
                    if (candidate.has("finishReason")) {
                        String finishReason = candidate.get("finishReason").getAsString();
                        if ("SAFETY".equals(finishReason)) {
                            log.warn("Gemini API blocked content due to safety filters");
                            return "I apologize, but I cannot provide a response to that query due to content safety guidelines.";
                        }
                    }
                    
                    if (candidate.has("content") && candidate.getAsJsonObject("content").has("parts")) {
                        JsonObject contentObj = candidate.getAsJsonObject("content");
                        if (contentObj.getAsJsonArray("parts").size() > 0) {
                            String responseText = contentObj.getAsJsonArray("parts").get(0).getAsJsonObject()
                                    .get("text").getAsString();
                            log.debug("Gemini API returned response with {} characters", responseText.length());
                            return responseText;
                        }
                    }
                }
                
                // Log the response for debugging
                log.warn("Unexpected Gemini API response structure: {}", response.getBody());
            } else {
                log.error("Gemini API returned non-2xx status: {} with body: {}", 
                         response.getStatusCode(), response.getBody());
            }
            
            return "I apologize, but I'm having trouble generating a response right now.";
            
        } catch (Exception e) {
            long apiDuration = System.currentTimeMillis() - apiStartTime;
            log.error("Error calling Gemini API after {}ms: {}", apiDuration, e.getMessage(), e);
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
