package com.hrchatbot.controller;

import com.hrchatbot.dto.AuthResponse;
import com.hrchatbot.dto.UserDto;
import com.hrchatbot.entity.User;
import com.hrchatbot.service.impl.UserServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final UserServiceImpl userService;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/test")
    public ResponseEntity<Map<String, String>> test() {
        return ResponseEntity.ok(Map.of("message", "Auth endpoint is working"));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody Map<String, String> loginRequest) {
        try {
            String email = loginRequest.get("email");
            String password = loginRequest.get("password");
            
            if (email == null || email.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(AuthResponse.builder()
                    .success(false)
                    .error("Email is required")
                    .build());
            }
            
            if (password == null || password.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(AuthResponse.builder()
                    .success(false)
                    .error("Password is required")
                    .build());
            }
            
            // Check if user exists
            var existingUser = userService.findByEmail(email);
            if (existingUser.isPresent()) {
                User user = existingUser.get();
                String storedPassword = user.getPassword();
                
                // Check if password is hashed (starts with $2a$) or plain text
                boolean passwordMatches = false;
                if (storedPassword != null && storedPassword.startsWith("$2a$")) {
                    // Password is hashed, use BCrypt
                    passwordMatches = passwordEncoder.matches(password, storedPassword);
                } else {
                    // Password is plain text, compare directly
                    passwordMatches = password.equals(storedPassword);
                }
                
                if (passwordMatches) {
                    UserDto userDto = userService.getUserByEmail(email);
                    return ResponseEntity.ok(AuthResponse.builder()
                        .success(true)
                        .user(userDto)
                        .message("Login successful")
                        .build());
                } else {
                    return ResponseEntity.badRequest().body(AuthResponse.builder()
                        .success(false)
                        .error("Invalid email or password")
                        .build());
                }
            } else {
                // User doesn't exist - require signup
                return ResponseEntity.badRequest().body(AuthResponse.builder()
                    .success(false)
                    .error("Account not found. Please sign up first.")
                    .build());
            }
            
        } catch (Exception e) {
            log.error("Login error: ", e);
            return ResponseEntity.ok(AuthResponse.builder()
                .success(false)
                .error("Login failed")
                .build());
        }
    }

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@RequestBody Map<String, String> signupRequest) {
        try {
            String email = signupRequest.get("email");
            String password = signupRequest.get("password");
            String name = signupRequest.get("name");
            
            if (email == null || email.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(AuthResponse.builder()
                    .success(false)
                    .error("Email is required")
                    .build());
            }
            
            if (password == null || password.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(AuthResponse.builder()
                    .success(false)
                    .error("Password is required")
                    .build());
            }
            
            if (name == null || name.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(AuthResponse.builder()
                    .success(false)
                    .error("Name is required")
                    .build());
            }
            
            // Check if user already exists
            if (userService.findByEmail(email).isPresent()) {
                return ResponseEntity.badRequest().body(AuthResponse.builder()
                    .success(false)
                    .error("User with this email already exists")
                    .build());
            }
            
            // Create new user with hashed password
            String hashedPassword = passwordEncoder.encode(password);
            userService.createUserWithPassword(email, name, hashedPassword);
            
            UserDto userDto = userService.getUserByEmail(email);
            return ResponseEntity.ok(AuthResponse.builder()
                .success(true)
                .user(userDto)
                .message("Account created successfully")
                .build());
            
        } catch (Exception e) {
            log.error("Signup error: ", e);
            return ResponseEntity.ok(AuthResponse.builder()
                .success(false)
                .error("Signup failed")
                .build());
        }
    }

    @GetMapping("/user/{email}")
    public ResponseEntity<UserDto> getUser(@PathVariable String email) {
        try {
            UserDto user = userService.getUserByEmail(email);
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            log.error("Error getting user: ", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout() {
        return ResponseEntity.ok(Map.of(
            "message", "Logout successful"
        ));
    }
}
