package com.ecodana.evodanavn1.service;

import com.ecodana.evodanavn1.dto.VehicleRequest;
import com.ecodana.evodanavn1.dto.VehicleResponse;
import com.ecodana.evodanavn1.model.TransmissionType;
import com.ecodana.evodanavn1.model.User;
import com.ecodana.evodanavn1.model.Vehicle;
import com.ecodana.evodanavn1.model.VehicleCategories;
import com.ecodana.evodanavn1.repository.TransmissionTypeRepository;
import com.ecodana.evodanavn1.repository.VehicleCategoryRepository;
import com.ecodana.evodanavn1.repository.VehicleRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class VehicleService {

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private VehicleCategoryRepository vehicleCategoryRepository;

    @Autowired
    private TransmissionTypeRepository transmissionTypeRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<Vehicle> getAllVehicles() {
        return vehicleRepository.findAll();
    }

    public Optional<Vehicle> getVehicleById(String id) {
        return vehicleRepository.findById(id);
    }

    public List<Vehicle> getAvailableVehicles() {
        return vehicleRepository.findAvailableVehicles();
    }

    public List<Vehicle> getVehiclesByOwnerId(String ownerId) {
        return vehicleRepository.findByOwnerId(ownerId);
    }

    public List<Vehicle> getVehiclesByType(Vehicle.VehicleType vehicleType) {
        return vehicleRepository.findByVehicleType(vehicleType);
    }

    public List<Vehicle> getVehiclesByCategory(Integer categoryId) {
        return vehicleRepository.findByCategory_CategoryId(categoryId);
    }

    public List<Vehicle> getVehiclesByTransmissionType(Integer transmissionTypeId) {
        return vehicleRepository.findByTransmissionType_TransmissionTypeId(transmissionTypeId);
    }

    public List<Vehicle> getVehiclesByPriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        return vehicleRepository.findByPriceRange(minPrice, maxPrice);
    }

    public List<Vehicle> getVehiclesBySeats(Integer seats) {
        return vehicleRepository.findBySeats(seats);
    }

    public List<Vehicle> getVehiclesByLicenseRequirement(Boolean requiresLicense) {
        return vehicleRepository.findByRequiresLicense(requiresLicense);
    }

    public Vehicle saveVehicle(Vehicle vehicle) {
        return vehicleRepository.save(vehicle);
    }

    public Vehicle updateVehicle(Vehicle vehicle) {
        return vehicleRepository.save(vehicle);
    }

    public void deleteVehicle(String vehicleId) {
        vehicleRepository.deleteById(vehicleId);
    }

    public List<Vehicle> getFavoriteVehiclesByUser(User user) {
        return vehicleRepository.findAvailableVehicles().stream().limit(2).collect(Collectors.toList());
    }

    public BigDecimal getDailyPrice(Vehicle vehicle) {
        try {
            Map<String, Object> prices = objectMapper.readValue(vehicle.getRentalPrices(), new TypeReference<>() {});
            Object dailyPrice = prices.get("daily");
            if (dailyPrice instanceof Number) {
                return new BigDecimal(((Number) dailyPrice).toString());
            }
        } catch (Exception e) {
            // Log error
        }
        return BigDecimal.ZERO;
    }

    public Map<String, Object> getVehicleStatistics() {
        Map<String, Object> stats = new HashMap<>();
        List<Vehicle> allVehicles = getAllVehicles();
        stats.put("totalVehicles", allVehicles.size());
        stats.put("availableVehicles", getAvailableVehicles().size());
        stats.put("inUseVehicles", allVehicles.stream().filter(v -> v.getStatus() == Vehicle.VehicleStatus.Rented).count());
        stats.put("maintenanceVehicles", allVehicles.stream().filter(v -> v.getStatus() == Vehicle.VehicleStatus.Maintenance).count());
        return stats;
    }

    public List<Vehicle> getVehiclesByStatus(String status) {
        return vehicleRepository.findByStatus(status);
    }

    public Vehicle updateVehicleStatus(String vehicleId, String status) {
        return vehicleRepository.findById(vehicleId)
                .map(vehicle -> {
                    vehicle.setStatus(Vehicle.VehicleStatus.valueOf(status));
                    return vehicleRepository.save(vehicle);
                })
                .orElse(null);
    }

    public List<Vehicle> searchVehicles(String keyword) {
        return vehicleRepository.searchVehicles(keyword);
    }

    public Map<String, Object> getVehicleAnalytics() {
        Map<String, Object> analytics = new HashMap<>();
        analytics.put("statusDistribution", vehicleRepository.findStatusDistribution());
        analytics.put("categoryDistribution", vehicleRepository.findCategoryDistribution());
        return analytics;
    }

    private boolean isNullOrEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    public List<Vehicle> filterVehicles(String location, String pickupDate, String returnDate, String pickupTime, String returnTime, String category, String vehicleType, String budget, Integer seats, Boolean requiresLicense) {
        // Bắt đầu bằng việc chỉ lấy các xe có sẵn (Available)
        Stream<Vehicle> vehicleStream = getAvailableVehicles().stream();

        // TODO: Implement location-based filtering.

        if (!isNullOrEmpty(vehicleType)) {
            vehicleStream = vehicleStream.filter(vehicle -> vehicleType.equals(vehicle.getVehicleType().name()));
        }

        if (!isNullOrEmpty(category)) {
            vehicleStream = vehicleStream.filter(vehicle -> vehicle.getCategory() != null && category.equalsIgnoreCase(vehicle.getCategory().getCategoryName()));
        }

        if (!isNullOrEmpty(budget)) {
            vehicleStream = vehicleStream.filter(vehicle -> {
                BigDecimal dailyPrice = vehicle.getDailyPriceFromJson();
                if ("under500k".equals(budget)) {
                    return dailyPrice.compareTo(new BigDecimal("500000")) < 0;
                } else if ("over500k".equals(budget)) {
                    return dailyPrice.compareTo(new BigDecimal("500000")) >= 0;
                }
                return true;
            });
        }

        if (seats != null && seats > 0) {
            vehicleStream = vehicleStream.filter(vehicle -> seats.equals(vehicle.getSeats()));
        }

        if (requiresLicense != null) {
            vehicleStream = vehicleStream.filter(vehicle -> requiresLicense.equals(vehicle.getRequiresLicense()));
        }

        return vehicleStream.collect(Collectors.toList());
    }

    // Admin-specific methods
    @Transactional
    public VehicleResponse createVehicle(VehicleRequest request) {
        Vehicle vehicle = new Vehicle();
        vehicle.setVehicleId(UUID.randomUUID().toString());
        vehicle.setVehicleModel(request.getVehicleModel());
        vehicle.setYearManufactured(request.getYearManufactured());
        vehicle.setLicensePlate(request.getLicensePlate());
        vehicle.setSeats(request.getSeats());
        vehicle.setOdometer(request.getOdometer());
        vehicle.setStatus(request.getStatus() != null ? Vehicle.VehicleStatus.valueOf(request.getStatus()) : Vehicle.VehicleStatus.Available);
        vehicle.setDescription(request.getDescription());
        vehicle.setVehicleType(request.getVehicleType() != null ? Vehicle.VehicleType.valueOf(request.getVehicleType()) : null);
        vehicle.setRequiresLicense(request.getRequiresLicense() != null ? request.getRequiresLicense() : true);
        vehicle.setBatteryCapacity(request.getBatteryCapacity());
        vehicle.setMainImageUrl(request.getMainImageUrl());
        vehicle.setCreatedDate(LocalDateTime.now());

        // Set rental prices as JSON
        try {
            Map<String, Object> prices = new HashMap<>();
            prices.put("hourly", request.getHourlyPrice() != null ? request.getHourlyPrice() : BigDecimal.ZERO);
            prices.put("daily", request.getDailyPrice() != null ? request.getDailyPrice() : BigDecimal.ZERO);
            prices.put("monthly", request.getMonthlyPrice() != null ? request.getMonthlyPrice() : BigDecimal.ZERO);
            vehicle.setRentalPrices(objectMapper.writeValueAsString(prices));
        } catch (Exception e) {
            throw new RuntimeException("Error setting rental prices: " + e.getMessage());
        }

        // Set image URLs as JSON
        if (request.getImageUrls() != null && !request.getImageUrls().isEmpty()) {
            try {
                vehicle.setImageUrls(objectMapper.writeValueAsString(request.getImageUrls()));
            } catch (Exception e) {
                throw new RuntimeException("Error setting image URLs: " + e.getMessage());
            }
        }

        // Set features as JSON
        if (request.getFeatures() != null && !request.getFeatures().isEmpty()) {
            try {
                vehicle.setFeatures(objectMapper.writeValueAsString(request.getFeatures()));
            } catch (Exception e) {
                throw new RuntimeException("Error setting features: " + e.getMessage());
            }
        }

        // Set category
        if (request.getCategoryId() != null) {
            vehicleCategoryRepository.findById(request.getCategoryId())
                    .ifPresent(vehicle::setCategory);
        }

        // Set transmission type
        if (request.getTransmissionTypeId() != null) {
            transmissionTypeRepository.findById(request.getTransmissionTypeId())
                    .ifPresent(vehicle::setTransmissionType);
        }

        Vehicle savedVehicle = vehicleRepository.save(vehicle);
        return new VehicleResponse(savedVehicle);
    }

    @Transactional
    public VehicleResponse updateVehicleById(String vehicleId, VehicleRequest request) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new RuntimeException("Vehicle not found"));

        vehicle.setVehicleModel(request.getVehicleModel());
        vehicle.setYearManufactured(request.getYearManufactured());
        vehicle.setLicensePlate(request.getLicensePlate());
        vehicle.setSeats(request.getSeats());
        vehicle.setOdometer(request.getOdometer());
        vehicle.setStatus(request.getStatus() != null ? Vehicle.VehicleStatus.valueOf(request.getStatus()) : vehicle.getStatus());
        vehicle.setDescription(request.getDescription());
        vehicle.setVehicleType(request.getVehicleType() != null ? Vehicle.VehicleType.valueOf(request.getVehicleType()) : vehicle.getVehicleType());
        vehicle.setRequiresLicense(request.getRequiresLicense());
        vehicle.setBatteryCapacity(request.getBatteryCapacity());
        vehicle.setMainImageUrl(request.getMainImageUrl());

        // Update rental prices as JSON
        try {
            Map<String, Object> prices = new HashMap<>();
            prices.put("hourly", request.getHourlyPrice() != null ? request.getHourlyPrice() : BigDecimal.ZERO);
            prices.put("daily", request.getDailyPrice() != null ? request.getDailyPrice() : BigDecimal.ZERO);
            prices.put("monthly", request.getMonthlyPrice() != null ? request.getMonthlyPrice() : BigDecimal.ZERO);
            vehicle.setRentalPrices(objectMapper.writeValueAsString(prices));
        } catch (Exception e) {
            throw new RuntimeException("Error updating rental prices: " + e.getMessage());
        }

        // Update image URLs as JSON
        if (request.getImageUrls() != null) {
            try {
                vehicle.setImageUrls(objectMapper.writeValueAsString(request.getImageUrls()));
            } catch (Exception e) {
                throw new RuntimeException("Error updating image URLs: " + e.getMessage());
            }
        }

        // Update features as JSON
        if (request.getFeatures() != null) {
            try {
                vehicle.setFeatures(objectMapper.writeValueAsString(request.getFeatures()));
            } catch (Exception e) {
                throw new RuntimeException("Error updating features: " + e.getMessage());
            }
        }

        // Update category
        if (request.getCategoryId() != null) {
            vehicleCategoryRepository.findById(request.getCategoryId())
                    .ifPresent(vehicle::setCategory);
        }

        // Update transmission type
        if (request.getTransmissionTypeId() != null) {
            transmissionTypeRepository.findById(request.getTransmissionTypeId())
                    .ifPresent(vehicle::setTransmissionType);
        }

        Vehicle updatedVehicle = vehicleRepository.save(vehicle);
        return new VehicleResponse(updatedVehicle);
    }

    public VehicleResponse getVehicleResponseById(String vehicleId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new RuntimeException("Vehicle not found"));
        return new VehicleResponse(vehicle);
    }

    public List<VehicleResponse> getAllVehicleResponses() {
        return vehicleRepository.findAll().stream()
                .map(VehicleResponse::new)
                .collect(Collectors.toList());
    }

    public List<VehicleCategories> getAllCategories() {
        return vehicleCategoryRepository.findAll();
    }

    public List<TransmissionType> getAllTransmissionTypes() {
        return transmissionTypeRepository.findAll();
    }

    public boolean vehicleExistsByLicensePlate(String licensePlate) {
        return vehicleRepository.existsByLicensePlate(licensePlate);
    }

    public boolean vehicleExistsByLicensePlateAndNotId(String licensePlate, String vehicleId) {
        Optional<Vehicle> vehicle = vehicleRepository.findByLicensePlate(licensePlate);
        return vehicle.isPresent() && !vehicle.get().getVehicleId().equals(vehicleId);
    }

    @Autowired
    private com.ecodana.evodanavn1.service.UserService userService;
    
    @Autowired
    private com.ecodana.evodanavn1.repository.RoleRepository roleRepository;

    /**
     * Approve vehicle - change status to Available
     * Can approve from PendingApproval, Unavailable, or Maintenance status
     * Also grants Owner role to the vehicle owner if they don't have it
     */
    @Transactional
    public Vehicle approveVehicle(String vehicleId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new RuntimeException("Vehicle not found"));
        
        // Log current status for debugging
        System.out.println("=== Approving Vehicle ===");
        System.out.println("Vehicle ID: " + vehicleId);
        System.out.println("Current Status: " + vehicle.getStatus());
        
        // Only prevent approval if already Available or Rented
        if (vehicle.getStatus() == Vehicle.VehicleStatus.Available) {
            throw new RuntimeException("Vehicle is already approved and available");
        }
        
        if (vehicle.getStatus() == Vehicle.VehicleStatus.Rented) {
            throw new RuntimeException("Cannot approve vehicle that is currently rented");
        }
        
        // Grant Owner role to vehicle owner if they don't have it
        if (vehicle.getOwnerId() != null) {
            try {
                com.ecodana.evodanavn1.model.User owner = userService.findById(vehicle.getOwnerId());
                if (owner != null && !userService.isOwner(owner) && !userService.isAdmin(owner)) {
                    // Find Owner role
                    com.ecodana.evodanavn1.model.Role ownerRole = roleRepository.findByRoleName("Owner")
                            .orElseThrow(() -> new RuntimeException("Owner role not found"));
                    
                    // Grant Owner role
                    owner.setRoleId(ownerRole.getRoleId());
                    userService.save(owner);
                    System.out.println("Granted Owner role to user: " + owner.getUsername());
                }
            } catch (Exception e) {
                System.err.println("Failed to grant Owner role: " + e.getMessage());
                // Don't fail the approval if role grant fails
            }
        }
        
        vehicle.setStatus(Vehicle.VehicleStatus.Available);
        Vehicle saved = vehicleRepository.save(vehicle);
        System.out.println("New Status: " + saved.getStatus());
        return saved;
    }

    /**
     * Reject vehicle - change status to Unavailable
     */
    @Transactional
    public Vehicle rejectVehicle(String vehicleId, String reason) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new RuntimeException("Vehicle not found"));
        
        // Log current status for debugging
        System.out.println("=== Rejecting Vehicle ===");
        System.out.println("Vehicle ID: " + vehicleId);
        System.out.println("Current Status: " + vehicle.getStatus());
        System.out.println("Reason: " + reason);
        
        // Cannot reject if already rented
        if (vehicle.getStatus() == Vehicle.VehicleStatus.Rented) {
            throw new RuntimeException("Cannot reject vehicle that is currently rented");
        }
        
        vehicle.setStatus(Vehicle.VehicleStatus.Unavailable);
        Vehicle saved = vehicleRepository.save(vehicle);
        System.out.println("New Status: " + saved.getStatus());
        return saved;
    }
}
