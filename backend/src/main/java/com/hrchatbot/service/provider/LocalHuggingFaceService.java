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
    
    // Default models for different use cases
    private static final Map<String, String> RECOMMENDED_MODELS = Map.of(
        "small", "distilbert-base-uncased", // Fast, lightweight
        "medium", "microsoft/DialoGPT-medium", // Good for conversations
        "large", "microsoft/DialoGPT-medium", // Same as medium for consistency
        "best", "microsoft/DialoGPT-medium" // Same as medium for consistency
    );

    @PostConstruct
    public void initialize() {
        log.info("Initializing Local Hugging Face Service...");
        log.info("Local models will be available for use");
        log.info("Note: This is a simplified implementation. For full local model support, additional setup is required.");
    }

    @Override
    public ChatResponse generateResponse(String userMessage, List<ChatMessage> conversationHistory, String context) {
        try {
            String modelName = getModelName();
            String prompt = buildCompletePrompt(userMessage, conversationHistory, context);
            
            log.debug("Generating response with local model: {}", modelName);
            String response = generateLocalResponse(prompt, modelName);
            
            return createResponse(response, context);
                    
        } catch (Exception e) {
            log.error("Error generating response with local Hugging Face model: {}", e.getMessage());
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
        try {
            String url = ollamaConfig.getBaseUrl() + "/api/generate";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", getOllamaModelName(modelName));
            requestBody.put("prompt", prompt);
            requestBody.put("stream", false);
            requestBody.put("options", Map.of(
                "temperature", ollamaConfig.getTemperature(),
                "top_p", 0.9,
                "num_predict", ollamaConfig.getMaxTokens()
            ));
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                return (String) responseBody.get("response");
            }
            
            return null;
        } catch (Exception e) {
            log.error("Error calling Ollama API: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Maps Hugging Face model names to Ollama model names
     */
    private String getOllamaModelName(String hfModelName) {
        // Map common Hugging Face models to Ollama models
        Map<String, String> modelMapping = Map.of(
            "microsoft/DialoGPT-medium", "llama2:7b",
            "microsoft/DialoGPT-large", "llama2:7b", 
            "microsoft/DialoGPT-xlarge", "llama2:7b",
            "distilbert-base-uncased", "llama2:7b"
        );
        
        return modelMapping.getOrDefault(hfModelName, "llama2:7b");
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
     * Gets the model name from configuration or uses a default
     */
    private String getModelName() {
        String configuredModel = config.getModel("local-huggingface");
        if (configuredModel != null && !configuredModel.trim().isEmpty()) {
            return configuredModel;
        }
        
        // Use a recommended model based on performance preference
        String modelSize = System.getProperty("hf.model.size", "medium");
        return RECOMMENDED_MODELS.getOrDefault(modelSize, RECOMMENDED_MODELS.get("medium"));
    }
    
    @Override
    public String getProviderName() {
        return "local-huggingface";
    }
    
    @Override
    public boolean isAvailable() {
        // For the simplified implementation, we're always available
        return true;
    }
    
    /**
     * Gets information about available models
     */
    public Map<String, String> getAvailableModels() {
        return Map.copyOf(RECOMMENDED_MODELS);
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
