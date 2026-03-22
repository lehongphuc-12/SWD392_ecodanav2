package com.ecodana.evodanavn1.dto;



import java.math.BigDecimal;

// Sử dụng Lombok để code ngắn gọn hơn (tùy chọn)
// import lombok.Data;
// @Data
public class BookingRequest {
    private String vehicleId;
    private String pickupDate;
    private String pickupTime;
    private String returnDate;
    private String returnTime;
    private String pickupLocation;
    private BigDecimal totalAmount;
    private BigDecimal vehicleRentalFee;
    private Integer rentalDays;
    private String discountId;
    private BigDecimal discountAmount;
    private String paymentMethod;

    // Thêm getters và setters nếu không dùng Lombok
    public String getVehicleId() { return vehicleId; }
    public void setVehicleId(String vehicleId) { this.vehicleId = vehicleId; }
    public String getPickupDate() { return pickupDate; }
    public void setPickupDate(String pickupDate) { this.pickupDate = pickupDate; }
    public String getPickupTime() { return pickupTime; }
    public void setPickupTime(String pickupTime) { this.pickupTime = pickupTime; }
    public String getReturnDate() { return returnDate; }
    public void setReturnDate(String returnDate) { this.returnDate = returnDate; }
    public String getReturnTime() { return returnTime; }
    public void setReturnTime(String returnTime) { this.returnTime = returnTime; }
    public String getPickupLocation() { return pickupLocation; }
    public void setPickupLocation(String pickupLocation) { this.pickupLocation = pickupLocation; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public Integer getRentalDays() { return rentalDays; }
    public void setRentalDays(Integer rentalDays) { this.rentalDays = rentalDays; }
    public String getDiscountId() { return discountId; }
    public void setDiscountId(String discountId) { this.discountId = discountId; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public BigDecimal getVehicleRentalFee() { return vehicleRentalFee; }
    public void setVehicleRentalFee(BigDecimal vehicleRentalFee) { this.vehicleRentalFee = vehicleRentalFee; }
}
