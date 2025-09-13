package com.hrchatbot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * Configuration properties for LLM providers.
 * Maps provider-specific settings from application.yml.
 */
@Configuration
@ConfigurationProperties(prefix = "llm")
@Data
public class LLMProviderConfig {
    
    /**
     * Default provider name
     */
    private String defaultProvider = "openai";
    
    /**
     * Provider-specific configurations
     */
    private Map<String, ProviderConfig> providers;
    
    /**
     * Individual provider configuration
     */
    @Data
    public static class ProviderConfig {
        private String apiKey;
        private String model;
        private boolean enabled = true;
        private int maxTokens = 1000;
        private double temperature = 0.7;
        private int timeout = 30; // seconds
    }
    
    /**
     * Gets configuration for a specific provider
     * 
     * @param providerName The provider name
     * @return Provider configuration or null if not found
     */
    public ProviderConfig getProviderConfig(String providerName) {
        return providers != null ? providers.get(providerName) : null;
    }
    
    /**
     * Checks if a provider is enabled
     * 
     * @param providerName The provider name
     * @return True if the provider is enabled
     */
    public boolean isProviderEnabled(String providerName) {
        ProviderConfig config = getProviderConfig(providerName);
        return config != null && config.isEnabled();
    }
    
    /**
     * Gets the model for a specific provider
     * 
     * @param providerName The provider name
     * @return Model name or null if not configured
     */
    public String getModel(String providerName) {
        ProviderConfig config = getProviderConfig(providerName);
        return config != null ? config.getModel() : null;
    }
    
    /**
     * Gets the API key for a specific provider
     * 
     * @param providerName The provider name
     * @return API key or null if not configured
     */
    public String getApiKey(String providerName) {
        ProviderConfig config = getProviderConfig(providerName);
        return config != null ? config.getApiKey() : null;
    }
    
    /**
     * Gets the max tokens for a specific provider
     * 
     * @param providerName The provider name
     * @return Max tokens or default value
     */
    public int getMaxTokens(String providerName) {
        ProviderConfig config = getProviderConfig(providerName);
        return config != null ? config.getMaxTokens() : 1000;
    }
    
    /**
     * Gets the temperature for a specific provider
     * 
     * @param providerName The provider name
     * @return Temperature or default value
     */
    public double getTemperature(String providerName) {
        ProviderConfig config = getProviderConfig(providerName);
        return config != null ? config.getTemperature() : 0.7;
    }
    
    /**
     * Gets the timeout for a specific provider
     * 
     * @param providerName The provider name
     * @return Timeout in seconds or default value
     */
    public int getTimeout(String providerName) {
        ProviderConfig config = getProviderConfig(providerName);
        return config != null ? config.getTimeout() : 30;
    }
}
