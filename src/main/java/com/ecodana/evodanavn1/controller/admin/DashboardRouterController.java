package com.ecodana.evodanavn1.controller.admin;

import com.ecodana.evodanavn1.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ecodana.evodanavn1.service.UserService;

import jakarta.servlet.http.HttpSession;

@Controller
public class DashboardRouterController {

    @Autowired
    private UserService userService;

    /**
     * Main dashboard route - redirects to appropriate dashboard based on user role
     */
    @GetMapping("/dashboard-router")
    public String dashboardRouter(HttpSession session, RedirectAttributes redirectAttributes) {
        User currentUser = (User) session.getAttribute("currentUser");
        
        if (currentUser == null) {
            redirectAttributes.addFlashAttribute("error", "Please log in to access dashboard.");
            return "redirect:/login";
        }
        
        // Reload user with role information to ensure it's up to date
        User userWithRole = userService.getUserWithRole(currentUser.getEmail());
        if (userWithRole == null) {
            redirectAttributes.addFlashAttribute("error", "Unable to load user information. Please try again.");
            return "redirect:/login";
        }
        
        // Update session with fresh user data
        session.setAttribute("currentUser", userWithRole);
        
        // Redirect based on role
        if (userService.isAdmin(userWithRole)) {
            return "redirect:/admin";
        } else if (userService.isStaff(userWithRole)) {
            return "redirect:/staff";
        } else if (userService.isCustomer(userWithRole)) {
            return "redirect:/";  // Customer goes to home page
        } else {
            // Default to home page if role is unknown
            redirectAttributes.addFlashAttribute("warning", "Unknown user role. Redirecting to home page.");
            return "redirect:/";
        }
    }
    
    /**
     * Customer dashboard
     */
    @GetMapping("/dashboard/customer")
    public String customerDashboard(HttpSession session, RedirectAttributes redirectAttributes) {
        User currentUser = (User) session.getAttribute("currentUser");
        
        if (currentUser == null) {
            redirectAttributes.addFlashAttribute("error", "Please log in to access dashboard.");
            return "redirect:/login";
        }
        
        // Check if user has customer role
        if (!userService.isCustomer(currentUser)) {
            redirectAttributes.addFlashAttribute("error", "Access denied. Customer role required.");
            return "redirect:/login";
        }
        
        return "redirect:/dashboard";
    }
    
    /**
     * Staff dashboard
     */
    @GetMapping("/dashboard/staff")
    public String staffDashboard(HttpSession session, RedirectAttributes redirectAttributes) {
        User currentUser = (User) session.getAttribute("currentUser");
        
        if (currentUser == null) {
            redirectAttributes.addFlashAttribute("error", "Please log in to access dashboard.");
            return "redirect:/login";
        }
        
        // Check if user has staff role
        if (!userService.isStaff(currentUser)) {
            redirectAttributes.addFlashAttribute("error", "Access denied. Staff role required.");
            return "redirect:/login";
        }
        
        return "redirect:/staff";
    }
    
    /**
     * Admin dashboard
     */
    @GetMapping("/dashboard/admin")
    public String adminDashboard(HttpSession session, RedirectAttributes redirectAttributes) {
        User currentUser = (User) session.getAttribute("currentUser");
        
        if (currentUser == null) {
            redirectAttributes.addFlashAttribute("error", "Please log in to access dashboard.");
            return "redirect:/login";
        }
        
        // Check if user has admin role
        if (!userService.isAdmin(currentUser)) {
            redirectAttributes.addFlashAttribute("error", "Access denied. Admin role required.");
            return "redirect:/login";
        }
        
        return "redirect:/admin";
    }
}
