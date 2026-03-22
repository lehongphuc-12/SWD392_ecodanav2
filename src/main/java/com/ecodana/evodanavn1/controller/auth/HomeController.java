package com.ecodana.evodanavn1.controller.auth;

import com.ecodana.evodanavn1.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.ecodana.evodanavn1.service.UserService;
import com.ecodana.evodanavn1.service.VehicleService;

import jakarta.servlet.http.HttpSession;

@Controller
public class HomeController{

    @Autowired
    private VehicleService vehicleService;
    
    @Autowired
    private UserService userService;

    @GetMapping("/")
    public String home(Model model, HttpSession session) {
        // Get current user from session
        User currentUser = (User) session.getAttribute("currentUser");
        
        // If user is logged in, reload user with role information
        if (currentUser != null) {
            User userWithRole = userService.getUserWithRole(currentUser.getEmail());
            if (userWithRole != null) {
                currentUser = userWithRole;
                session.setAttribute("currentUser", currentUser);
            }
        }
        
        // Add currentUser to model (null if not logged in)
        model.addAttribute("currentUser", currentUser);
        
        // Get featured vehicles
        var allVehicles = vehicleService.getAllVehicles();
        var featuredVehicles = allVehicles.size() >= 3 ? allVehicles.subList(0, 3) : allVehicles;
        model.addAttribute("vehicles", featuredVehicles);
        
        return "auth/home";
    }





}