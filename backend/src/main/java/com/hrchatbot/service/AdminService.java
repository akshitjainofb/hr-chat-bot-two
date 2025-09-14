package com.hrchatbot.service;

import com.hrchatbot.dto.UserManagementDto;
import com.hrchatbot.dto.UpdateUserRoleRequest;
import com.hrchatbot.entity.User;

import java.util.List;

public interface AdminService {
    
    /**
     * Get all users for admin management
     * 
     * @param adminUser The admin user making the request
     * @return List of user management DTOs
     */
    List<UserManagementDto> getAllUsers(User adminUser);
    
    /**
     * Update a user's role
     * 
     * @param request The role update request
     * @param adminUser The admin user making the request
     * @return Updated user management DTO
     */
    UserManagementDto updateUserRole(UpdateUserRoleRequest request, User adminUser);
    
    /**
     * Check if a user is an admin
     * 
     * @param user The user to check
     * @return True if user is admin
     */
    boolean isAdmin(User user);
}


