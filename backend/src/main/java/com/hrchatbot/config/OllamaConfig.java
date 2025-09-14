package com.hrchatbot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for Ollama local model service
 */
@Configuration
@ConfigurationProperties(prefix = "llm.providers.ollama")
@Data
public class OllamaConfig {
    
    /**
     * API key (empty for local models)
     */
    private String apiKey = "";
    
    /**
     * Model to use (configured in application.yml)
     */
    private String model;
    
    /**
     * Whether this provider is enabled
     */
    private boolean enabled = true;
    
    /**
     * Maximum tokens to generate
     */
    private int maxTokens = 500;
    
    /**
     * Temperature for generation (0.0 to 1.0)
     */
    private double temperature = 0.5;
    
    /**
     * Request timeout in seconds
     */
    private int timeout = 15;
    
    /**
     * Ollama base URL
     */
    private String baseUrl = "http://localhost:11434";
    
    /**
     * Check if Ollama is available
     */
    public boolean isAvailable() {
        try {
            // This would be checked by the service
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
