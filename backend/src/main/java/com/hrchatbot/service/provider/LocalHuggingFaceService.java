package com.hrchatbot.service.provider;

import com.hrchatbot.config.LLMProviderConfig;
import com.hrchatbot.config.OllamaConfig;
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

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Local Hugging Face service that runs models locally without API keys.
 * This is a simplified implementation that provides local model functionality.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LocalHuggingFaceService extends BaseLLMProvider {

    private final LLMProviderConfig config;
    private final OllamaConfig ollamaConfig;
    private final RestTemplate restTemplate;
    
    // Cache for model status
    private final Map<String, Boolean> modelStatus = new ConcurrentHashMap<>();

    @PostConstruct
    public void initialize() {
        log.info("Initializing Local Hugging Face Service...");
        log.info("Using Ollama model: {}", ollamaConfig.getModel());
        log.info("Ollama base URL: {}", ollamaConfig.getBaseUrl());
        log.info("Note: Make sure to run 'ollama pull {}' to download the model", ollamaConfig.getModel());
    }

    @Override
    public ChatResponse generateResponse(String userMessage, List<ChatMessage> conversationHistory, String context) {
        long startTime = System.currentTimeMillis();
        try {
            log.debug("Starting Local Hugging Face/Ollama API call for user message: {}", userMessage.substring(0, Math.min(50, userMessage.length())));
            
            String modelName = getModelName();
            String prompt = buildCompletePrompt(userMessage, conversationHistory, context);
            log.debug("Built prompt with {} characters", prompt.length());
            
            log.debug("Generating response with local model: {}", modelName);
            String response = generateLocalResponse(prompt, modelName);
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("Local Hugging Face/Ollama API call completed in {}ms", duration);
            
            return createResponse(response, context);
                    
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Local Hugging Face/Ollama API call failed after {}ms: {}", duration, e.getMessage());
            return handleException(e, context);
        }
    }
    
    /**
     * Generates a response using local model via Ollama
     * This uses Ollama for actual local model inference
     */
    private String generateLocalResponse(String prompt, String modelName) {
        log.info("Using local model via Ollama for: {}", modelName);
        
        try {
            // Try to use Ollama first
            String ollamaResponse = callOllamaAPI(prompt, modelName);
            if (ollamaResponse != null && !ollamaResponse.trim().isEmpty()) {
                return ollamaResponse;
            }
        } catch (Exception e) {
            log.warn("Ollama not available, falling back to enhanced local response: {}", e.getMessage());
        }
        
        // Fallback to enhanced local response if Ollama is not available
        return generateEnhancedLocalResponse(prompt, modelName);
    }
    
    /**
     * Calls Ollama API for local model inference
     */
    private String callOllamaAPI(String prompt, String modelName) {
        long apiStartTime = System.currentTimeMillis();
        try {
            String url = ollamaConfig.getBaseUrl() + "/api/generate";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("User-Agent", "HR-Chatbot/1.0");
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", modelName);
            requestBody.put("prompt", prompt);
            requestBody.put("stream", false);
            
            // Optimized options for better performance
            Map<String, Object> options = new HashMap<>();
            options.put("temperature", Math.min(ollamaConfig.getTemperature(), 0.7)); // Cap temperature
            options.put("top_p", 0.8); // Reduced from 0.9 for better performance
            options.put("top_k", 40); // Add top_k for better performance
            options.put("num_predict", Math.min(ollamaConfig.getMaxTokens(), 500)); // Cap max tokens
            options.put("repeat_penalty", 1.1); // Prevent repetition
            options.put("stop", new String[]{"</s>", "USER:", "Assistant:"}); // Stop tokens
            
            requestBody.put("options", options);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            
            long apiDuration = System.currentTimeMillis() - apiStartTime;
            log.debug("Ollama API HTTP call completed in {}ms", apiDuration);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                String responseText = (String) responseBody.get("response");
                if (responseText != null && !responseText.trim().isEmpty()) {
                    log.debug("Ollama API returned response with {} characters", responseText.length());
                    return responseText;
                }
            }
            
            return null;
        } catch (Exception e) {
            long apiDuration = System.currentTimeMillis() - apiStartTime;
            log.error("Error calling Ollama API after {}ms: {}", apiDuration, e.getMessage());
            return null;
        }
    }
    
    
    /**
     * Generates an enhanced local response with better context awareness
     */
    private String generateEnhancedLocalResponse(String prompt, String modelName) {
        log.info("Using enhanced local response for: {}", modelName);
        
        // Parse the prompt to extract the actual user message
        String userMessage = extractUserMessage(prompt);
        String lowerMessage = userMessage.toLowerCase();
        
        // Generate context-aware responses
        if (lowerMessage.contains("hr") || lowerMessage.contains("human resources")) {
            return "As your local HR assistant, I can help you with company policies, benefits, " +
                   "and workplace questions. Since I'm running locally, your data stays completely private. " +
                   "What specific HR topic would you like to discuss?";
        } else if (lowerMessage.contains("policy") || lowerMessage.contains("policies")) {
            return "I can help you understand company policies. Please note that I'm running locally " +
                   "and may not have access to the most current policy information. " +
                   "Could you tell me which specific policy you're asking about?";
        } else if (lowerMessage.contains("benefits") || lowerMessage.contains("benefit")) {
            return "I can provide general information about employee benefits. For specific details, " +
                   "I recommend contacting your HR department directly. " +
                   "What type of benefits are you interested in learning about?";
        } else if (lowerMessage.contains("leave") || lowerMessage.contains("vacation")) {
            return "I can help with general leave and vacation policies. For specific requests, " +
                   "please contact your HR department. " +
                   "Are you asking about sick leave, vacation time, or another type of leave?";
        } else if (lowerMessage.contains("salary") || lowerMessage.contains("pay")) {
            return "I can provide general information about compensation policies. For specific salary " +
                   "questions, I recommend speaking with your manager or HR department. " +
                   "What would you like to know about compensation?";
        } else if (lowerMessage.contains("training") || lowerMessage.contains("development")) {
            return "I can help you understand professional development opportunities and training programs. " +
                   "What type of training or development are you interested in?";
        } else if (lowerMessage.contains("workplace") || lowerMessage.contains("office")) {
            return "I can help with workplace policies and procedures. Since I'm running locally, " +
                   "your questions remain completely private. What workplace topic can I help you with?";
        } else {
            return "I'm here to help with your HR-related questions. Since I'm running locally, " +
                   "your conversations are completely private and secure. " +
                   "How can I assist you today?";
        }
    }
    
    /**
     * Extracts the actual user message from the formatted prompt
     */
    private String extractUserMessage(String prompt) {
        // Look for "USER QUERY:" in the prompt
        String userQueryMarker = "USER QUERY:\n";
        int userQueryIndex = prompt.indexOf(userQueryMarker);
        
        if (userQueryIndex != -1) {
            String userQuery = prompt.substring(userQueryIndex + userQueryMarker.length());
            // Remove any trailing instructions
            int instructionIndex = userQuery.indexOf("\n\nINSTRUCTION:");
            if (instructionIndex != -1) {
                userQuery = userQuery.substring(0, instructionIndex);
            }
            return userQuery.trim();
        }
        
        // Fallback: return the last line that doesn't contain system markers
        String[] lines = prompt.split("\n");
        for (int i = lines.length - 1; i >= 0; i--) {
            String line = lines[i].trim();
            if (!line.isEmpty() && 
                !line.startsWith("SYSTEM:") && 
                !line.startsWith("CONTEXT:") && 
                !line.startsWith("CONVERSATION HISTORY:") &&
                !line.startsWith("INSTRUCTION:")) {
                return line;
            }
        }
        
        return prompt; // Fallback to full prompt
    }
    
    /**
     * Gets the model name from Ollama configuration
     */
    private String getModelName() {
        return ollamaConfig.getModel();
    }
    
    /**
     * Gets the configured model name for external access
     */
    public String getConfiguredModel() {
        return ollamaConfig.getModel();
    }
    
    @Override
    public String getProviderName() {
        return "ollama";
    }
    
    @Override
    public boolean isAvailable() {
        try {
            // Check if the configured model is available
            String modelName = ollamaConfig.getModel();
            return modelStatus.computeIfAbsent(modelName, this::checkModelAvailability);
        } catch (Exception e) {
            log.warn("Error checking model availability: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Checks if a specific model is available in Ollama
     */
    private boolean checkModelAvailability(String modelName) {
        try {
            String url = ollamaConfig.getBaseUrl() + "/api/tags";
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                List<Map<String, Object>> models = (List<Map<String, Object>>) body.get("models");
                
                if (models != null) {
                    return models.stream()
                            .anyMatch(model -> modelName.equals(model.get("name")));
                }
            }
            return false;
        } catch (Exception e) {
            log.debug("Model {} not available: {}", modelName, e.getMessage());
            return false;
        }
    }
    
    
    /**
     * Gets memory usage information
     */
    public String getMemoryInfo() {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        long maxMemory = runtime.maxMemory();
        
        return String.format("Memory - Used: %d MB, Free: %d MB, Total: %d MB, Max: %d MB",
            usedMemory / (1024 * 1024),
            freeMemory / (1024 * 1024),
            totalMemory / (1024 * 1024),
            maxMemory / (1024 * 1024));
    }
}
