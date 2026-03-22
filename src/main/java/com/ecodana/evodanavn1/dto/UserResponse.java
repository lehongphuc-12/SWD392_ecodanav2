package com.ecodana.evodanavn1.dto;

import com.ecodana.evodanavn1.model.User;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO for User responses
 */
public class UserResponse {
    
    private String id;
    private String username;
    private String firstName;
    private String lastName;
    private LocalDate userDOB;
    private String phoneNumber;
    private String avatarUrl;
    private String gender;
    private String status;
    private String roleId;
    private String roleName;
    private String email;
    private Boolean emailVerified;
    private LocalDateTime createdDate;
    private Boolean twoFactorEnabled;
    private LocalDateTime lockoutEnd;
    private Boolean lockoutEnabled;
    private Integer accessFailedCount;
    
    // Constructors
    public UserResponse() {}
    
    public UserResponse(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.firstName = user.getFirstName();
        this.lastName = user.getLastName();
        this.userDOB = user.getUserDOB();
        this.phoneNumber = user.getPhoneNumber();
        this.avatarUrl = user.getAvatarUrl();
        this.gender = user.getGender() != null ? user.getGender().name() : null;
        this.status = user.getStatus() != null ? user.getStatus().name() : null;
        this.roleId = user.getRoleId();
        this.roleName = user.getRole() != null ? user.getRole().getRoleName() : null;
        this.email = user.getEmail();
        this.emailVerified = user.isEmailVerifed();
        this.createdDate = user.getCreatedDate();
        this.twoFactorEnabled = user.isTwoFactorEnabled();
        this.lockoutEnd = user.getLockoutEnd();
        this.lockoutEnabled = user.isLockoutEnabled();
        this.accessFailedCount = user.getAccessFailedCount();
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getFirstName() {
        return firstName;
    }
    
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }
    
    public String getLastName() {
        return lastName;
    }
    
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
    
    public String getFullName() {
        return (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "");
    }
    
    public LocalDate getUserDOB() {
        return userDOB;
    }
    
    public void setUserDOB(LocalDate userDOB) {
        this.userDOB = userDOB;
    }
    
    public String getPhoneNumber() {
        return phoneNumber;
    }
    
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
    
    public String getAvatarUrl() {
        return avatarUrl;
    }
    
    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }
    
    public String getGender() {
        return gender;
    }
    
    public void setGender(String gender) {
        this.gender = gender;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getRoleId() {
        return roleId;
    }
    
    public void setRoleId(String roleId) {
        this.roleId = roleId;
    }
    
    public String getRoleName() {
        return roleName;
    }
    
    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public Boolean getEmailVerified() {
        return emailVerified;
    }
    
    public void setEmailVerified(Boolean emailVerified) {
        this.emailVerified = emailVerified;
    }
    
    public LocalDateTime getCreatedDate() {
        return createdDate;
    }
    
    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }
    
    public Boolean getTwoFactorEnabled() {
        return twoFactorEnabled;
    }
    
    public void setTwoFactorEnabled(Boolean twoFactorEnabled) {
        this.twoFactorEnabled = twoFactorEnabled;
    }
    
    public LocalDateTime getLockoutEnd() {
        return lockoutEnd;
    }
    
    public void setLockoutEnd(LocalDateTime lockoutEnd) {
        this.lockoutEnd = lockoutEnd;
    }
    
    public Boolean getLockoutEnabled() {
        return lockoutEnabled;
    }
    
    public void setLockoutEnabled(Boolean lockoutEnabled) {
        this.lockoutEnabled = lockoutEnabled;
    }
    
    public Integer getAccessFailedCount() {
        return accessFailedCount;
    }
    
    public void setAccessFailedCount(Integer accessFailedCount) {
        this.accessFailedCount = accessFailedCount;
    }
}
