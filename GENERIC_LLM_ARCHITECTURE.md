# Generic LLM Provider Architecture

## Overview

The HR Chatbot backend has been refactored to use a generic, extensible architecture for LLM providers. This eliminates code duplication, makes it easy to add new providers, and provides consistent error handling and configuration management.

## Architecture Components

### 1. Core Interfaces and Classes

#### `LLMProvider` Interface
```java
public interface LLMProvider {
    ChatResponse generateResponse(String userMessage, List<ChatMessage> conversationHistory, String context);
    String getProviderName();
    boolean isAvailable();
}
```

#### `BaseLLMProvider` Abstract Class
- **Purpose**: Provides common functionality and reduces code duplication
- **Key Features**:
  - Standardized system prompts
  - Conversation history formatting
  - Error handling utilities
  - Response creation helpers

#### `LLMProviderFactory` Class
- **Purpose**: Centralized provider management and discovery
- **Features**:
  - Automatic provider registration
  - Provider availability checking
  - Fallback provider selection
  - Configuration integration

### 2. Configuration Management

#### `LLMProviderConfig` Class
```yaml
llm:
  default: openai
  providers:
    openai:
      api-key: ${OPENAI_API_KEY}
      model: gpt-3.5-turbo
      enabled: true
      max-tokens: 1000
      temperature: 0.7
      timeout: 30
```

**Benefits**:
- Centralized configuration
- Environment variable support
- Per-provider settings
- Easy to modify without code changes

## Provider Implementations

### 1. OpenAI Provider (`OpenAiProviderService`)
- **Inherits**: `BaseLLMProvider`
- **Features**: Native OpenAI SDK integration
- **Configuration**: Uses `llm.providers.openai.*` settings

### 2. Gemini Provider (`GeminiService`)
- **Inherits**: `BaseLLMProvider`
- **Features**: REST API integration with Google Gemini
- **Configuration**: Uses `llm.providers.gemini.*` settings

### 3. HuggingFace Provider (`HuggingFaceService`)
- **Inherits**: `BaseLLMProvider`
- **Features**: HuggingFace Inference API integration
- **Configuration**: Uses `llm.providers.huggingface.*` settings

### 4. Example: Claude Provider (`ClaudeProviderService`)
- **Inherits**: `BaseLLMProvider`
- **Features**: Anthropic Claude API integration
- **Configuration**: Uses `llm.providers.claude.*` settings

## Key Benefits

### 1. Code Reuse
- **Before**: Each provider duplicated prompt building, error handling, and response creation
- **After**: Common functionality in `BaseLLMProvider`, providers focus on API-specific logic

### 2. Easy Provider Addition
```java
@Service
@RequiredArgsConstructor
public class NewProviderService extends BaseLLMProvider {
    // Only need to implement:
    // 1. generateResponse() - API-specific logic
    // 2. getProviderName() - return provider name
    // 3. isAvailable() - check availability
    // 4. API call method - provider-specific implementation
}
```

### 3. Consistent Error Handling
- Standardized error responses
- Automatic fallback to available providers
- Comprehensive logging

### 4. Configuration Management
- Centralized provider settings
- Environment variable support
- Per-provider customization

## Usage Examples

### 1. Adding a New Provider

#### Step 1: Create Provider Class
```java
@Service
@RequiredArgsConstructor
public class MyProviderService extends BaseLLMProvider {
    private final LLMProviderConfig config;
    private final RestTemplate restTemplate;

    @Override
    public ChatResponse generateResponse(String userMessage, List<ChatMessage> conversationHistory, String context) {
        // Use inherited methods:
        String prompt = buildCompletePrompt(userMessage, conversationHistory, context);
        String response = callMyAPI(prompt);
        return createResponse(response, context);
    }

    @Override
    public String getProviderName() {
        return "myprovider";
    }

    @Override
    public boolean isAvailable() {
        return config.isProviderEnabled("myprovider") && 
               config.getApiKey("myprovider") != null;
    }
}
```

#### Step 2: Add Configuration
```yaml
llm:
  providers:
    myprovider:
      api-key: ${MYPROVIDER_API_KEY}
      model: my-model-v1
      enabled: true
      max-tokens: 1000
      temperature: 0.7
```

#### Step 3: Done!
The provider is automatically discovered and available for use.

### 2. Using the Generic Service

```java
@Service
public class MyService {
    private final LLMProviderFactory providerFactory;
    
    public void useProvider() {
        // Get specific provider
        LLMProvider provider = providerFactory.getProvider("openai");
        
        // Get best available provider
        LLMProvider best = providerFactory.getBestAvailableProvider();
        
        // Check availability
        boolean available = providerFactory.isProviderAvailable("gemini");
        
        // Get all enabled providers
        List<String> providers = providerFactory.getEnabledProviderNames();
    }
}
```

