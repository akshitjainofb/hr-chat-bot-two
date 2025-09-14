package com.hrchatbot.service.impl;

import com.hrchatbot.dto.UserDto;
import com.hrchatbot.entity.User;
import com.hrchatbot.repository.UserRepository;
import com.hrchatbot.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;


    public UserDto getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return convertToDto(user);
    }

    @Transactional
    public User createOrUpdateUser(String email, String name, String googleId) {
        Optional<User> existingUser = userRepository.findByEmail(email);
        
        if (existingUser.isPresent()) {
            User user = existingUser.get();
            user.setName(name);
            user.setGoogleId(googleId);
            return userRepository.save(user);
        } else {
            User newUser = User.builder()
                    .email(email)
                    .name(name)
                    .password("") // Empty password for OAuth users
                    .googleId(googleId)
                    .preferredLlmProvider("openai")
                    .build();
            return userRepository.save(newUser);
        }
    }

    @Transactional
    public User createUserWithPassword(String email, String name, String hashedPassword) {
        User newUser = User.builder()
                .email(email)
                .name(name)
                .password(hashedPassword)
                .googleId(null)
                .preferredLlmProvider("openai")
                .build();
        return userRepository.save(newUser);
    }

    @Transactional
    public UserDto updateUserPreferences(String email, String preferredLlmProvider) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        user.setPreferredLlmProvider(preferredLlmProvider);
        user = userRepository.save(user);
        
        return convertToDto(user);
    }

    @Transactional
    public UserDto updateUserProfile(String email, String name) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        user.setName(name);
        user = userRepository.save(user);
        
        return convertToDto(user);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }
    
    private UserDto convertToDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .preferredLlmProvider(user.getPreferredLlmProvider())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
