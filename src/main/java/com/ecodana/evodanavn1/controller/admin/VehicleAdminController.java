package com.ecodana.evodanavn1.controller.admin;

import com.ecodana.evodanavn1.model.User;
import com.ecodana.evodanavn1.model.Vehicle;
import com.ecodana.evodanavn1.service.UserService;
import com.ecodana.evodanavn1.service.VehicleService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/api/vehicles")
public class VehicleAdminController {

    @Autowired
    private VehicleService vehicleService;

    @Autowired
    private UserService userService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Approve vehicle - change status from PendingApproval to Available
     */
    @PostMapping("/{vehicleId}/approve")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> approveVehicle(
            @PathVariable String vehicleId,
            HttpSession session) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Check admin authentication
            User user = (User) session.getAttribute("currentUser");
            if (user == null || !userService.isAdmin(user)) {
                response.put("success", false);
                response.put("message", "Unauthorized");
                return ResponseEntity.status(401).body(response);
            }

            // Approve vehicle
            Vehicle vehicle = vehicleService.approveVehicle(vehicleId);
            
            response.put("success", true);
            response.put("message", "Vehicle approved successfully");
            response.put("vehicleId", vehicle.getVehicleId());
            response.put("status", vehicle.getStatus().name());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Reject vehicle - change status from PendingApproval to Unavailable
     */
    @PostMapping("/{vehicleId}/reject")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> rejectVehicle(
            @PathVariable String vehicleId,
            @RequestBody Map<String, String> requestBody,
            HttpSession session) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Check admin authentication
            User user = (User) session.getAttribute("currentUser");
            if (user == null || !userService.isAdmin(user)) {
                response.put("success", false);
                response.put("message", "Unauthorized");
                return ResponseEntity.status(401).body(response);
            }

            String reason = requestBody.get("reason");
            if (reason == null || reason.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Rejection reason is required");
                return ResponseEntity.badRequest().body(response);
            }

            // Reject vehicle
            Vehicle vehicle = vehicleService.rejectVehicle(vehicleId, reason);
            
            response.put("success", true);
            response.put("message", "Vehicle rejected successfully");
            response.put("vehicleId", vehicle.getVehicleId());
            response.put("status", vehicle.getStatus().name());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Delete vehicle
     */
    @DeleteMapping("/{vehicleId}/delete")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteVehicle(
            @PathVariable String vehicleId,
            HttpSession session) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Check admin authentication
            User user = (User) session.getAttribute("currentUser");
            if (user == null || !userService.isAdmin(user)) {
                response.put("success", false);
                response.put("message", "Unauthorized");
                return ResponseEntity.status(401).body(response);
            }

            // Delete vehicle
            vehicleService.deleteVehicle(vehicleId);
            
            response.put("success", true);
            response.put("message", "Vehicle deleted successfully");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Get vehicle detail
     */
    @GetMapping("/{vehicleId}/detail")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getVehicleDetail(
            @PathVariable String vehicleId,
            HttpSession session) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Check admin authentication
            User user = (User) session.getAttribute("currentUser");
            if (user == null || !userService.isAdmin(user)) {
                response.put("success", false);
                response.put("message", "Unauthorized");
                return ResponseEntity.status(401).body(response);
            }

            // Get vehicle
            Vehicle vehicle = vehicleService.getVehicleById(vehicleId)
                    .orElseThrow(() -> new RuntimeException("Vehicle not found"));
            
            // Build vehicle detail map
            Map<String, Object> vehicleDetail = new HashMap<>();
            vehicleDetail.put("vehicleId", vehicle.getVehicleId());
            vehicleDetail.put("vehicleModel", vehicle.getVehicleModel());
            vehicleDetail.put("licensePlate", vehicle.getLicensePlate());
            vehicleDetail.put("vehicleType", vehicle.getVehicleType().name());
            vehicleDetail.put("seats", vehicle.getSeats());
            vehicleDetail.put("yearManufactured", vehicle.getYearManufactured());
            vehicleDetail.put("odometer", vehicle.getOdometer());
            vehicleDetail.put("transmission", vehicle.getTransmissionType() != null ? 
                    vehicle.getTransmissionType().getTransmissionTypeName() : "N/A");
            vehicleDetail.put("batteryCapacity", vehicle.getBatteryCapacity());
            vehicleDetail.put("status", vehicle.getStatus().name());
            vehicleDetail.put("dailyPrice", vehicle.getDailyPriceFromJson());
            vehicleDetail.put("mainImageUrl", vehicle.getMainImageUrl());
            
            // Parse image URLs
            try {
                if (vehicle.getImageUrls() != null && !vehicle.getImageUrls().isEmpty()) {
                    List<String> imageUrls = objectMapper.readValue(
                            vehicle.getImageUrls(), 
                            objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)
                    );
                    vehicleDetail.put("imageUrls", imageUrls);
                }
            } catch (Exception e) {
                vehicleDetail.put("imageUrls", List.of());
            }
            
            // Parse features
            try {
                if (vehicle.getFeatures() != null && !vehicle.getFeatures().isEmpty()) {
                    List<String> features = objectMapper.readValue(
                            vehicle.getFeatures(), 
                            objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)
                    );
                    vehicleDetail.put("features", features);
                }
            } catch (Exception e) {
                vehicleDetail.put("features", List.of());
            }
            
            vehicleDetail.put("description", vehicle.getDescription());
            
            response.put("success", true);
            response.put("vehicle", vehicleDetail);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}
