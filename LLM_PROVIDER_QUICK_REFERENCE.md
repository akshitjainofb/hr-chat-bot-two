# LLM Provider Quick Reference

## Adding a New Provider

### 1. Create Provider Class
```java
@Service
@RequiredArgsConstructor
public class MyProviderService extends BaseLLMProvider {
    private final LLMProviderConfig config;
    private final RestTemplate restTemplate;

    @Override
    public ChatResponse generateResponse(String userMessage, List<ChatMessage> conversationHistory, String context) {
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

### 2. Add Configuration
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

### 3. Done! Provider is automatically available.

## Using Providers

### Get Provider
```java
LLMProvider provider = providerFactory.getProvider("openai");
LLMProvider best = providerFactory.getBestAvailableProvider();
```

### Check Availability
```java
boolean available = providerFactory.isProviderAvailable("gemini");
List<String> providers = providerFactory.getEnabledProviderNames();
```

### Generate Response
```java
ChatResponse response = provider.generateResponse(userMessage, history, context);
```

## Configuration Options

| Option | Description | Default |
|--------|-------------|---------|
| `api-key` | Provider API key | Required |
| `model` | Model name | Required |
| `enabled` | Enable/disable provider | `true` |
| `max-tokens` | Maximum response tokens | `1000` |
| `temperature` | Response creativity (0-1) | `0.7` |
| `timeout` | Request timeout (seconds) | `30` |

## Available Methods in BaseLLMProvider

| Method | Description |
|--------|-------------|
| `buildSystemPrompt(context)` | Build system prompt with context |
| `formatConversationHistory(messages)` | Format conversation history |
| `buildCompletePrompt(user, history, context)` | Build complete prompt |
| `createResponse(message, context)` | Create success response |
| `createErrorResponse(message, context)` | Create error response |
| `handleException(exception, context)` | Handle exceptions |

## Error Handling

### Error Response
1. Check if provider exists
2. Check if provider is available
3. Return specific error if provider fails

### Error Types
- Configuration errors (missing API key)
- API errors (network, rate limits)
- Response errors (parsing failures)

## Example: Complete Provider Implementation

```java
@Service
@RequiredArgsConstructor
public class ExampleProviderService extends BaseLLMProvider {
    private final LLMProviderConfig config;
    private final RestTemplate restTemplate;

    @Override
    public ChatResponse generateResponse(String userMessage, List<ChatMessage> conversationHistory, String context) {
        try {
            String apiKey = config.getApiKey("example");
            String model = config.getModel("example");
            
            if (apiKey == null || apiKey.trim().isEmpty()) {
                return createErrorResponse("Example API key not configured", context);
            }
            
            String prompt = buildCompletePrompt(userMessage, conversationHistory, context);
            String response = callExampleAPI(prompt, apiKey, model);
            
            return createResponse(response, context);
                    
        } catch (Exception e) {
            return handleException(e, context);
        }
    }
    
    private String callExampleAPI(String prompt, String apiKey, String model) {
        // Provider-specific API implementation
        // Use config.getMaxTokens("example"), config.getTemperature("example"), etc.
    }
    
    @Override
    public String getProviderName() {
        return "example";
    }
    
    @Override
    public boolean isAvailable() {
        String apiKey = config.getApiKey("example");
        return apiKey != null && !apiKey.trim().isEmpty() && config.isProviderEnabled("example");
    }
}
```

## Configuration Example

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
    example:
      api-key: ${EXAMPLE_API_KEY}
      model: example-model-v1
      enabled: true
      max-tokens: 500
      temperature: 0.5
```

## Environment Variables

```bash
OPENAI_API_KEY=sk-...
GEMINI_API_KEY=AIza...
HUGGINGFACE_API_KEY=hf_...
EXAMPLE_API_KEY=ex_...
```

## Testing

```java
@Test
public void testProvider() {
    LLMProvider provider = new ExampleProviderService(config, restTemplate);
    assertTrue(provider.isAvailable());
    assertEquals("example", provider.getProviderName());
}
```
