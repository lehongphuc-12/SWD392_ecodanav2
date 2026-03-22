package com.ecodana.evodanavn1.security;

import java.io.IOException;

import com.ecodana.evodanavn1.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.ecodana.evodanavn1.service.UserService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    @Autowired
    private UserService userService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, 
                                      Authentication authentication) throws IOException, ServletException {
        
        String username = authentication.getName();
        System.out.println("Authentication successful for: " + username);
        
        // Find user by username or email
        User user = userService.findByUsername(username);
        if (user == null) {
            user = userService.findByEmail(username);
        }
        
        if (user != null) {
            // Reload user with role information
            User userWithRole = userService.getUserWithRole(user.getEmail());
            if (userWithRole != null) {
                // Set user in session
                HttpSession session = request.getSession(true);
                session.setAttribute("currentUser", userWithRole);
                System.out.println("User set in session: " + userWithRole.getEmail());
                System.out.println("User role: " + userWithRole.getRoleName());
                
                // Redirect based on role
                String roleName = userWithRole.getRoleName();
                if ("Admin".equalsIgnoreCase(roleName)) {
                    session.setAttribute("flash_success", "🎉 Đăng nhập thành công! Chào mừng Admin " + userWithRole.getFirstName() + "! Bạn có quyền truy cập đầy đủ hệ thống.");
                    response.sendRedirect("/admin");
                } else if ("Owner".equalsIgnoreCase(roleName)) {
                    session.setAttribute("flash_success", "🎉 Đăng nhập thành công! Chào mừng Owner " + userWithRole.getFirstName() + "! Bạn có thể quản lý xe và đặt chỗ.");
                    response.sendRedirect("/owner/dashboard");
                } else if ("Staff".equalsIgnoreCase(roleName)) {
                    session.setAttribute("flash_success", "🎉 Đăng nhập thành công! Chào mừng Staff " + userWithRole.getFirstName() + "! Bạn có thể quản lý xe và đặt chỗ.");
                    response.sendRedirect("/staff");
                } else if ("Customer".equalsIgnoreCase(roleName)) {
                    session.setAttribute("flash_success", "🎉 Đăng nhập thành công! Chào mừng " + userWithRole.getFirstName() + "! Hãy khám phá và đặt xe ngay.");
                    response.sendRedirect("/");
                } else {
                    session.setAttribute("flash_success", "🎉 Đăng nhập thành công! Chào mừng " + userWithRole.getFirstName() + " trở lại ecodana.");
                    response.sendRedirect("/");
                }
                return;
            }
        }
        
        // Fallback to home page if user not found
        HttpSession session = request.getSession(true);
        session.setAttribute("flash_success", "🎉 Đăng nhập thành công! Chào mừng trở lại ecodana.");
        response.sendRedirect("/");
    }
}
