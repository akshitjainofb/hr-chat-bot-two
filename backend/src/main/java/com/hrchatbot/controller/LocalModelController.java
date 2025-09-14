package com.hrchatbot.controller;

import com.hrchatbot.config.LocalModelConfig;
import com.hrchatbot.dto.ApiResponse;
import com.hrchatbot.service.provider.LocalHuggingFaceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for managing local Hugging Face models
 */
@RestController
@RequestMapping("/local-models")
@RequiredArgsConstructor
@Slf4j
public class LocalModelController {
    
    private final LocalHuggingFaceService localHuggingFaceService;
    private final LocalModelConfig localModelConfig;
    
    /**
     * Get information about available local models
     */
    @GetMapping("/info")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getModelInfo() {
        try {
            Map<String, Object> info = new HashMap<>();
            
            // Configured model
            info.put("configuredModel", localHuggingFaceService.getConfiguredModel());
            
            // Configuration
            Map<String, Object> config = new HashMap<>();
            config.put("cacheDir", localModelConfig.getCacheDirAbsolute());
            config.put("preferredSize", localModelConfig.getPreferredSize());
            config.put("maxModelsInMemory", localModelConfig.getMaxModelsInMemory());
            config.put("autoDownload", localModelConfig.isAutoDownload());
            config.put("useGpu", localModelConfig.isUseGpu());
            config.put("memoryLimitMB", localModelConfig.getMemoryLimitMB());
            config.put("cacheDirWritable", localModelConfig.isCacheDirWritable());
            info.put("configuration", config);
            
            // Service status
            info.put("serviceAvailable", localHuggingFaceService.isAvailable());
            info.put("memoryInfo", localHuggingFaceService.getMemoryInfo());
            
            return ResponseEntity.ok(ApiResponse.success("Local model information retrieved", info));
            
        } catch (Exception e) {
            log.error("Error getting local model info: {}", e.getMessage());
            return ResponseEntity.ok(ApiResponse.error("Failed to get model information: " + e.getMessage()));
        }
    }
    
    /**
     * Get memory usage information
     */
    @GetMapping("/memory")
    public ResponseEntity<ApiResponse<String>> getMemoryInfo() {
        try {
            String memoryInfo = localHuggingFaceService.getMemoryInfo();
            return ResponseEntity.ok(ApiResponse.success("Memory information retrieved", memoryInfo));
        } catch (Exception e) {
            log.error("Error getting memory info: {}", e.getMessage());
            return ResponseEntity.ok(ApiResponse.error("Failed to get memory information: " + e.getMessage()));
        }
    }
    
    /**
     * Check if local models are available
     */
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStatus() {
        try {
            Map<String, Object> status = new HashMap<>();
            status.put("available", localHuggingFaceService.isAvailable());
            status.put("providerName", localHuggingFaceService.getProviderName());
            status.put("memoryInfo", localHuggingFaceService.getMemoryInfo());
            
            return ResponseEntity.ok(ApiResponse.success("Local model status retrieved", status));
        } catch (Exception e) {
            log.error("Error getting local model status: {}", e.getMessage());
            return ResponseEntity.ok(ApiResponse.error("Failed to get status: " + e.getMessage()));
        }
    }
    
    /**
     * Test local model with a simple query
     */
    @PostMapping("/test")
    public ResponseEntity<ApiResponse<String>> testModel(@RequestBody Map<String, String> request) {
        try {
            String testMessage = request.getOrDefault("prompt", request.getOrDefault("message", "Hello, how are you?"));
            
            log.info("Testing local model with message: {}", testMessage);
            
            // Create a simple prompt for testing
            String prompt = "SYSTEM: You are a helpful HR assistant.\n\nUSER QUERY:\n" + testMessage + "\n\nINSTRUCTION: Please provide a helpful response.";
            
            // Test the enhanced local response
            String response = localHuggingFaceService.generateResponse(testMessage, null, null).getMessage();
            
            return ResponseEntity.ok(ApiResponse.success("Local model test completed", response));
            
        } catch (Exception e) {
            log.error("Error testing local model: {}", e.getMessage());
            return ResponseEntity.ok(ApiResponse.error("Failed to test model: " + e.getMessage()));
        }
    }
}
