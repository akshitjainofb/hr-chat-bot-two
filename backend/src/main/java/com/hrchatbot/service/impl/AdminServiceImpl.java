package com.hrchatbot.service.impl;

import com.hrchatbot.dto.UserManagementDto;
import com.hrchatbot.dto.UpdateUserRoleRequest;
import com.hrchatbot.entity.User;
import com.hrchatbot.entity.UserRole;
import com.hrchatbot.repository.UserRepository;
import com.hrchatbot.service.AdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminServiceImpl implements AdminService {
    
    private final UserRepository userRepository;
    
    @Override
    public List<UserManagementDto> getAllUsers(User adminUser) {
        // Verify admin access
        if (!isAdmin(adminUser)) {
            throw new RuntimeException("Unauthorized: Admin access required");
        }
        
        log.debug("Admin {} requested all users list", adminUser.getEmail());
        
        List<User> users = userRepository.findAll();
        
        return users.stream()
                .map(this::convertToUserManagementDto)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional
    public UserManagementDto updateUserRole(UpdateUserRoleRequest request, User adminUser) {
        // Verify admin access
        if (!isAdmin(adminUser)) {
            throw new RuntimeException("Unauthorized: Admin access required");
        }
        
        // Prevent admin from changing their own role
        if (adminUser.getId().equals(request.getUserId())) {
            throw new RuntimeException("Cannot change your own role");
        }
        
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        UserRole oldRole = user.getRole();
        user.setRole(request.getRole());
        user = userRepository.save(user);
        
        log.info("Admin {} changed user {} role from {} to {}", 
                adminUser.getEmail(), user.getEmail(), oldRole, request.getRole());
        
        return convertToUserManagementDto(user);
    }
    
    @Override
    public boolean isAdmin(User user) {
        return user != null && UserRole.ADMIN.equals(user.getRole());
    }
    
    private UserManagementDto convertToUserManagementDto(User user) {
        return UserManagementDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .chatRoomCount(user.getChatRooms() != null ? user.getChatRooms().size() : 0)
                .pdfDocumentCount(user.getPdfDocuments() != null ? user.getPdfDocuments().size() : 0)
                .build();
    }
}


