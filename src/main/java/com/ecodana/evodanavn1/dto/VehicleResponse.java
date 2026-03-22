package com.ecodana.evodanavn1.dto;

import com.ecodana.evodanavn1.model.Vehicle;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class VehicleResponse {
    private String vehicleId;
    private String vehicleModel;
    private Integer yearManufactured;
    private String licensePlate;
    private Integer seats;
    private Integer odometer;
    private BigDecimal hourlyPrice;
    private BigDecimal dailyPrice;
    private BigDecimal monthlyPrice;
    private String status;
    private String description;
    private String vehicleType;
    private Boolean requiresLicense;
    private BigDecimal batteryCapacity;
    private LocalDateTime createdDate;
    private String categoryName;
    private Integer categoryId;
    private String transmissionTypeName;
    private Integer transmissionTypeId;
    private String mainImageUrl;
    private List<String> imageUrls;
    private List<String> features;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    // Constructors
    public VehicleResponse() {}

    public VehicleResponse(Vehicle vehicle) {
        this.vehicleId = vehicle.getVehicleId();
        this.vehicleModel = vehicle.getVehicleModel();
        this.yearManufactured = vehicle.getYearManufactured();
        this.licensePlate = vehicle.getLicensePlate();
        this.seats = vehicle.getSeats();
        this.odometer = vehicle.getOdometer();
        this.status = vehicle.getStatus() != null ? vehicle.getStatus().name() : null;
        this.description = vehicle.getDescription();
        this.vehicleType = vehicle.getVehicleType() != null ? vehicle.getVehicleType().name() : null;
        this.requiresLicense = vehicle.getRequiresLicense();
        this.batteryCapacity = vehicle.getBatteryCapacity();
        this.createdDate = vehicle.getCreatedDate();
        this.mainImageUrl = vehicle.getMainImageUrl();

        // Parse rental prices from JSON
        if (vehicle.getRentalPrices() != null && !vehicle.getRentalPrices().isEmpty()) {
            try {
                Map<String, Object> prices = objectMapper.readValue(vehicle.getRentalPrices(), new TypeReference<Map<String, Object>>() {});
                this.hourlyPrice = prices.get("hourly") != null ? new BigDecimal(prices.get("hourly").toString()) : BigDecimal.ZERO;
                this.dailyPrice = prices.get("daily") != null ? new BigDecimal(prices.get("daily").toString()) : BigDecimal.ZERO;
                this.monthlyPrice = prices.get("monthly") != null ? new BigDecimal(prices.get("monthly").toString()) : BigDecimal.ZERO;
            } catch (Exception e) {
                this.hourlyPrice = BigDecimal.ZERO;
                this.dailyPrice = BigDecimal.ZERO;
                this.monthlyPrice = BigDecimal.ZERO;
            }
        }

        // Parse image URLs from JSON
        if (vehicle.getImageUrls() != null && !vehicle.getImageUrls().isEmpty()) {
            try {
                this.imageUrls = objectMapper.readValue(vehicle.getImageUrls(), new TypeReference<List<String>>() {});
            } catch (Exception e) {
                this.imageUrls = new ArrayList<>();
            }
        } else {
            this.imageUrls = new ArrayList<>();
        }

        // Parse features from JSON
        if (vehicle.getFeatures() != null && !vehicle.getFeatures().isEmpty()) {
            try {
                this.features = objectMapper.readValue(vehicle.getFeatures(), new TypeReference<List<String>>() {});
            } catch (Exception e) {
                this.features = new ArrayList<>();
            }
        } else {
            this.features = new ArrayList<>();
        }

        // Set category information
        if (vehicle.getCategory() != null) {
            this.categoryId = vehicle.getCategory().getCategoryId();
            this.categoryName = vehicle.getCategory().getCategoryName();
        }

        // Set transmission type information
        if (vehicle.getTransmissionType() != null) {
            this.transmissionTypeId = vehicle.getTransmissionType().getTransmissionTypeId();
            this.transmissionTypeName = vehicle.getTransmissionType().getTransmissionTypeName();
        }
    }

    // Getters and Setters
    public String getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(String vehicleId) {
        this.vehicleId = vehicleId;
    }

    public String getVehicleModel() {
        return vehicleModel;
    }

    public void setVehicleModel(String vehicleModel) {
        this.vehicleModel = vehicleModel;
    }

    public Integer getYearManufactured() {
        return yearManufactured;
    }

    public void setYearManufactured(Integer yearManufactured) {
        this.yearManufactured = yearManufactured;
    }

    public String getLicensePlate() {
        return licensePlate;
    }

    public void setLicensePlate(String licensePlate) {
        this.licensePlate = licensePlate;
    }

    public Integer getSeats() {
        return seats;
    }

    public void setSeats(Integer seats) {
        this.seats = seats;
    }

    public Integer getOdometer() {
        return odometer;
    }

    public void setOdometer(Integer odometer) {
        this.odometer = odometer;
    }

    public BigDecimal getHourlyPrice() {
        return hourlyPrice;
    }

    public void setHourlyPrice(BigDecimal hourlyPrice) {
        this.hourlyPrice = hourlyPrice;
    }

    public BigDecimal getDailyPrice() {
        return dailyPrice;
    }

    public void setDailyPrice(BigDecimal dailyPrice) {
        this.dailyPrice = dailyPrice;
    }

    public BigDecimal getMonthlyPrice() {
        return monthlyPrice;
    }

    public void setMonthlyPrice(BigDecimal monthlyPrice) {
        this.monthlyPrice = monthlyPrice;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(String vehicleType) {
        this.vehicleType = vehicleType;
    }

    public Boolean getRequiresLicense() {
        return requiresLicense;
    }

    public void setRequiresLicense(Boolean requiresLicense) {
        this.requiresLicense = requiresLicense;
    }

    public BigDecimal getBatteryCapacity() {
        return batteryCapacity;
    }

    public void setBatteryCapacity(BigDecimal batteryCapacity) {
        this.batteryCapacity = batteryCapacity;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public Integer getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Integer categoryId) {
        this.categoryId = categoryId;
    }

    public String getTransmissionTypeName() {
        return transmissionTypeName;
    }

    public void setTransmissionTypeName(String transmissionTypeName) {
        this.transmissionTypeName = transmissionTypeName;
    }

    public Integer getTransmissionTypeId() {
        return transmissionTypeId;
    }

    public void setTransmissionTypeId(Integer transmissionTypeId) {
        this.transmissionTypeId = transmissionTypeId;
    }

    public String getMainImageUrl() {
        return mainImageUrl;
    }

    public void setMainImageUrl(String mainImageUrl) {
        this.mainImageUrl = mainImageUrl;
    }

    public List<String> getImageUrls() {
        return imageUrls;
    }

    public void setImageUrls(List<String> imageUrls) {
        this.imageUrls = imageUrls;
    }

    public List<String> getFeatures() {
        return features;
    }

    public void setFeatures(List<String> features) {
        this.features = features;
    }
}
