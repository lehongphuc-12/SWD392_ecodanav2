package com.ecodana.evodanavn1.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * DTO for User creation and update requests
 */
public class UserRequest {
    
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 100, message = "Username must be between 3 and 100 characters")
    private String username;
    
    @NotBlank(message = "First name is required")
    @Size(max = 256, message = "First name must not exceed 256 characters")
    private String firstName;
    
    @NotBlank(message = "Last name is required")
    @Size(max = 256, message = "Last name must not exceed 256 characters")
    private String lastName;
    
    private LocalDate userDOB;
    
    @Pattern(regexp = "^[0-9]{0,15}$", message = "Phone number must contain only digits and not exceed 15 characters")
    private String phoneNumber;
    
    private String avatarUrl;
    
    @Pattern(regexp = "^(Male|Female|Other)$", message = "Gender must be Male, Female, or Other")
    private String gender;
    
    @NotBlank(message = "Status is required")
    @Pattern(regexp = "^(Active|Inactive|Banned)$", message = "Status must be Active, Inactive, or Banned")
    private String status;
    
    @NotBlank(message = "Role ID is required")
    private String roleId;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    private String email;
    
    private Boolean emailVerified;
    
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;
    
    private Boolean twoFactorEnabled;
    
    private Boolean lockoutEnabled;
    
    // Constructors
    public UserRequest() {
        this.status = "Active";
        this.emailVerified = false;
        this.twoFactorEnabled = false;
        this.lockoutEnabled = false;
    }
    
    // Getters and Setters
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
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public Boolean getTwoFactorEnabled() {
        return twoFactorEnabled;
    }
    
    public void setTwoFactorEnabled(Boolean twoFactorEnabled) {
        this.twoFactorEnabled = twoFactorEnabled;
    }
    
    public Boolean getLockoutEnabled() {
        return lockoutEnabled;
    }
    
    public void setLockoutEnabled(Boolean lockoutEnabled) {
        this.lockoutEnabled = lockoutEnabled;
    }
}
