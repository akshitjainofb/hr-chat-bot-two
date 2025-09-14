package com.hrchatbot.controller;

import com.hrchatbot.dto.ApiResponse;
import com.hrchatbot.dto.UpdateUserRoleRequest;
import com.hrchatbot.dto.UserManagementDto;
import com.hrchatbot.entity.User;
import com.hrchatbot.exception.UserNotFoundException;
import com.hrchatbot.service.AdminService;
import com.hrchatbot.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {
    
    private final AdminService adminService;
    private final UserService userService;
    
    /**
     * Get all users for admin management
     */
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<UserManagementDto>>> getAllUsers(@RequestParam String userEmail) {
        try {
            User adminUser = userService.findByEmail(userEmail)
                    .orElseThrow(() -> new UserNotFoundException("User not found"));
            
            List<UserManagementDto> users = adminService.getAllUsers(adminUser);
            
            return ResponseEntity.ok(ApiResponse.success("Users retrieved successfully", users));
            
        } catch (Exception e) {
            log.error("Error retrieving users: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to retrieve users: " + e.getMessage()));
        }
    }
    
    /**
     * Update a user's role
     */
    @PutMapping("/users/role")
    public ResponseEntity<ApiResponse<UserManagementDto>> updateUserRole(
            @RequestBody UpdateUserRoleRequest request,
            @RequestParam String userEmail) {
        try {
            User adminUser = userService.findByEmail(userEmail)
                    .orElseThrow(() -> new UserNotFoundException("User not found"));
            
            UserManagementDto updatedUser = adminService.updateUserRole(request, adminUser);
            
            return ResponseEntity.ok(ApiResponse.success("User role updated successfully", updatedUser));
            
        } catch (Exception e) {
            log.error("Error updating user role: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to update user role: " + e.getMessage()));
        }
    }
    
    /**
     * Check if current user is admin
     */
    @GetMapping("/check-admin")
    public ResponseEntity<ApiResponse<Boolean>> checkAdmin(@RequestParam String userEmail) {
        try {
            User user = userService.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            boolean isAdmin = adminService.isAdmin(user);
            
            return ResponseEntity.ok(ApiResponse.success("Admin status checked", isAdmin));
            
        } catch (Exception e) {
            log.error("Error checking admin status: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to check admin status: " + e.getMessage()));
        }
    }
}
