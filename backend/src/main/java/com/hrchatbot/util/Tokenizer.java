package com.hrchatbot.util;

import com.hrchatbot.entity.ChatMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Utility class for estimating token counts of text.
 * This provides a basic approximation and can be extended with more sophisticated tokenizers
 * like OpenAI's tiktoken or specific LLM provider tokenizers if needed.
 */
@Component
@Slf4j
public class Tokenizer {

    /**
     * Estimates the token count for a given text.
     * This is a simple approximation (e.g., word count or character count divided by a factor).
     * For more accurate results, integrate a specific LLM's tokenizer.
     *
     * @param text The text to count tokens for.
     * @return An estimated number of tokens.
     */
    public int countTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        // Simple approximation: count words. Average word length is ~4-5 characters, plus spaces.
        // A common rule of thumb is 1 token ~ 4 characters for English text.
        // So, word count is a reasonable proxy.
        String[] words = text.trim().split("\\s+");
        return words.length;
    }

    /**
     * Estimates the total token count for a list of chat messages.
     *
     * @param messages The list of ChatMessage objects.
     * @return The total estimated token count.
     */
    public int countTokens(List<ChatMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return 0;
        }
        return messages.stream()
                .mapToInt(msg -> countTokens(msg.getMessage()))
                .sum();
    }
}
