package com.ecodana.evodanavn1.controller.customer;

import com.ecodana.evodanavn1.model.Discount;
import com.ecodana.evodanavn1.model.User;
import com.ecodana.evodanavn1.model.UserFeedback;
import com.ecodana.evodanavn1.service.UserFeedbackService;
import com.ecodana.evodanavn1.service.FavoriteService;
import com.ecodana.evodanavn1.service.UserService;
import com.ecodana.evodanavn1.service.DiscountService;
import com.ecodana.evodanavn1.model.Vehicle;
import com.ecodana.evodanavn1.service.VehicleService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class VehicleController {

    private final VehicleService vehicleService;

    @Autowired
    private UserFeedbackService userFeedbackService;

    @Autowired
    private FavoriteService favoriteService;

    @Autowired
    private UserService userService;

    @Autowired
    private DiscountService discountService;

    // Inject the Google API key from application.properties
    @Value("${google.api.key}")
    private String googleApiKey;

    @Autowired
    public VehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    @GetMapping("/vehicles")
    public String listVehicles(@RequestParam(required = false) String location,
                               @RequestParam(required = false) String pickupDate,
                               @RequestParam(required = false) String returnDate,
                               @RequestParam(required = false) String pickupTime,
                               @RequestParam(required = false) String returnTime,
                               @RequestParam(required = false) String category,
                               @RequestParam(required = false) String vehicleType,
                               @RequestParam(required = false) String budget,
                               @RequestParam(required = false) Integer seats,
                               @RequestParam(required = false) Boolean requiresLicense,
                               Model model, HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser != null) {
            model.addAttribute("currentUser", currentUser);
        }

        List<Vehicle> vehicles = vehicleService.filterVehicles(location, pickupDate, returnDate, pickupTime, returnTime, category, vehicleType, budget, seats, requiresLicense);
        model.addAttribute("vehicles", vehicles);

        // Calculate average rating and feedback count for each vehicle
        java.util.Map<String, java.util.Map<String, Object>> vehicleRatings = new java.util.HashMap<>();
        for (Vehicle vehicle : vehicles) {
            java.util.List<UserFeedback> feedbacks = userFeedbackService.getFeedbackByVehicle(vehicle);
            double averageRating = 0.0;
            int roundedRating = 0;
            if (!feedbacks.isEmpty()) {
                averageRating = feedbacks.stream().mapToInt(UserFeedback::getRating).average().orElse(0.0);
                roundedRating = (int) Math.round(averageRating);
            }
            java.util.Map<String, Object> ratingInfo = new java.util.HashMap<>();
            ratingInfo.put("averageRating", averageRating);
            ratingInfo.put("roundedRating", roundedRating);
            ratingInfo.put("feedbackCount", feedbacks.size());
            vehicleRatings.put(vehicle.getVehicleId(), ratingInfo);
        }
        model.addAttribute("vehicleRatings", vehicleRatings);

        // Add all filter parameters to the model to be used in the view
        model.addAttribute("selectedLocation", location);
        model.addAttribute("selectedPickupDate", pickupDate);
        model.addAttribute("selectedReturnDate", returnDate);
        model.addAttribute("selectedPickupTime", pickupTime);
        model.addAttribute("selectedReturnTime", returnTime);
        model.addAttribute("selectedCategory", category);
        model.addAttribute("selectedVehicleType", vehicleType);
        model.addAttribute("selectedBudget", budget);
        model.addAttribute("selectedSeats", seats);
        model.addAttribute("selectedRequiresLicense", requiresLicense);

        return "customer/vehicles";
    }

    @GetMapping("/vehicles/{id}")
    public String vehicleDetail(@PathVariable("id") String vehicleId, Model model, HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser != null) {
            model.addAttribute("currentUser", currentUser);
        }

        Vehicle vehicle = vehicleService.getVehicleById(vehicleId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy xe"));

        model.addAttribute("vehicle", vehicle);

        // Add the API key to the model to pass it to the view
        model.addAttribute("googleApiKey", googleApiKey);

        // Load vehicle owner information
        if (vehicle.getOwnerId() != null) {
            User owner = userService.findById(vehicle.getOwnerId());
            model.addAttribute("vehicleOwner", owner);
        }

        // Load feedbacks for this vehicle and compute average rating
        java.util.List<UserFeedback> feedbacks = userFeedbackService.getFeedbackByVehicle(vehicle);
        model.addAttribute("vehicleFeedbacks", feedbacks);
        double averageRating = 0.0;
        if (!feedbacks.isEmpty()) {
            averageRating = feedbacks.stream().mapToInt(UserFeedback::getRating).average().orElse(0.0);
        }
        model.addAttribute("averageRating", averageRating);
        model.addAttribute("feedbackCount", feedbacks.size());

        // Get related vehicles (same type, different model)
        List<Vehicle> relatedVehicles = vehicleService.getVehiclesByType(vehicle.getVehicleType())
                .stream()
                .filter(v -> !v.getVehicleId().equals(vehicleId) && Vehicle.VehicleStatus.Available.equals(v.getStatus()))
                .limit(3)
                .toList();
        model.addAttribute("relatedVehicles", relatedVehicles);

        // Favorite state
        try {
            boolean isFavorite = currentUser != null && favoriteService.isFavorite(currentUser, vehicle);
            model.addAttribute("isFavorite", isFavorite);
        } catch (Exception ignored) {}

        // Lấy danh sách các mã giảm giá có sẵn và đang hoạt động
        List<Discount> availableDiscounts = discountService.getAvailableDiscountsForCustomer();
        model.addAttribute("availableDiscounts", availableDiscounts);

        return "customer/vehicle-detail";
    }
}