## Migration from Old Architecture

### Before (Duplicated Code)
```java
// Each provider had its own prompt building
StringBuilder prompt = new StringBuilder();
if (context != null && !context.isEmpty()) {
    prompt.append("You are an HR assistant...");
    // ... more duplicated code
}

// Each provider had its own error handling
try {
    // API call
} catch (Exception e) {
    log.error("Error: {}", e.getMessage());
    return ChatResponse.builder()
        .message("I apologize...")
        .build();
}
```

### After (Generic Architecture)
```java
// Inherited from BaseLLMProvider
String prompt = buildCompletePrompt(userMessage, conversationHistory, context);
ChatResponse response = callAPI(prompt);
return createResponse(response, context);
```

## Configuration Examples

### Environment Variables
```bash
# OpenAI
OPENAI_API_KEY=sk-...
OPENAI_MODEL=gpt-4

# Gemini
GEMINI_API_KEY=AIza...
GEMINI_MODEL=gemini-pro

# HuggingFace
HUGGINGFACE_API_KEY=hf_...
HUGGINGFACE_MODEL=microsoft/DialoGPT-medium
```

### Application Properties
```yaml
llm:
  default: openai
  providers:
    openai:
      api-key: ${OPENAI_API_KEY}
      model: ${OPENAI_MODEL:gpt-3.5-turbo}
      enabled: true
      max-tokens: 1000
      temperature: 0.7
    gemini:
      api-key: ${GEMINI_API_KEY}
      model: ${GEMINI_MODEL:gemini-pro}
      enabled: true
      max-tokens: 1000
      temperature: 0.7
```

## Error Handling and Fallbacks

### Error Handling
1. **Provider Selection**: User's preferred provider or default
2. **Availability Check**: Verify provider is available before use
3. **Error Response**: Return error if provider fails or is unavailable

### Error Types
- **Configuration Errors**: Missing API keys, invalid models
- **API Errors**: Network issues, rate limits, service unavailable
- **Response Errors**: Invalid responses, parsing failures

### Logging
```java
log.debug("Using provider: {}", provider.getProviderName());
log.warn("Provider {} not available, trying fallback", selectedProvider);
log.error("All providers failed: {}", error.getMessage());
```

## Performance Considerations

### Provider Selection
- **Fast Lookup**: O(1) provider retrieval by name
- **Lazy Loading**: Providers initialized on first use
- **Caching**: Configuration cached after first access

### Error Recovery
- **Immediate Error**: Return error immediately on provider failure
- **Clear Error Messages**: Specific error messages for different failure types
- **Timeout Handling**: Configurable timeouts per provider

## Testing

### Unit Testing
```java
@Test
public void testProvider() {
    LLMProvider provider = new MyProviderService(config, restTemplate);
    assertTrue(provider.isAvailable());
    assertEquals("myprovider", provider.getProviderName());
}
```

### Integration Testing
```java
@Test
public void testProviderFactory() {
    LLMProvider provider = providerFactory.getProvider("openai");
    assertNotNull(provider);
    assertTrue(provider.isAvailable());
}
```

## Best Practices

### 1. Provider Implementation
- Extend `BaseLLMProvider` for common functionality
- Implement only provider-specific logic
- Use configuration for all settings
- Handle errors gracefully

### 2. Configuration
- Use environment variables for sensitive data
- Provide sensible defaults
- Document all configuration options
- Validate configuration on startup

### 3. Error Handling
- Log errors with context
- Provide meaningful error messages
- Implement proper fallback mechanisms
- Monitor provider health

### 4. Testing
- Test each provider individually
- Test provider factory functionality
- Test error scenarios
- Test configuration validation

## Future Enhancements

### Potential Improvements
1. **Provider Health Monitoring**: Track provider performance and availability
2. **Load Balancing**: Distribute requests across multiple providers
3. **Caching**: Cache responses for repeated queries
4. **Metrics**: Detailed performance and usage metrics
5. **Provider Plugins**: Dynamic provider loading

### Extensibility
- **Custom Providers**: Easy to add new providers
- **Provider Chains**: Multiple providers for different use cases
- **A/B Testing**: Compare provider performance
- **Cost Optimization**: Route to most cost-effective provider

## Conclusion

The generic LLM provider architecture provides:

✅ **Eliminated Code Duplication**: Common functionality in base classes  
✅ **Easy Provider Addition**: Simple interface for new providers  
✅ **Consistent Error Handling**: Standardized error management  
✅ **Centralized Configuration**: Single place for all settings  
✅ **Automatic Discovery**: Providers registered automatically  
✅ **Clear Error Handling**: Immediate error responses on failure  
✅ **Production Ready**: Comprehensive logging and monitoring  

This architecture makes the HR Chatbot highly extensible and maintainable while providing excellent performance and reliability.
