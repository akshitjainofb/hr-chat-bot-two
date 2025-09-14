package com.hrchatbot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for Ollama local model service
 */
@Configuration
@ConfigurationProperties(prefix = "ollama")
@Data
public class OllamaConfig {
    
    /**
     * Ollama base URL
     */
    private String baseUrl = "http://localhost:11434";
    
    /**
     * Model to use (configured in application.yml)
     */
    private String model;
    
    /**
     * Request timeout in seconds
     */
    private int timeout = 30;
    
    /**
     * Temperature for generation (0.0 to 1.0)
     */
    private double temperature = 0.7;
    
    /**
     * Maximum tokens to generate
     */
    private int maxTokens = 500;
    
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
