package com.hrchatbot.service.provider;

import com.hrchatbot.config.LLMProviderConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Factory class for managing LLM providers.
 * Provides a centralized way to get and manage different LLM providers.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LLMProviderFactory {
    
    private final LLMProviderConfig config;
    private final List<LLMProvider> providers;
    private Map<String, LLMProvider> providerMap;
    
    /**
     * Initializes the provider map after all providers are injected
     */
    private void initializeProviderMap() {
        if (providerMap == null) {
            providerMap = providers.stream()
                    .collect(Collectors.toMap(
                            LLMProvider::getProviderName,
                            Function.identity()
                    ));
            log.info("Initialized LLM providers: {}", providerMap.keySet());
        }
    }
    
    /**
     * Gets a provider by name
     * 
     * @param providerName The provider name
     * @return LLMProvider instance or null if not found
     */
    public LLMProvider getProvider(String providerName) {
        initializeProviderMap();
        return providerMap.get(providerName.toLowerCase());
    }
    
    /**
     * Gets the default provider
     * 
     * @return Default LLMProvider instance
     */
    public LLMProvider getDefaultProvider() {
        return getProvider(config.getDefaultProvider());
    }
    
    /**
     * Gets all available providers
     * 
     * @return List of all LLMProvider instances
     */
    public List<LLMProvider> getAllProviders() {
        initializeProviderMap();
        return providers;
    }
    
    /**
     * Gets all enabled providers
     * 
     * @return List of enabled LLMProvider instances
     */
    public List<LLMProvider> getEnabledProviders() {
        initializeProviderMap();
        return providers.stream()
                .filter(provider -> config.isProviderEnabled(provider.getProviderName()))
                .collect(Collectors.toList());
    }
    
    /**
     * Gets provider names
     * 
     * @return List of provider names
     */
    public List<String> getProviderNames() {
        initializeProviderMap();
        return providers.stream()
                .map(LLMProvider::getProviderName)
                .collect(Collectors.toList());
    }
    
    /**
     * Gets enabled provider names
     * 
     * @return List of enabled provider names
     */
    public List<String> getEnabledProviderNames() {
        return getEnabledProviders().stream()
                .map(LLMProvider::getProviderName)
                .collect(Collectors.toList());
    }
    
    /**
     * Checks if a provider is available
     * 
     * @param providerName The provider name
     * @return True if the provider is available and enabled
     */
    public boolean isProviderAvailable(String providerName) {
        LLMProvider provider = getProvider(providerName);
        return provider != null && 
               config.isProviderEnabled(providerName) && 
               provider.isAvailable();
    }
    
}
