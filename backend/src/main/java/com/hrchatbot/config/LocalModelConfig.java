package com.hrchatbot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Configuration for local Hugging Face models
 */
@Configuration
@ConfigurationProperties(prefix = "local-models")
@Data
public class LocalModelConfig {
    
    /**
     * Directory where models will be cached locally
     */
    private String cacheDir = "./models";
    
    /**
     * Whether to download models automatically
     */
    private boolean autoDownload = true;
    
    /**
     * Maximum number of models to keep in memory
     */
    private int maxModelsInMemory = 2;
    
    /**
     * Model size preference: small, medium, large, best
     */
    private String preferredSize = "medium";
    
    /**
     * Whether to use GPU if available
     */
    private boolean useGpu = false;
    
    /**
     * Memory limit for model loading (in MB)
     */
    private long memoryLimitMB = 2048;
    
    @PostConstruct
    public void initialize() {
        try {
            // Create model cache directory if it doesn't exist
            Path cachePath = Paths.get(cacheDir);
            if (!Files.exists(cachePath)) {
                Files.createDirectories(cachePath);
                System.out.println("Created model cache directory: " + cachePath.toAbsolutePath());
            }
            
            // Set system properties for DJL
            System.setProperty("DJL_CACHE_DIR", cachePath.toAbsolutePath().toString());
            System.setProperty("DJL_PYTORCH_USE_MKLDNN", "false"); // Disable MKLDNN for better compatibility
            System.setProperty("DJL_PYTORCH_USE_MKL", "false");
            
            // Set model size preference
            System.setProperty("hf.model.size", preferredSize);
            
            System.out.println("Local model configuration initialized:");
            System.out.println("  Cache directory: " + cachePath.toAbsolutePath());
            System.out.println("  Preferred size: " + preferredSize);
            System.out.println("  Auto download: " + autoDownload);
            System.out.println("  Max models in memory: " + maxModelsInMemory);
            System.out.println("  Memory limit: " + memoryLimitMB + " MB");
            
        } catch (Exception e) {
            System.err.println("Failed to initialize local model configuration: " + e.getMessage());
        }
    }
    
    /**
     * Gets the absolute path to the cache directory
     */
    public String getCacheDirAbsolute() {
        return Paths.get(cacheDir).toAbsolutePath().toString();
    }
    
    /**
     * Checks if the cache directory is writable
     */
    public boolean isCacheDirWritable() {
        try {
            Path cachePath = Paths.get(cacheDir);
            return Files.exists(cachePath) && Files.isWritable(cachePath);
        } catch (Exception e) {
            return false;
        }
    }
}
