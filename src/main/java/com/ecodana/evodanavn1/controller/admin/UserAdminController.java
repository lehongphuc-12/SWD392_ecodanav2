package com.ecodana.evodanavn1.controller.admin;

import com.ecodana.evodanavn1.dto.UserRequest;
import com.ecodana.evodanavn1.dto.UserResponse;
import com.ecodana.evodanavn1.model.Role;
import com.ecodana.evodanavn1.model.User;
import com.ecodana.evodanavn1.repository.RoleRepository;
import com.ecodana.evodanavn1.service.RoleService;
import com.ecodana.evodanavn1.service.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Controller for User Management in Admin Panel
 * Provides both REST API endpoints and page rendering
 */
@Controller
@RequestMapping("/admin/users")
public class UserAdminController {
    
    private static final Logger logger = LoggerFactory.getLogger(UserAdminController.class);
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private RoleService roleService;
    
    @Autowired
    private RoleRepository roleRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private com.ecodana.evodanavn1.service.EmailService emailService;
    
    /**
     * Display user management page
     */
    @GetMapping
    public String getUserManagementPage(HttpSession session, Model model) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null || !userService.isAdmin(currentUser)) {
            return "redirect:/login";
        }
        
        try {
            List<User> users = userService.getAllUsersWithRole();
            List<Role> roles = roleRepository.findAll();
            
            model.addAttribute("currentUser", currentUser);
            model.addAttribute("users", users);
            model.addAttribute("roles", roles);
            model.addAttribute("totalUsers", users.size());
            model.addAttribute("activeUsers", users.stream().filter(u -> u.getStatus() == User.UserStatus.Active).count());
            model.addAttribute("inactiveUsers", users.stream().filter(u -> u.getStatus() == User.UserStatus.Inactive).count());
            model.addAttribute("bannedUsers", users.stream().filter(u -> u.getStatus() == User.UserStatus.Banned).count());
            
            // Count by role
            model.addAttribute("adminCount", users.stream().filter(u -> u.getRole() != null && "Admin".equals(u.getRole().getRoleName())).count());
            model.addAttribute("ownerCount", users.stream().filter(u -> u.getRole() != null && "Owner".equals(u.getRole().getRoleName())).count());
            model.addAttribute("customerCount", users.stream().filter(u -> u.getRole() != null && "Customer".equals(u.getRole().getRoleName())).count());
            
            return "admin/user-management";
        } catch (Exception e) {
            logger.error("Error loading user management page", e);
            model.addAttribute("error", "Error loading users: " + e.getMessage());
            model.addAttribute("totalUsers", 0);
            model.addAttribute("activeUsers", 0);
            model.addAttribute("inactiveUsers", 0);
            model.addAttribute("bannedUsers", 0);
            model.addAttribute("adminCount", 0);
            model.addAttribute("ownerCount", 0);
            model.addAttribute("customerCount", 0);
            model.addAttribute("users", List.of());
            model.addAttribute("roles", List.of());
            return "admin/user-management";
        }
    }
    
    /**
     * Display user detail page
     */
    @GetMapping("/detail/{id}")
    public String getUserDetailPage(@PathVariable String id, HttpSession session, Model model) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null || !userService.isAdmin(currentUser)) {
            return "redirect:/login";
        }
        
        try {
            User user = userService.findByIdWithRole(id);
            if (user == null) {
                model.addAttribute("error", "User not found");
                return "redirect:/admin/users";
            }
            
            model.addAttribute("currentUser", currentUser);
            model.addAttribute("user", user);
            
            return "admin/user-detail";
        } catch (Exception e) {
            logger.error("Error loading user detail page", e);
            model.addAttribute("error", "Error loading user: " + e.getMessage());
            return "redirect:/admin/users";
        }
    }
    
    /**
     * Display user edit page
     */
    @GetMapping("/edit/{id}")
    public String getUserEditPage(@PathVariable String id, HttpSession session, Model model) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null || !userService.isAdmin(currentUser)) {
            return "redirect:/login";
        }
        
        try {
            User user = userService.findByIdWithRole(id);
            if (user == null) {
                model.addAttribute("error", "User not found");
                return "redirect:/admin/users";
            }
            
            List<Role> roles = roleRepository.findAll();
            
            model.addAttribute("currentUser", currentUser);
            model.addAttribute("user", user);
            model.addAttribute("roles", roles);
            
            return "admin/user-edit";
        } catch (Exception e) {
            logger.error("Error loading user edit page", e);
            model.addAttribute("error", "Error loading user: " + e.getMessage());
            return "redirect:/admin/users";
        }
    }
    
    /**
     * Get all users (API endpoint)
     */
    @GetMapping("/api/list")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getAllUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String role,
            HttpSession session) {
        
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null || !userService.isAdmin(currentUser)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("success", false, "message", "Unauthorized access"));
        }
        
        try {
            List<User> users = userService.getAllUsersWithRole();
            
            // Apply filters
            if (search != null && !search.trim().isEmpty()) {
                String searchLower = search.toLowerCase();
                users = users.stream()
                        .filter(u -> u.getUsername().toLowerCase().contains(searchLower) ||
                                u.getEmail().toLowerCase().contains(searchLower) ||
                                (u.getFirstName() != null && u.getFirstName().toLowerCase().contains(searchLower)) ||
                                (u.getLastName() != null && u.getLastName().toLowerCase().contains(searchLower)))
                        .collect(Collectors.toList());
            }
            
            if (status != null && !status.trim().isEmpty() && !"all".equalsIgnoreCase(status)) {
                users = users.stream()
                        .filter(u -> u.getStatus() != null && status.equalsIgnoreCase(u.getStatus().name()))
                        .collect(Collectors.toList());
            }
            
            if (role != null && !role.trim().isEmpty() && !"all".equalsIgnoreCase(role)) {
                users = users.stream()
                        .filter(u -> u.getRole() != null && role.equalsIgnoreCase(u.getRole().getRoleName()))
                        .collect(Collectors.toList());
            }
            
            List<UserResponse> userResponses = users.stream()
                    .map(UserResponse::new)
                    .collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("users", userResponses);
            response.put("total", userResponses.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error fetching users", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Error fetching users: " + e.getMessage()));
        }
    }
    
    /**
     * Get user by ID (API endpoint)
     */
    @GetMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getUserById(@PathVariable String id, HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null || !userService.isAdmin(currentUser)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("success", false, "message", "Unauthorized access"));
        }
        
        try {
            User user = userService.findByIdWithRole(id);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("success", false, "message", "User not found"));
            }
            
            UserResponse userResponse = new UserResponse(user);
            return ResponseEntity.ok(Map.of("success", true, "user", userResponse));
        } catch (Exception e) {
            logger.error("Error fetching user", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Error fetching user: " + e.getMessage()));
        }
    }
    
    /**
     * Create new user (API endpoint)
     */
    @PostMapping("/api/create")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createUser(
            @Valid @RequestBody UserRequest userRequest,
            BindingResult bindingResult,
            HttpSession session) {
        
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null || !userService.isAdmin(currentUser)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("success", false, "message", "Unauthorized access"));
        }
        
        if (bindingResult.hasErrors()) {
            List<String> errors = bindingResult.getAllErrors().stream()
                    .map(error -> error.getDefaultMessage())
                    .collect(Collectors.toList());
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Validation failed", "errors", errors));
        }
        
        try {
            // Check if username exists
            if (userService.existsByUsername(userRequest.getUsername())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Username already exists"));
            }
            
            // Check if email exists
            if (userService.existsByEmail(userRequest.getEmail())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Email already exists"));
            }
            
            // Validate role
            if (!roleService.isValidRoleId(userRequest.getRoleId())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Invalid role ID"));
            }
            
            // Create new user
            User newUser = new User();
            newUser.setId(UUID.randomUUID().toString());
            newUser.setUsername(userRequest.getUsername());
            newUser.setFirstName(userRequest.getFirstName());
            newUser.setLastName(userRequest.getLastName());
            newUser.setUserDOB(userRequest.getUserDOB());
            newUser.setPhoneNumber(userRequest.getPhoneNumber());
            newUser.setAvatarUrl(userRequest.getAvatarUrl());
            if (userRequest.getGender() != null) {
                newUser.setGender(User.Gender.valueOf(userRequest.getGender()));
            }
            newUser.setStatus(User.UserStatus.valueOf(userRequest.getStatus()));
            newUser.setRoleId(userRequest.getRoleId());
            newUser.setEmail(userRequest.getEmail());
            newUser.setEmailVerifed(userRequest.getEmailVerified() != null && userRequest.getEmailVerified());
            newUser.setTwoFactorEnabled(userRequest.getTwoFactorEnabled() != null && userRequest.getTwoFactorEnabled());
            newUser.setLockoutEnabled(userRequest.getLockoutEnabled() != null && userRequest.getLockoutEnabled());
            newUser.setAccessFailedCount(0);
            newUser.setCreatedDate(LocalDateTime.now());
            
            // Set normalized fields
            newUser.setNormalizedUserName(userRequest.getUsername().toUpperCase());
            newUser.setNormalizedEmail(userRequest.getEmail().toUpperCase());
            newUser.setSecurityStamp(UUID.randomUUID().toString());
            newUser.setConcurrencyStamp(UUID.randomUUID().toString());
            
            // Encode password
            if (userRequest.getPassword() != null && !userRequest.getPassword().isEmpty()) {
                newUser.setPassword(passwordEncoder.encode(userRequest.getPassword()));
            } else {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Password is required"));
            }
            
            // Save user
            User savedUser = userService.save(newUser);
            
            // Load with role for response
            User userWithRole = userService.findByIdWithRole(savedUser.getId());
            UserResponse userResponse = new UserResponse(userWithRole);
            
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("success", true, "message", "User created successfully", "user", userResponse));
        } catch (Exception e) {
            logger.error("Error creating user", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Error creating user: " + e.getMessage()));
        }
    }
    
    /**
     * Update user (API endpoint)
     */
    @PutMapping("/api/update/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateUser(
            @PathVariable String id,
            @Valid @RequestBody UserRequest userRequest,
            BindingResult bindingResult,
            HttpSession session) {
        
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null || !userService.isAdmin(currentUser)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("success", false, "message", "Unauthorized access"));
        }
        
        if (bindingResult.hasErrors()) {
            List<String> errors = bindingResult.getAllErrors().stream()
                    .map(error -> error.getDefaultMessage())
                    .collect(Collectors.toList());
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Validation failed", "errors", errors));
        }
        
        try {
            User existingUser = userService.findById(id);
            if (existingUser == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("success", false, "message", "User not found"));
            }
            
            // Check if username is being changed and if it already exists
            if (!existingUser.getUsername().equals(userRequest.getUsername()) &&
                    userService.existsByUsername(userRequest.getUsername())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Username already exists"));
            }
            
            // Check if email is being changed and if it already exists
            if (!existingUser.getEmail().equals(userRequest.getEmail()) &&
                    userService.existsByEmail(userRequest.getEmail())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Email already exists"));
            }
            
            // Validate role
            if (!roleService.isValidRoleId(userRequest.getRoleId())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Invalid role ID"));
            }
            
            // Check if role is being changed to Owner
            String oldRoleId = existingUser.getRoleId();
            boolean roleChangedToOwner = false;
            if (!oldRoleId.equals(userRequest.getRoleId())) {
                // Get the new role to check if it's Owner
                Role newRole = roleRepository.findById(userRequest.getRoleId()).orElse(null);
                if (newRole != null && "Owner".equalsIgnoreCase(newRole.getRoleName())) {
                    roleChangedToOwner = true;
                }
            }
            
            // Update user fields
            existingUser.setUsername(userRequest.getUsername());
            existingUser.setFirstName(userRequest.getFirstName());
            existingUser.setLastName(userRequest.getLastName());
            existingUser.setUserDOB(userRequest.getUserDOB());
            existingUser.setPhoneNumber(userRequest.getPhoneNumber());
            existingUser.setAvatarUrl(userRequest.getAvatarUrl());
            if (userRequest.getGender() != null) {
                existingUser.setGender(User.Gender.valueOf(userRequest.getGender()));
            }
            existingUser.setStatus(User.UserStatus.valueOf(userRequest.getStatus()));
            existingUser.setRoleId(userRequest.getRoleId());
            existingUser.setEmail(userRequest.getEmail());
            
            if (userRequest.getEmailVerified() != null) {
                existingUser.setEmailVerifed(userRequest.getEmailVerified());
            }
            if (userRequest.getTwoFactorEnabled() != null) {
                existingUser.setTwoFactorEnabled(userRequest.getTwoFactorEnabled());
            }
            if (userRequest.getLockoutEnabled() != null) {
                existingUser.setLockoutEnabled(userRequest.getLockoutEnabled());
            }
            
            // Update normalized fields
            existingUser.setNormalizedUserName(userRequest.getUsername().toUpperCase());
            existingUser.setNormalizedEmail(userRequest.getEmail().toUpperCase());
            
            // Update password if provided
            if (userRequest.getPassword() != null && !userRequest.getPassword().trim().isEmpty()) {
                existingUser.setPassword(passwordEncoder.encode(userRequest.getPassword()));
            }
            
            // Save user
            User updatedUser = userService.save(existingUser);
            
            // Send email if role changed to Owner
            if (roleChangedToOwner && updatedUser.getEmail() != null) {
                try {
                    String userName = (updatedUser.getFirstName() != null) ? 
                        (updatedUser.getFirstName() + " " + updatedUser.getLastName()) : updatedUser.getUsername();
                    emailService.sendOwnerApprovalNotification(updatedUser.getEmail(), userName);
                    logger.info("Owner approval email sent to user: {}", updatedUser.getEmail());
                } catch (Exception emailError) {
                    logger.warn("Failed to send owner approval email to {}: {}", 
                        updatedUser.getEmail(), emailError.getMessage());
                }
            }
            
            // Load with role for response
            User userWithRole = userService.findByIdWithRole(updatedUser.getId());
            UserResponse userResponse = new UserResponse(userWithRole);
            
            return ResponseEntity.ok()
                    .body(Map.of("success", true, "message", "User updated successfully", "user", userResponse));
        } catch (Exception e) {
            logger.error("Error updating user", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Error updating user: " + e.getMessage()));
        }
    }
    
    /**
     * Delete user (API endpoint)
     */
    @DeleteMapping("/api/delete/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteUser(@PathVariable String id, HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null || !userService.isAdmin(currentUser)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("success", false, "message", "Unauthorized access"));
        }
        
        try {
            // Prevent deleting self
            if (currentUser.getId().equals(id)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Cannot delete your own account"));
            }
            
            User user = userService.findById(id);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("success", false, "message", "User not found"));
            }
            
            userService.deleteById(id);
            
            return ResponseEntity.ok()
                    .body(Map.of("success", true, "message", "User deleted successfully"));
        } catch (Exception e) {
            logger.error("Error deleting user", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Error deleting user: " + e.getMessage()));
        }
    }
    
    /**
     * Update user status (API endpoint)
     */
    @PatchMapping("/api/status/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateUserStatus(
            @PathVariable String id,
            @RequestParam String status,
            HttpSession session) {
        
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null || !userService.isAdmin(currentUser)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("success", false, "message", "Unauthorized access"));
        }
        
        try {
            // Validate status
            if (!Arrays.asList("Active", "Inactive", "Banned").contains(status)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Invalid status value"));
            }
            
            User user = userService.findById(id);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("success", false, "message", "User not found"));
            }
            
            user.setStatus(User.UserStatus.valueOf(status));
            User updatedUser = userService.save(user);
            
            // Load with role for response
            User userWithRole = userService.findByIdWithRole(updatedUser.getId());
            UserResponse userResponse = new UserResponse(userWithRole);
            
            return ResponseEntity.ok()
                    .body(Map.of("success", true, "message", "User status updated successfully", "user", userResponse));
        } catch (Exception e) {
            logger.error("Error updating user status", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Error updating user status: " + e.getMessage()));
        }
    }
    
    /**
     * Get all roles (API endpoint)
     */
    @GetMapping("/api/roles")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getAllRoles(HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null || !userService.isAdmin(currentUser)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("success", false, "message", "Unauthorized access"));
        }
        
        try {
            List<Role> roles = roleRepository.findAll();
            return ResponseEntity.ok(Map.of("success", true, "roles", roles));
        } catch (Exception e) {
            logger.error("Error fetching roles", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Error fetching roles: " + e.getMessage()));
        }
    }
    
    /**
     * Search users (API endpoint)
     */
    @GetMapping("/api/search")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> searchUsers(
            @RequestParam String keyword,
            HttpSession session) {
        
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null || !userService.isAdmin(currentUser)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("success", false, "message", "Unauthorized access"));
        }
        
        try {
            List<User> users = userService.searchUsers(keyword);
            List<UserResponse> userResponses = users.stream()
                    .map(UserResponse::new)
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(Map.of("success", true, "users", userResponses, "total", userResponses.size()));
        } catch (Exception e) {
            logger.error("Error searching users", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Error searching users: " + e.getMessage()));
        }
    }
    
    /**
     * Ban user (Form endpoint)
     */
    @PostMapping("/ban")
    public String banUser(@RequestParam String userId, HttpSession session, Model model) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null || !userService.isAdmin(currentUser)) {
            return "redirect:/login";
        }
        
        try {
            // Prevent banning self
            if (currentUser.getId().equals(userId)) {
                model.addAttribute("error", "Cannot ban your own account");
                return "redirect:/admin/dashboard?tab=users";
            }
            
            User user = userService.findById(userId);
            if (user == null) {
                model.addAttribute("error", "User not found");
                return "redirect:/admin/dashboard?tab=users";
            }
            
            user.setStatus(User.UserStatus.Banned);
            userService.save(user);
            
            logger.info("User {} banned by admin {}", userId, currentUser.getUsername());
            model.addAttribute("success", "User banned successfully");
        } catch (Exception e) {
            logger.error("Error banning user", e);
            model.addAttribute("error", "Error banning user: " + e.getMessage());
        }
        
        return "redirect:/admin/dashboard?tab=users";
    }
    
    /**
     * Ban user (API endpoint for AJAX)
     */
    @PostMapping("/api/ban")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> banUserApi(@RequestParam String userId, HttpSession session) {
        try {
            User currentUser = (User) session.getAttribute("currentUser");
            if (currentUser == null || !userService.isAdmin(currentUser)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("success", false, "message", "Unauthorized access"));
            }
            
            // Prevent banning self
            if (currentUser.getId().equals(userId)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Cannot ban your own account"));
            }
            
            // Update status directly - the method will return false if user not found
            boolean success = userService.updateUserStatus(userId, User.UserStatus.Banned);
            
            if (!success) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("success", false, "message", "Failed to update user status"));
            }
            
            logger.info("User {} banned by admin {}", userId, currentUser.getUsername());
            return ResponseEntity.ok()
                    .body(Map.of("success", true, "message", "User banned successfully", "userId", userId));
        } catch (Exception e) {
            logger.error("Error banning user: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Error: " + e.getMessage()));
        }
    }
    
    /**
     * Unban user (Form endpoint)
     */
    @PostMapping("/unban")
    public String unbanUser(@RequestParam String userId, HttpSession session, Model model) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null || !userService.isAdmin(currentUser)) {
            return "redirect:/login";
        }
        
        try {
            User user = userService.findById(userId);
            if (user == null) {
                model.addAttribute("error", "User not found");
                return "redirect:/admin/dashboard?tab=users";
            }
            
            user.setStatus(User.UserStatus.Active);
            userService.save(user);
            
            logger.info("User {} unbanned by admin {}", userId, currentUser.getUsername());
            model.addAttribute("success", "User unbanned successfully");
        } catch (Exception e) {
            logger.error("Error unbanning user", e);
            model.addAttribute("error", "Error unbanning user: " + e.getMessage());
        }
        
        return "redirect:/admin/dashboard?tab=users";
    }
    
    /**
     * Unban user (API endpoint for AJAX)
     */
    @PostMapping("/api/unban")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> unbanUserApi(@RequestParam String userId, HttpSession session) {
        try {
            User currentUser = (User) session.getAttribute("currentUser");
            if (currentUser == null || !userService.isAdmin(currentUser)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("success", false, "message", "Unauthorized access"));
            }
            
            // Update status directly
            boolean success = userService.updateUserStatus(userId, User.UserStatus.Active);
            
            if (!success) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("success", false, "message", "Failed to update user status"));
            }
            
            logger.info("User {} unbanned by admin {}", userId, currentUser.getUsername());
            return ResponseEntity.ok()
                    .body(Map.of("success", true, "message", "User unbanned successfully", "userId", userId));
        } catch (Exception e) {
            logger.error("Error unbanning user: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Error: " + e.getMessage()));
        }
    }
}
