package com.hrchatbot.service;

import com.hrchatbot.entity.ChatMessage;
import com.hrchatbot.entity.User;
import com.hrchatbot.service.impl.LlmServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
    "llm.providers.openai.api-key=test-key",
    "llm.providers.gemini.api-key=test-key",
    "llm.providers.huggingface.api-key=test-key"
})
public class LlmServiceTest {

    @Autowired
    private LlmService llmService;

    @Test
    public void testGetAvailableProviders() {
        List<String> providers = llmService.getAvailableProviders();
        assertNotNull(providers);
        assertTrue(providers.contains("openai"));
        assertTrue(providers.contains("gemini"));
        assertTrue(providers.contains("huggingface"));
    }

    @Test
    public void testGetDefaultProvider() {
        String defaultProvider = llmService.getDefaultProvider();
        assertNotNull(defaultProvider);
        assertEquals("openai", defaultProvider);
    }

    @Test
    public void testIsProviderAvailable() {
        assertTrue(llmService.isProviderAvailable("openai"));
        assertTrue(llmService.isProviderAvailable("gemini"));
        assertTrue(llmService.isProviderAvailable("huggingface"));
        assertFalse(llmService.isProviderAvailable("invalid-provider"));
    }
}
