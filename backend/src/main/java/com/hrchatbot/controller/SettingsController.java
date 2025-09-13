package com.hrchatbot.controller;

import com.hrchatbot.dto.LlmProvidersResponse;
import com.hrchatbot.dto.UserDto;
import com.hrchatbot.service.impl.LlmServiceImpl;
import com.hrchatbot.service.impl.UserServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/settings")
@RequiredArgsConstructor
@Slf4j
public class SettingsController {

    private final UserServiceImpl userService;
    private final LlmServiceImpl llmService;

    @GetMapping("/llm-providers")
    public ResponseEntity<LlmProvidersResponse> getLlmProviders() {
        try {
            List<String> providers = llmService.getAvailableProviders();
            String defaultProvider = llmService.getDefaultProvider();
            
            return ResponseEntity.ok(LlmProvidersResponse.builder()
                .providers(providers)
                .defaultProvider(defaultProvider)
                .build());
        } catch (Exception e) {
            log.error("Error getting LLM providers: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/llm-provider")
    public ResponseEntity<UserDto> updateLlmProvider(@RequestBody Map<String, String> request) {
        try {
            String provider = request.get("provider");
            String userEmail = request.get("userEmail");
            
            if (provider == null || !llmService.isProviderAvailable(provider)) {
                return ResponseEntity.badRequest().build();
            }
            
            if (userEmail == null || userEmail.trim().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            
            UserDto user = userService.updateUserPreferences(userEmail, provider);
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            log.error("Error updating LLM provider: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/user")
    public ResponseEntity<UserDto> getUserSettings(@RequestParam String userEmail) {
        try {
            UserDto user = userService.getUserByEmail(userEmail);
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            log.error("Error getting user settings: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/user")
    public ResponseEntity<UserDto> updateUserProfile(@RequestBody Map<String, String> request) {
        try {
            String userEmail = request.get("userEmail");
            String name = request.get("name");
            
            if (userEmail == null || userEmail.trim().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            
            if (name == null || name.trim().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            
            UserDto user = userService.updateUserProfile(userEmail, name.trim());
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            log.error("Error updating user profile: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
}
