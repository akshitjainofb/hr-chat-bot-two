package com.hrchatbot.service.provider;

import com.hrchatbot.config.LLMProviderConfig;
import com.hrchatbot.dto.ChatResponse;
import com.hrchatbot.entity.ChatMessage;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.service.OpenAiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OpenAiProviderService extends BaseLLMProvider {

    private final LLMProviderConfig config;

    @Override
    public ChatResponse generateResponse(String userMessage, List<ChatMessage> conversationHistory, String context) {
        try {
            String apiKey = config.getApiKey("openai");
            String model = config.getModel("openai");
            
            if (apiKey == null || apiKey.trim().isEmpty()) {
                return createErrorResponse("OpenAI API key not configured", context);
            }
            
            OpenAiService service = new OpenAiService(apiKey);
            
            List<com.theokanning.openai.completion.chat.ChatMessage> messages = buildOpenAIMessages(
                userMessage, conversationHistory, context);
            
            ChatCompletionRequest request = ChatCompletionRequest.builder()
                    .model(model)
                    .messages(messages)
                    .maxTokens(config.getMaxTokens("openai"))
                    .temperature(config.getTemperature("openai"))
                    .build();
            
            String response = service.createChatCompletion(request)
                    .getChoices()
                    .get(0)
                    .getMessage()
                    .getContent();
            
            return createResponse(response, context);
                    
        } catch (Exception e) {
            return handleException(e, context);
        }
    }
    
    /**
     * Builds OpenAI-specific message format using the new structured prompts
     */
    private List<com.theokanning.openai.completion.chat.ChatMessage> buildOpenAIMessages(
            String userMessage, List<ChatMessage> conversationHistory, String context) {
        
        List<com.theokanning.openai.completion.chat.ChatMessage> messages = new ArrayList<>();
        
        // Use the new structured prompt format
        String structuredPrompt = buildCompletePrompt(userMessage, conversationHistory, context);
        
        // For OpenAI, we'll send the entire structured prompt as a single user message
        // This ensures the LLM gets the full context in the proper format
        messages.add(new com.theokanning.openai.completion.chat.ChatMessage("user", structuredPrompt));
        
        return messages;
    }
    
    @Override
    public String getProviderName() {
        return "openai";
    }
    
    @Override
    public boolean isAvailable() {
        String apiKey = config.getApiKey("openai");
        return apiKey != null && !apiKey.trim().isEmpty() && config.isProviderEnabled("openai");
    }
}
