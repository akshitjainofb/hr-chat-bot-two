package com.hrchatbot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for memory management.
 * Maps properties from application.yml to Java objects.
 */
@Configuration
@ConfigurationProperties(prefix = "memory")
@Data
public class MemoryConfig {
    
    /**
     * Maximum token limit for short-term memory sliding window
     */
    private int tokenLimit = 2000;
    
    /**
     * Maximum number of long-term memory items to retrieve from Pinecone
     */
    private int longTermMemoryLimit = 5;
    
    /**
     * Maximum number of document context items to retrieve from Pinecone
     */
    private int documentContextLimit = 5;
    
    /**
     * Batch size for archiving messages to long-term memory
     */
    private int archiveBatchSize = 20;
}
