package com.ecodana.evodanavn1.controller.customer;

import com.ecodana.evodanavn1.model.UserFavoriteVehicles;
import com.ecodana.evodanavn1.model.User;
import com.ecodana.evodanavn1.model.Vehicle;
import com.ecodana.evodanavn1.service.FavoriteService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/favorites")
public class FavoriteController {

    @Autowired
    private FavoriteService favoriteService;


    @PostMapping("/toggle/{vehicleId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> toggleFavorite(@PathVariable String vehicleId, HttpSession session) {
        Map<String, Object> res = new HashMap<>();
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            res.put("success", false);
            res.put("message", "Vui lòng đăng nhập");
            return ResponseEntity.status(401).body(res);
        }
        try {
            boolean favorited = favoriteService.toggleFavorite(currentUser, vehicleId);
            res.put("success", true);
            res.put("favorited", favorited);
            return ResponseEntity.ok(res);
        } catch (IllegalArgumentException e) {
            res.put("success", false);
            res.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(res);
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", "Error: " + e.getMessage());
            return ResponseEntity.internalServerError().body(res);
        }
    }

    @GetMapping
    public String listFavorites(HttpSession session, Model model) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login";
        }
        model.addAttribute("currentUser", currentUser);
        List<UserFavoriteVehicles> favorites = favoriteService.getFavorites(currentUser);
        List<Vehicle> vehicles = favorites.stream().map(UserFavoriteVehicles::getVehicle).collect(Collectors.toList());
        model.addAttribute("vehicles", vehicles);
        return "customer/favorites";
    }
}


