package com.ecodana.evodanavn1.service;

import java.util.List;
import java.util.Optional;

import com.ecodana.evodanavn1.model.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ecodana.evodanavn1.repository.RoleRepository;

@Service
public class RoleService {
    
    @Autowired
    private RoleRepository roleRepository;
    
    // Default role names - will be loaded from database
    private static final String CUSTOMER_ROLE_NAME = "Customer";
    private static final String STAFF_ROLE_NAME = "Staff";
    private static final String ADMIN_ROLE_NAME = "Admin";
    
    /**
     * Find role by role ID
     * @param roleId the role ID to search for
     * @return Optional containing the role if found
     */
    public Optional<Role> findById(String roleId) {
        return roleRepository.findById(roleId);
    }
    
    /**
     * Find role by role name
     * @param roleName the role name to search for
     * @return Optional containing the role if found
     */
    public Optional<Role> findByRoleName(String roleName) {
        return roleRepository.findByRoleName(roleName);
    }
    
    /**
     * Find role by normalized name
     * @param normalizedName the normalized name to search for
     * @return Optional containing the role if found
     */
    public Optional<Role> findByNormalizedName(String normalizedName) {
        return roleRepository.findByNormalizedName(normalizedName);
    }
    
    /**
     * Get all roles
     * @return list of all roles
     */
    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }
    
    /**
     * Validates if the given role ID is valid
     * @param roleId the role ID to validate
     * @return true if valid, false otherwise
     */
    public boolean isValidRoleId(String roleId) {
        if (roleId == null || roleId.trim().isEmpty()) {
            return false;
        }
        return roleRepository.existsById(roleId.trim());
    }
    
    /**
     * Validates if the given role name is valid
     * @param roleName the role name to validate
     * @return true if valid, false otherwise
     */
    public boolean isValidRoleName(String roleName) {
        if (roleName == null || roleName.trim().isEmpty()) {
            return false;
        }
        return roleRepository.existsByRoleName(roleName.trim());
    }
    
    /**
     * Gets the default role ID for customers
     * @return the default customer role ID from database
     */
    public String getDefaultCustomerRoleId() {
        Role role = getCustomerRole();
        return role != null ? role.getRoleId() : null;
    }
    
    /**
     * Gets the default role ID for staff
     * @return the default staff role ID from database
     */
    public String getDefaultStaffRoleId() {
        Role role = getStaffRole();
        return role != null ? role.getRoleId() : null;
    }
    
    /**
     * Gets the default role ID for admin
     * @return the default admin role ID from database
     */
    public String getDefaultAdminRoleId() {
        Role role = getAdminRole();
        return role != null ? role.getRoleId() : null;
    }
    
    /**
     * Gets the default role ID for owner
     * @return the default owner role ID from database
     */
    public String getDefaultOwnerRoleId() {
        Role role = getOwnerRole();
        return role != null ? role.getRoleId() : null;
    }
    
    /**
     * Get customer role from database
     * @return customer role or null if not found
     */
    public Role getCustomerRole() {
        return findByRoleName(CUSTOMER_ROLE_NAME).orElse(null);
    }
    
    /**
     * Get staff role from database
     * @return staff role or null if not found
     */
    public Role getStaffRole() {
        return findByRoleName(STAFF_ROLE_NAME).orElse(null);
    }
    
    /**
     * Get admin role from database
     * @return admin role or null if not found
     */
    public Role getAdminRole() {
        return findByRoleName(ADMIN_ROLE_NAME).orElse(null);
    }
    
    /**
     * Get owner role from database
     * @return owner role or null if not found
     */
    public Role getOwnerRole() {
        return findByRoleName("Owner").orElse(null);
    }
    
    /**
     * Check if user has specific role by role name
     * @param userRole the user's role
     * @param roleName the role name to check
     * @return true if user has the role, false otherwise
     */
    public boolean hasRole(Role userRole, String roleName) {
        if (userRole == null || roleName == null) {
            return false;
        }
        return roleName.equalsIgnoreCase(userRole.getRoleName());
    }
    
    /**
     * Check if user is admin
     * @param userRole the user's role
     * @return true if user is admin, false otherwise
     */
    public boolean isAdmin(Role userRole) {
        return hasRole(userRole, ADMIN_ROLE_NAME);
    }
    
    /**
     * Check if user is staff
     * @param userRole the user's role
     * @return true if user is staff, false otherwise
     */
    public boolean isStaff(Role userRole) {
        return hasRole(userRole, STAFF_ROLE_NAME);
    }
    
    /**
     * Check if user is customer
     * @param userRole the user's role
     * @return true if user is customer, false otherwise
     */
    public boolean isCustomer(Role userRole) {
        return hasRole(userRole, CUSTOMER_ROLE_NAME);
    }
}
