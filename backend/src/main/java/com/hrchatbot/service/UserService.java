package com.hrchatbot.service;

import com.hrchatbot.dto.UserDto;
import com.hrchatbot.entity.User;

import java.util.Optional;

public interface UserService {
    
    UserDto getUserByEmail(String email);
    
    User createOrUpdateUser(String email, String name, String googleId);
    
    User createUserWithPassword(String email, String name, String hashedPassword);
    
    UserDto updateUserPreferences(String email, String preferredLlmProvider);
    
    UserDto updateUserProfile(String email, String name);
    
    Optional<User> findByEmail(String email);
}


