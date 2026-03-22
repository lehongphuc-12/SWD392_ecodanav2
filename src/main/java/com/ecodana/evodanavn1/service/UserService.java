package com.ecodana.evodanavn1.service;

import com.ecodana.evodanavn1.model.PasswordResetToken;
import com.ecodana.evodanavn1.model.User;
import com.ecodana.evodanavn1.model.User.Gender;
import com.ecodana.evodanavn1.model.User.UserStatus;
import com.ecodana.evodanavn1.repository.PasswordResetTokenRepository;
import com.ecodana.evodanavn1.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import com.ecodana.evodanavn1.model.UserLogins;
import com.ecodana.evodanavn1.repository.UserLoginsRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    public enum TokenValidationResult {
        VALID,
        INVALID,
        EXPIRED,
        USED
    }

    @Autowired
    private UserLoginsRepository userLoginsRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordResetTokenRepository tokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private com.ecodana.evodanavn1.service.RoleService roleService;

    @Autowired
    private com.ecodana.evodanavn1.repository.RoleRepository roleRepository;

    @Autowired
    private com.ecodana.evodanavn1.service.EmailService emailService;

    public User login(String username, String password, String secretKey) {
        Optional<User> userOpt = userRepository.findByUsername(username);

        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByEmail(username);
        }

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            
            // Check if user is banned
            if (user.getStatus() == UserStatus.Banned) {
                logger.warn("Login attempt by banned user: {}", username);
                return null;
            }
            
            // Check if user is inactive
            if (user.getStatus() == UserStatus.Inactive) {
                logger.warn("Login attempt by inactive user: {}", username);
                return null;
            }

            boolean passwordMatches = passwordEncoder.matches(password, user.getPassword());

            if (passwordMatches) {
                user.getRole(); // Eager load role
                return user;
            }
        }
        return null;
    }

    public boolean register(User user) {
        try {
            if (user == null || user.getEmail() == null || user.getEmail().trim().isEmpty() || user.getPassword() == null || user.getPassword().trim().isEmpty()) {
                return false;
            }

            if (userRepository.existsByEmail(user.getEmail())) {
                return false;
            }

            if (user.getUsername() != null && !user.getUsername().isEmpty() &&
                    userRepository.existsByUsername(user.getUsername())) {
                return false;
            }

            if (user.getId() == null || user.getId().isEmpty()) {
                user.setId(UUID.randomUUID().toString());
            }

            if (user.getUsername() == null || user.getUsername().isEmpty()) {
                String email = user.getEmail();
                String username = email.split("@")[0] + "_" + System.currentTimeMillis();
                user.setUsername(username);
            }

            user.setNormalizedUserName(user.getUsername().toUpperCase());
            user.setNormalizedEmail(user.getEmail().toUpperCase());
            user.setSecurityStamp(UUID.randomUUID().toString());
            user.setConcurrencyStamp(UUID.randomUUID().toString());

            if (user.getFirstName() == null) user.setFirstName("");
            if (user.getLastName() == null) user.setLastName("");

            if (user.getRoleId() == null || user.getRoleId().isEmpty() || !roleService.isValidRoleId(user.getRoleId())) {
                user.setRoleId(roleService.getDefaultCustomerRoleId());
            }

            user.setStatus(UserStatus.Active);
            user.setEmailVerifed(false);
            user.setTwoFactorEnabled(false);
            user.setLockoutEnabled(false);
            user.setAccessFailedCount(0);
            user.setCreatedDate(java.time.LocalDateTime.now());

            user.setPassword(passwordEncoder.encode(user.getPassword()));

            userRepository.save(user);
            return true;
        } catch (Exception e) {
            logger.error("Error in UserService.register(): " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Tìm người dùng bằng thông tin đăng nhập OAuth (LoginProvider và ProviderKey)
     *
     * @param loginProvider Tên nhà cung cấp (vd: "google")
     * @param providerKey   ID người dùng từ nhà cung cấp
     * @return Optional<User>
     */
    public Optional<User> findUserByLogin(String loginProvider, String providerKey) {
        return userLoginsRepository.findByLoginProviderAndProviderKey(loginProvider, providerKey)
                .map(UserLogins::getUser);
    }

    /**
     * Tạo liên kết OAuth cho một người dùng đã tồn tại
     *
     * @param user          Người dùng trong hệ thống
     * @param loginProvider Tên nhà cung cấp
     * @param providerKey   ID người dùng từ nhà cung cấp
     * @param providerDisplayName Tên hiển thị (vd: Email)
     */
    @Transactional
    public void linkOAuthAccount(User user, String loginProvider, String providerKey, String providerDisplayName) {
        UserLogins userLogin = new UserLogins();
        userLogin.setUser(user);
        userLogin.setLoginProvider(loginProvider);
        userLogin.setProviderKey(providerKey);
        userLogin.setProviderDisplayName(providerDisplayName);

        userLoginsRepository.save(userLogin);
    }

    /**
     * Kiểm tra xem người dùng có liên kết OAuth (như Google) hay không.
     *
     * @param userId ID của người dùng
     * @param provider Tên nhà cung cấp (vd: "google")
     * @return true nếu có liên kết, false nếu không
     */
    public boolean isProviderLinked(String userId, String provider) {
        try {
            List<UserLogins> logins = userLoginsRepository.findByUser_Id(userId);
            for (UserLogins login : logins) {
                if (provider.equalsIgnoreCase(login.getLoginProvider())) {
                    return true;
                }
            }
        } catch (Exception e) {
            logger.error("Lỗi khi kiểm tra UserLogins: " + e.getMessage());
        }
        return false;
    }


    /**
     * Kiểm tra xem tài khoản có mật khẩu (không phải chỉ OAuth) hay không
     */
    public boolean isPasswordAccount(User user) {
        if (user == null || user.getPassword() == null) {
            return false;
        }
        // Tài khoản OAuth được tạo bởi SuccessHandler (bản gốc) có mật khẩu là "OAUTH_USER_..."
        // và được mã hóa bởi register().
        // Tài khoản OAuth (bản sửa lỗi) có mật khẩu là HASH của "OAUTH_USER_..."

        // Cách kiểm tra đơn giản nhất (nhưng không hoàn toàn chính xác nếu bạn thay đổi logic):
        // Nếu user *không* có liên kết "google" (hoặc provider nào khác),
        // thì đó chắc chắn là tài khoản mật khẩu.
        try {
            List<UserLogins> logins = userLoginsRepository.findByUser_Id(user.getId());
            return logins.isEmpty(); // Nếu không có liên kết nào, đó là tài khoản mật khẩu
        } catch (Exception e) {
            logger.error("Lỗi khi kiểm tra UserLogins: " + e.getMessage());
            // An toàn nhất là giả định là tài khoản mật khẩu nếu có lỗi
            return true;
        }
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }

    public User findByIdWithRole(String id) {
        return userRepository.findById(id).map(user -> {
            user.getRole(); // Eager load role
            return user;
        }).orElse(null);
    }

    public User findByUsernameWithRole(String username) {
        return userRepository.findByUsername(username).map(user -> {
            user.getRole(); // Eager load role
            return user;
        }).orElse(null);
    }

    public User findByEmailWithRole(String email) {
        return userRepository.findByEmail(email).map(user -> {
            user.getRole(); // Eager load role
            return user;
        }).orElse(null);
    }

    public User getUserWithRole(String usernameOrEmail) {
        User user = findByUsernameWithRole(usernameOrEmail);
        if (user != null) {
            return user;
        }
        return findByEmailWithRole(usernameOrEmail);
    }

    public boolean hasRole(User user, String roleName) {
        if (user == null || user.getRole() == null) {
            return false;
        }
        return roleName.equalsIgnoreCase(user.getRole().getRoleName());
    }

    public boolean isAdmin(User user) {
        return hasRole(user, "Admin");
    }

    public boolean isStaff(User user) {
        return hasRole(user, "Staff");
    }

    public boolean isOwner(User user) {
        return hasRole(user, "Owner");
    }

    public boolean isCustomer(User user) {
        return hasRole(user, "Customer");
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public List<User> getAllUsersWithRole() {
        try {
            return userRepository.findAllWithRoles();
        } catch (Exception e) {
            logger.error("Error loading users with roles: " + e.getMessage(), e);
            return userRepository.findAll();
        }
    }

    public User updateUser(User user) {
        return userRepository.save(user);
    }

    public User updateUser(String id, String firstName, String lastName, LocalDate userDOB, String gender, String phoneNumber) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setUserDOB(userDOB);
            try {
                if (gender != null && !gender.trim().isEmpty()) {
                    user.setGender(Gender.valueOf(gender));
                }
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid gender value provided for user update: " + gender);
                // Optionally handle the error, e.g., by setting a default or leaving it unchanged
            }
            user.setPhoneNumber(phoneNumber);
            return userRepository.save(user);
        }
        return null;
    }

    public Map<String, Object> getUserStatistics() {
        Map<String, Object> stats = new HashMap<>();
        List<User> allUsers = getAllUsers();
        long activeUsers = allUsers.stream().filter(u -> u.getStatus() == UserStatus.Active).count();
        long inactiveUsers = allUsers.stream().filter(u -> u.getStatus() == UserStatus.Inactive).count();
        long bannedUsers = allUsers.stream().filter(u -> u.getStatus() == UserStatus.Banned).count();

        stats.put("totalUsers", allUsers.size());
        stats.put("activeUsers", activeUsers);
        stats.put("inactiveUsers", inactiveUsers);
        stats.put("bannedUsers", bannedUsers);

        return stats;
    }

    public List<User> getUsersByRole(String roleName) {
        return userRepository.findByRoleName(roleName);
    }

    public List<User> getRecentUsers(int limit) {
        return userRepository.findRecentUsers().stream().limit(limit).toList();
    }

    public boolean suspendUser(String userId) {
        return userRepository.findById(userId).map(user -> {
            user.setStatus(UserStatus.Banned);
            userRepository.save(user);
            return true;
        }).orElse(false);
    }

    public boolean activateUser(String userId) {
        return userRepository.findById(userId).map(user -> {
            user.setStatus(UserStatus.Active);
            userRepository.save(user);
            return true;
        }).orElse(false);
    }

    public boolean deleteUser(String userId) {
        try {
            if (userRepository.existsById(userId)) {
                userRepository.deleteById(userId);
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            logger.error("Error deleting user: " + e.getMessage(), e);
            return false;
        }
    }

    public boolean updateUserRole(String userId, String roleId) {
        if (!roleService.isValidRoleId(roleId)) {
            return false;
        }

        return userRepository.findById(userId).map(user -> {
            // Check if role is being changed to Owner
            String oldRoleId = user.getRoleId();
            boolean roleChangedToOwner = false;
            
            if (!oldRoleId.equals(roleId)) {
                // Get the new role to check if it's Owner
                com.ecodana.evodanavn1.model.Role newRole = roleRepository.findById(roleId).orElse(null);
                if (newRole != null && "Owner".equalsIgnoreCase(newRole.getRoleName())) {
                    roleChangedToOwner = true;
                }
            }
            
            user.setRoleId(roleId);
            User savedUser = userRepository.save(user);
            
            // Send email if role changed to Owner
            if (roleChangedToOwner && savedUser.getEmail() != null) {
                try {
                    String userName = (savedUser.getFirstName() != null) ? 
                        (savedUser.getFirstName() + " " + savedUser.getLastName()) : savedUser.getUsername();
                    emailService.sendOwnerApprovalNotification(savedUser.getEmail(), userName);
                    logger.info("Owner approval email sent to user: {}", savedUser.getEmail());
                } catch (Exception emailError) {
                    logger.warn("Failed to send owner approval email to {}: {}", 
                        savedUser.getEmail(), emailError.getMessage());
                }
            }
            
            return true;
        }).orElse(false);
    }


    public Optional<User> findUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Transactional
    public PasswordResetToken createPasswordResetTokenForUser(User user) {
        String newTokenValue = UUID.randomUUID().toString();
        LocalDateTime expiryDate = LocalDateTime.now().plusMinutes(15);

        Optional<PasswordResetToken> existingToken = tokenRepository.findByUser(user);

        PasswordResetToken passwordResetToken;
        if (existingToken.isPresent()) {
            passwordResetToken = existingToken.get();
            passwordResetToken.setToken(newTokenValue);
            passwordResetToken.setExpiryTime(expiryDate);
            passwordResetToken.setUsed(false); // Mark as not used for a new request
        } else {
            passwordResetToken = new PasswordResetToken(newTokenValue, user, expiryDate);
        }
        return tokenRepository.save(passwordResetToken);
    }

    public Optional<PasswordResetToken> getPasswordResetToken(String token) {
        return Optional.ofNullable(tokenRepository.findByToken(token));
    }

    public TokenValidationResult validatePasswordResetToken(String token) {
        Optional<PasswordResetToken> passTokenOpt = getPasswordResetToken(token);
        if (passTokenOpt.isEmpty()) {
            return TokenValidationResult.INVALID;
        }

        PasswordResetToken passToken = passTokenOpt.get();
        if (passToken.isUsed()) {
            return TokenValidationResult.USED;
        }

        if (passToken.getExpiryTime().isBefore(LocalDateTime.now())) {
            return TokenValidationResult.EXPIRED;
        }

        return TokenValidationResult.VALID;
    }

    @Transactional
    public TokenValidationResult resetPassword(String token, String newPassword) {
        TokenValidationResult validationResult = validatePasswordResetToken(token);
        if (validationResult != TokenValidationResult.VALID) {
            return validationResult;
        }

        // We can safely assume the token is present because validation passed
        Optional<PasswordResetToken> tokenOptional = getPasswordResetToken(token);
        if (tokenOptional.isPresent()) {
            PasswordResetToken resetToken = tokenOptional.get();
            User user = resetToken.getUser();
            
            // Change password and invalidate token
            changeUserPassword(user, newPassword);
            resetToken.setUsed(true);
            tokenRepository.save(resetToken);
            
            return TokenValidationResult.VALID;
        } else {
            // This case should theoretically not be reached if validation passed
            return TokenValidationResult.INVALID;
        }
    }

    /**
     * Changes the password for an authenticated user.
     *
     * @param userId          The ID of the user to change the password for.
     * @param currentPassword The current password for verification.
     * @param newPassword     The new password.
     * @return true if the password was changed successfully, false if the current password is incorrect.
     */
    @Transactional
    public boolean changePasswordForAuthenticatedUser(String userId, String currentPassword, String newPassword) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            // Check if the current password matches
            if (passwordEncoder.matches(currentPassword, user.getPassword())) {
                // If it matches, encode and set the new password
                user.setPassword(passwordEncoder.encode(newPassword));
                userRepository.save(user);
                return true;
            }
        }
        // Return false if user not found or current password doesn't match
        return false;
    }

    public void changeUserPassword(User user, String newPassword) {
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    public List<User> searchUsers(String keyword) {
        return userRepository.searchUsers(keyword);
    }

    public User findById(String id) {
        return userRepository.findById(id).orElse(null);
    }

    @Transactional
    public User save(User user) {
        return userRepository.save(user);
    }
    
    @Transactional
    public boolean updateUserStatus(String userId, User.UserStatus status) {
        try {
            // Convert enum to string: 'Inactive', 'Active', 'Banned'
            String statusValue = status.name();
            logger.info("Updating user {} status to {} (value: {})", userId, status, statusValue);
            int rowsUpdated = userRepository.updateUserStatus(userId, statusValue);
            logger.info("Rows updated: {}", rowsUpdated);
            return rowsUpdated > 0;
        } catch (Exception e) {
            logger.error("Error updating user status: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to update user status: " + e.getMessage(), e);
        }
    }

    @Transactional
    public void deleteById(String id) {
        Optional<User> userOptional = userRepository.findById(id);
        if (userOptional.isPresent()) {
            User user = userOptional.get();

            // Delete dependent PasswordResetToken first
            Optional<PasswordResetToken> tokenOptional = tokenRepository.findByUser(user);
            tokenOptional.ifPresent(tokenRepository::delete);

            // NOTE: Add deletion for other dependent entities here in the future
            // For example: bookingRepository.deleteByUser(user);

            // Finally, delete the user
            userRepository.delete(user);
        }
    }

    public boolean existsByPhoneNumber(String phoneNumber) {return userRepository.existsByPhoneNumber(phoneNumber); }

    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
}
