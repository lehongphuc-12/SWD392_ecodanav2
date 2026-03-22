package com.ecodana.evodanavn1.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.format.annotation.DateTimeFormat;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "Users")
public class User {
    @Id
    @Column(name = "UserId", length = 36)
    private String id;

    @Column(name = "Username", unique = true, nullable = false, length=100)
    private String username;

    @Column(name = "UserDOB")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate userDOB;

    @Column(name = "AvatarUrl", length = 255)
    private String avatarUrl;

    @Column(name = "Gender")
    @Enumerated(EnumType.STRING)
    private Gender gender;

    @NotBlank(message = "Họ không được để trống")
    @Column(name = "FirstName", length = 256)
    private String firstName;

    @NotBlank(message = "Tên không được để trống")
    @Column(name = "LastName", length = 256)
    private String lastName;

    @Column(name = "Status", nullable = false)
    @Enumerated(EnumType.STRING)
    private UserStatus status = UserStatus.Active;

    @Column(name = "RoleId", length = 36, nullable = false)
    private String roleId;

    @ManyToOne(fetch = jakarta.persistence.FetchType.EAGER)
    @JoinColumn(name = "RoleId", referencedColumnName = "RoleId", insertable = false, updatable = false)
    private Role role;

    @Column(name = "CreatedDate", nullable = false)
    private LocalDateTime createdDate;

    @Column(name = "NormalizedUserName", length = 256)
    private String normalizedUserName;

    @NotBlank(message = "Email là bắt buộc")
    @Email(message = "Vui lòng cung cấp địa chỉ email hợp lệ")
    @Column(name = "Email", unique = true, nullable = false, length = 100)
    private String email;

    @Column(name = "NormalizedEmail", length = 256)
    private String normalizedEmail;

    @Column(name = "EmailVerifed", nullable = false)
    private boolean emailVerifed = false;

    @Column(name = "PasswordHash", nullable = false, length = 255)
    private String password;

    @Size(max = 15, message = "Số điện thoại không được vượt quá 15 ký tự")
    @Column(name = "PhoneNumber", length = 15)
    private String phoneNumber;

    @Column(name = "SecurityStamp", columnDefinition = "TEXT")
    private String securityStamp;

    @Column(name = "ConcurrencyStamp", columnDefinition = "TEXT")
    private String concurrencyStamp;

    @Column(name = "TwoFactorEnabled", nullable = false)
    private boolean twoFactorEnabled = false;

    @Column(name = "LockoutEnd")
    private LocalDateTime lockoutEnd;

    @Column(name = "LockoutEnabled", nullable = false)
    private boolean lockoutEnabled = false;

    @Column(name = "AccessFailedCount", nullable = false)
    private int accessFailedCount = 0;

    @Transient
    private boolean hasLicense = false;

    // Constructors
    public User() {
        this.createdDate = LocalDateTime.now();
        this.status = UserStatus.Active;
        this.emailVerifed = false;
        this.twoFactorEnabled = false;
        this.lockoutEnabled = false;
        this.accessFailedCount = 0;
    }

    public User(String username, String email, String password, String phoneNumber) {
        this();
        this.username = username;
        this.email = email;
        this.password = password;
        this.phoneNumber = phoneNumber;
        this.hasLicense = false;
    }

    // Getters/Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public LocalDate getUserDOB() { return userDOB; }
    public void setUserDOB(LocalDate userDOB) { this.userDOB = userDOB; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public Gender getGender() { return gender; }
    public void setGender(Gender gender) { this.gender = gender; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public UserStatus getStatus() { return status; }
    public void setStatus(UserStatus status) { this.status = status; }

    public String getRoleId() { return roleId; }
    public void setRoleId(String roleId) { this.roleId = roleId; }

    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }

    public String getNormalizedUserName() { return normalizedUserName; }
    public void setNormalizedUserName(String normalizedUserName) { this.normalizedUserName = normalizedUserName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getNormalizedEmail() { return normalizedEmail; }
    public void setNormalizedEmail(String normalizedEmail) { this.normalizedEmail = normalizedEmail; }

    public boolean isEmailVerifed() { return emailVerifed; }
    public void setEmailVerifed(boolean emailVerifed) { this.emailVerifed = emailVerifed; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getSecurityStamp() { return securityStamp; }
    public void setSecurityStamp(String securityStamp) { this.securityStamp = securityStamp; }

    public String getConcurrencyStamp() { return concurrencyStamp; }
    public void setConcurrencyStamp(String concurrencyStamp) { this.concurrencyStamp = concurrencyStamp; }

    public boolean isTwoFactorEnabled() { return twoFactorEnabled; }
    public void setTwoFactorEnabled(boolean twoFactorEnabled) { this.twoFactorEnabled = twoFactorEnabled; }

    public LocalDateTime getLockoutEnd() { return lockoutEnd; }
    public void setLockoutEnd(LocalDateTime lockoutEnd) { this.lockoutEnd = lockoutEnd; }

    public boolean isLockoutEnabled() { return lockoutEnabled; }
    public void setLockoutEnabled(boolean lockoutEnabled) { this.lockoutEnabled = lockoutEnabled; }

    public int getAccessFailedCount() { return accessFailedCount; }
    public void setAccessFailedCount(int accessFailedCount) { this.accessFailedCount = accessFailedCount; }

    public boolean isHasLicense() { return hasLicense; }
    public void setHasLicense(boolean hasLicense) { this.hasLicense = hasLicense; }

    public boolean isActive() {
        return UserStatus.Active.equals(this.status);
    }
    public void setActive(boolean active) {
        this.status = active ? UserStatus.Active : UserStatus.Inactive;
    }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public String getRoleName() {
        if (role != null) {
            return role.getRoleName();
        }
        return "CUSTOMER"; // Default role
    }

    public boolean hasRole(String roleName) {
        if (role != null) {
            return role.getRoleName().equalsIgnoreCase(roleName);
        }
        return false;
    }

    public enum Gender {
        Male, Female, Other
    }

    public enum UserStatus {
        Active, Inactive, Banned
    }

    @Override
    public String toString() {
        return "User{" +
                "id='" + id + '\'' +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", roleName='" + getRoleName() + '\'' +
                '}';
    }
}