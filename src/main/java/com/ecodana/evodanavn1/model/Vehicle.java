package com.ecodana.evodanavn1.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.*;

@Entity
@Table(name = "Vehicle")
public class Vehicle {
    @Id
    @Column(name = "VehicleId", length = 36)
    private String vehicleId;

    @Column(name = "VehicleModel", length = 50, nullable = false)
    private String vehicleModel;

    @Column(name = "YearManufactured")
    private Integer yearManufactured;

    @Column(name = "LicensePlate", length = 20, nullable = false)
    private String licensePlate;

    @Column(name = "Seats")
    private Integer seats;

    @Column(name = "Odometer")
    private Integer odometer;

    @Column(name = "RentalPrices", columnDefinition = "JSON")
    private String rentalPrices;

    @Column(name = "Status", length = 20)
    @Enumerated(EnumType.STRING)
    private VehicleStatus status;

    @Column(name = "Description", length = 500)
    private String description;

    @Column(name = "VehicleType", length = 20)
    @Enumerated(EnumType.STRING)
    private VehicleType vehicleType;

    @Column(name = "RequiresLicense")
    private Boolean requiresLicense = true;

    @Column(name = "BatteryCapacity", precision = 10, scale = 2)
    private BigDecimal batteryCapacity;

    @Column(name = "CreatedDate", nullable = false)
    private LocalDateTime createdDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "LastUpdatedBy")
    private User lastUpdatedBy;

    @Column(name = "OwnerId", length = 36)
    private String ownerId; // ID của User là chủ sở hữu xe (P2P model)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CategoryId", referencedColumnName = "CategoryId")
    private VehicleCategories category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TransmissionTypeId", referencedColumnName = "TransmissionTypeId")
    private TransmissionType transmissionType;

    @Column(name = "MainImageUrl", length = 255)
    private String mainImageUrl;

    @Column(name = "ImageUrls", columnDefinition = "JSON")
    private String imageUrls; // JSON Array: ["url1", "url2"]

    @Column(name = "Features", columnDefinition = "JSON")
    private String features; // JSON Array: ["GPS", "Camera 360"]

    @Transient
    private static final ObjectMapper objectMapper = new ObjectMapper();

    // Constructors
    public Vehicle() {
        this.createdDate = LocalDateTime.now();
        this.status = VehicleStatus.Available;
    }

    @Transient
    public BigDecimal getDailyPriceFromJson() {
        if (this.rentalPrices == null || this.rentalPrices.isEmpty()) {
            return BigDecimal.ZERO;
        }
        try {
            Map<String, Object> prices = objectMapper.readValue(this.rentalPrices, new TypeReference<Map<String, Object>>() {});
            Object dailyPrice = prices.get("daily");
            if (dailyPrice instanceof Number) {
                return new BigDecimal(dailyPrice.toString());
            }
        } catch (Exception e) {
            System.err.println("Error parsing rentalPrices JSON for vehicle " + this.vehicleId + ": " + e.getMessage());
        }
        return BigDecimal.ZERO;
    }

    @Transient
    public BigDecimal getHourlyPriceFromJson() {
        if (this.rentalPrices == null || this.rentalPrices.isEmpty()) {
            return BigDecimal.ZERO;
        }
        try {
            Map<String, Object> prices = objectMapper.readValue(this.rentalPrices, new TypeReference<Map<String, Object>>() {});
            Object hourlyPrice = prices.get("hourly");
            if (hourlyPrice instanceof Number) {
                return new BigDecimal(hourlyPrice.toString());
            }
        } catch (Exception e) {
            System.err.println("Error parsing rentalPrices JSON for vehicle " + this.vehicleId + ": " + e.getMessage());
        }
        return BigDecimal.ZERO;
    }

    @Transient
    public BigDecimal getMonthlyPriceFromJson() {
        if (this.rentalPrices == null || this.rentalPrices.isEmpty()) {
            return BigDecimal.ZERO;
        }
        try {
            Map<String, Object> prices = objectMapper.readValue(this.rentalPrices, new TypeReference<Map<String, Object>>() {});
            Object monthlyPrice = prices.get("monthly");
            if (monthlyPrice instanceof Number) {
                return new BigDecimal(monthlyPrice.toString());
            }
        } catch (Exception e) {
            System.err.println("Error parsing rentalPrices JSON for vehicle " + this.vehicleId + ": " + e.getMessage());
        }
        return BigDecimal.ZERO;
    }

    @Transient
    public List<String> getImageUrlsFromJson() {
        if (this.imageUrls == null || this.imageUrls.isEmpty()) {
            return new java.util.ArrayList<>();
        }
        try {
            return objectMapper.readValue(this.imageUrls, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            System.err.println("Error parsing imageUrls JSON for vehicle " + this.vehicleId + ": " + e.getMessage());
        }
        return new java.util.ArrayList<>();
    }

    @Transient
    public List<String> getFeaturesFromJson() {
        if (this.features == null || this.features.isEmpty()) {
            return new java.util.ArrayList<>();
        }
        try {
            return objectMapper.readValue(this.features, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            System.err.println("Error parsing features JSON for vehicle " + this.vehicleId + ": " + e.getMessage());
        }
        return new java.util.ArrayList<>();
    }

    // Getters and Setters
    public String getVehicleId() { return vehicleId; }
    public void setVehicleId(String vehicleId) { this.vehicleId = vehicleId; }
    public String getVehicleModel() { return vehicleModel; }
    public void setVehicleModel(String vehicleModel) { this.vehicleModel = vehicleModel; }
    public Integer getYearManufactured() { return yearManufactured; }
    public void setYearManufactured(Integer yearManufactured) { this.yearManufactured = yearManufactured; }
    public String getLicensePlate() { return licensePlate; }
    public void setLicensePlate(String licensePlate) { this.licensePlate = licensePlate; }
    public Integer getSeats() { return seats; }
    public void setSeats(Integer seats) { this.seats = seats; }
    public Integer getOdometer() { return odometer; }
    public void setOdometer(Integer odometer) { this.odometer = odometer; }
    public String getRentalPrices() { return rentalPrices; }
    public void setRentalPrices(String rentalPrices) { this.rentalPrices = rentalPrices; }
    public VehicleStatus getStatus() { return status; }
    public void setStatus(VehicleStatus status) { this.status = status; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public VehicleType getVehicleType() { return vehicleType; }
    public void setVehicleType(VehicleType vehicleType) { this.vehicleType = vehicleType; }
    public Boolean getRequiresLicense() { return requiresLicense; }
    public void setRequiresLicense(Boolean requiresLicense) { this.requiresLicense = requiresLicense; }
    public BigDecimal getBatteryCapacity() { return batteryCapacity; }
    public void setBatteryCapacity(BigDecimal batteryCapacity) { this.batteryCapacity = batteryCapacity; }
    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }
    public User getLastUpdatedBy() { return lastUpdatedBy; }
    public void setLastUpdatedBy(User lastUpdatedBy) { this.lastUpdatedBy = lastUpdatedBy; }

    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

    public VehicleCategories getCategory() {
        return category;
    }

    public void setCategory(VehicleCategories category) {
        this.category = category;
    }

    public TransmissionType getTransmissionType() {
        return transmissionType;
    }

    public void setTransmissionType(TransmissionType transmissionType) {
        this.transmissionType = transmissionType;
    }

    public String getMainImageUrl() { return mainImageUrl; }
    public void setMainImageUrl(String mainImageUrl) { this.mainImageUrl = mainImageUrl; }
    public String getImageUrls() { return imageUrls; }
    public void setImageUrls(String imageUrls) { this.imageUrls = imageUrls; }
    public String getFeatures() { return features; }
    public void setFeatures(String features) { this.features = features; }

    public enum VehicleStatus {
        PendingApproval,  // Chờ admin duyệt (Trạng thái này có trong DB script nhưng thiếu ở đây)
        Available,        // Đã duyệt, sẵn sàng cho thuê
        Rented,          // Đang được thuê
        Maintenance,     // Đang bảo trì
        Unavailable      // Không khả dụng
    }

    public enum VehicleType {
        ElectricCar, ElectricMotorcycle
    }
}