package com.ecodana.evodanavn1.security;

import java.io.IOException;
import java.util.UUID;

import com.ecodana.evodanavn1.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.ecodana.evodanavn1.security.CustomOAuth2UserService.CustomOidcUser;
import com.ecodana.evodanavn1.service.RoleService;
import com.ecodana.evodanavn1.service.UserService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger logger = LoggerFactory.getLogger(OAuth2LoginSuccessHandler.class);

    private final UserService userService;
    private final RoleService roleService;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public OAuth2LoginSuccessHandler(UserService userService, RoleService roleService, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.roleService = roleService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        try {
            String loginProvider = ((OAuth2AuthenticationToken) authentication).getAuthorizedClientRegistrationId();
            Object principal = authentication.getPrincipal();
            OAuth2User oauth2User;
            User user = null; // The user from our database

            if (principal instanceof CustomOidcUser customOidcUser) {
                oauth2User = customOidcUser;
                user = customOidcUser.getUser(); // This might be a user found via email, but not yet linked
            } else if (principal instanceof OAuth2User) {
                oauth2User = (OAuth2User) principal;
                // Fallback if not using CustomOidcUser: find by provider key
                user = userService.findUserByLogin(loginProvider, oauth2User.getName()).orElse(null);
            } else {
                logger.error("Invalid principal type in OAuth2LoginSuccessHandler: {}", principal.getClass().getName());
                response.sendRedirect("/login?error=invalid_principal");
                return;
            }

            String email = oauth2User.getAttribute("email");
            if (email == null || email.isEmpty()) {
                logger.warn("OAuth2 login attempt without email from provider: {}", loginProvider);
                response.sendRedirect("/login?error=no_email");
                return;
            }

            // If user from principal is null, try to find an existing user by email.
            // This covers cases where a user signed up with a password first.
            if (user == null) {
                user = userService.findByEmail(email);
            }

            String providerKey = oauth2User.getName();

            // Case 1: User does not exist at all. Create a new user account.
            if (user == null) {
                logger.info("Creating new user for email {} from provider {}", email, loginProvider);
                user = new User();
                user.setId(UUID.randomUUID().toString());
                user.setEmail(email);
                user.setPassword(passwordEncoder.encode("OAUTH_USER_" + UUID.randomUUID().toString()));
                user.setPhoneNumber(""); // Set a default empty value if required
                user.setStatus(User.UserStatus.Active);

                String name = oauth2User.getAttribute("name");
                String avatarUrl = oauth2User.getAttribute("picture");

                if (name != null && !name.isEmpty()) {
                    String[] nameParts = name.split(" ", 2);
                    user.setFirstName(nameParts.length > 0 ? nameParts[0] : name);
                    user.setLastName(nameParts.length > 1 ? nameParts[1] : "");
                } else {
                    user.setFirstName(email.split("@")[0]);
                    user.setLastName("");
                }

                user.setUsername(email.split("@")[0] + "_" + System.currentTimeMillis());
                user.setAvatarUrl(avatarUrl);

                String assignedRoleId = getAssignedRoleForEmail(email);
                user.setRoleId(assignedRoleId != null ? assignedRoleId : roleService.getDefaultCustomerRoleId());

                user.setNormalizedUserName(user.getUsername().toUpperCase());
                user.setNormalizedEmail(user.getEmail().toUpperCase());
                user.setSecurityStamp(UUID.randomUUID().toString());
                user.setConcurrencyStamp(UUID.randomUUID().toString());
                user.setEmailVerifed(true); // Email from provider is considered verified
                user.setCreatedDate(java.time.LocalDateTime.now());

                user = userService.save(user);
                userService.linkOAuthAccount(user, loginProvider, providerKey, email);
                logger.info("Successfully created and linked new user {}", email);
            }
            // Case 2: User exists, but the link to this specific OAuth provider doesn't.
            else if (!userService.isProviderLinked(user.getId(), loginProvider)) {
                logger.info("Linking existing user {} with provider {}", email, loginProvider);
                userService.linkOAuthAccount(user, loginProvider, providerKey, email);
            }
            // Case 3: User exists and is already linked. No action needed.
            else {
                logger.info("User {} already linked with provider {}. Proceeding to login.", email, loginProvider);
            }

            // Final checks and redirection
            if (user.getStatus() == User.UserStatus.Banned) {
                logger.warn("Banned user login attempt: {}", user.getEmail());
                response.sendRedirect("/login?error=account_banned");
                return;
            }

            if (user.getStatus() == User.UserStatus.Inactive) {
                logger.warn("Inactive user login attempt: {}", user.getEmail());
                response.sendRedirect("/login?error=account_inactive");
                return;
            }

            HttpSession session = request.getSession(true);

            User userWithRole = userService.findByIdWithRole(user.getId());
            session.setAttribute("currentUser", userWithRole);

            String roleName = userWithRole.getRoleName();
            String displayName = userWithRole.getFirstName() != null && !userWithRole.getFirstName().isEmpty() ? userWithRole.getFirstName() : userWithRole.getUsername();

            logger.info("User {} logged in successfully with role {}", user.getEmail(), roleName);

            if ("Admin".equalsIgnoreCase(roleName)) {
                session.setAttribute("flash_success", "🎉 Đăng nhập thành công! Chào mừng Admin " + displayName + "!");
                response.sendRedirect("/admin");
            } else if ("Owner".equalsIgnoreCase(roleName)) {
                session.setAttribute("flash_success", "🎉 Đăng nhập thành công! Chào mừng Owner " + displayName + "!");
                response.sendRedirect("/owner/dashboard");
            } else if ("Staff".equalsIgnoreCase(roleName)) {
                session.setAttribute("flash_success", "🎉 Đăng nhập thành công! Chào mừng Staff " + displayName + "!");
                response.sendRedirect("/staff");
            } else {
                session.setAttribute("flash_success", "🎉 Đăng nhập thành công! Chào mừng " + displayName + "!");
                response.sendRedirect("/");
            }
        } catch (Exception e) {
            logger.error("Critical error in OAuth2LoginSuccessHandler", e);
            response.sendRedirect("/login?error=oauth_error");
        }
    }

    private String getAssignedRoleForEmail(String email) {
        try {
            User existingUser = userService.findByEmailWithRole(email);
            if (existingUser != null && existingUser.getRole() != null && !"Customer".equalsIgnoreCase(existingUser.getRole().getRoleName())) {
                return existingUser.getRoleId();
            }
            if (isAdminEmail(email)) return roleService.getDefaultAdminRoleId();
            if (isStaffEmail(email)) return roleService.getDefaultStaffRoleId();
            if (isOwnerEmail(email)) return roleService.getDefaultOwnerRoleId();
            return null;
        } catch (Exception e) {
            logger.error("Error in getAssignedRoleForEmail for email: {}", email, e);
            return null;
        }
    }

    private boolean isAdminEmail(String email) {
        return email != null && (email.equalsIgnoreCase("admin@ecodana.com") || email.endsWith("@ecodana.com"));
    }

    private boolean isStaffEmail(String email) {
        return email != null && email.equalsIgnoreCase("staff@ecodana.com");
    }

    private boolean isOwnerEmail(String email) {
        return email != null && email.equalsIgnoreCase("owner@ecodana.com");
    }
}
