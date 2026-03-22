package com.ecodana.evodanavn1.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "Booking")
public class Booking {
    @Id
    @Column(name = "BookingId", length = 36)
    private String bookingId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "UserId", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "VehicleId", nullable = false)
    private Vehicle vehicle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "HandledBy")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User handledBy;

    @Column(name = "PickupDateTime", nullable = false)
    private LocalDateTime pickupDateTime;

    @Column(name = "ReturnDateTime", nullable = false)
    private LocalDateTime returnDateTime;

    @Column(name = "PickupLocation", length = 500)
    private String pickupLocation;

    @Column(name = "VehicleRentalFee", precision = 10, scale = 2, nullable = false)
    private BigDecimal vehicleRentalFee;

    @Column(name = "PlatformFee", precision = 10, scale = 2, nullable = false)
    private BigDecimal platformFee;

    @Column(name = "OwnerPayout", precision = 10, scale = 2, nullable = false)
    private BigDecimal ownerPayout;

    @Column(name = "TotalAmount", precision = 10, scale = 2, nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "DepositAmountRequired", precision = 10, scale = 2, nullable = false)
    private BigDecimal depositAmountRequired = BigDecimal.ZERO;

    @Column(name = "PaymentOption", length = 20) // "DEPOSIT" or "FULL"
    private String paymentOption;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Payment> payments = new ArrayList<>();


    @Column(name = "PaymentConfirmedAt")
    private LocalDateTime paymentConfirmedAt;

    @Column(name = "Status", length = 50, nullable = false)
    @Enumerated(EnumType.STRING)
    private BookingStatus status = BookingStatus.Pending;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "DiscountId")
    private Discount discount;

    @Column(name = "CreatedDate", nullable = false)
    private LocalDateTime createdDate;

    @Column(name = "CancelReason", length = 500)
    private String cancelReason;

    @Column(name = "BookingCode", length = 20, nullable = false, unique = true)
    private String bookingCode;

    @Column(name = "ExpectedPaymentMethod", length = 50)
    private String expectedPaymentMethod;

    @Column(name = "RentalType", length = 10, nullable = false)
    @Enumerated(EnumType.STRING)
    private RentalType rentalType = RentalType.daily;

    @Column(name = "TermsAgreed", nullable = false)
    private Boolean termsAgreed = false;

    @Column(name = "TermsAgreedAt")
    private LocalDateTime termsAgreedAt;

    @Column(name = "TermsVersion", length = 10)
    private String termsVersion = "v1.0";

    @Column(name = "return_notes", columnDefinition = "TEXT")
    private String returnNotes;

    @Column(name = "return_image_urls", columnDefinition = "TEXT")
    private String returnImageUrls; // Changed to String to store comma-separated URLs

    @Transient
    private boolean hasFeedback = false;

    @Transient
    private boolean canReview = false;

    @Transient
    private BigDecimal remainingAmount;

    // Constructors
    public Booking() {
        this.createdDate = LocalDateTime.now();
        this.vehicleRentalFee = BigDecimal.ZERO;
        this.platformFee = BigDecimal.ZERO;
        this.ownerPayout = BigDecimal.ZERO;
    }

    // Getters and Setters
    public BigDecimal getRemainingAmount() {
        return remainingAmount;
    }

    public void setRemainingAmount(BigDecimal remainingAmount) {
        this.remainingAmount = remainingAmount;
    }

    public String getReturnNotes() {
        return returnNotes;
    }

    public void setReturnNotes(String returnNotes) {
        this.returnNotes = returnNotes;
    }

    // Modified getter for returnImageUrls to return a List<String>
    public List<String> getReturnImageUrlsList() {
        if (this.returnImageUrls == null || this.returnImageUrls.isEmpty()) {
            return new ArrayList<>();
        }
        return Arrays.asList(this.returnImageUrls.split(","));
    }

    // Modified setter for returnImageUrls to accept a List<String> and store as a comma-separated String
    public void setReturnImageUrlsList(List<String> returnImageUrlsList) {
        if (returnImageUrlsList == null || returnImageUrlsList.isEmpty()) {
            this.returnImageUrls = null;
        } else {
            this.returnImageUrls = returnImageUrlsList.stream().collect(Collectors.joining(","));
        }
    }

    // Existing getter and setter for the String field (can be used internally or for direct DB interaction)
    public String getReturnImageUrls() {
        return returnImageUrls;
    }

    public void setReturnImageUrls(String returnImageUrls) {
        this.returnImageUrls = returnImageUrls;
    }

    public String getBookingId() { return bookingId; }
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Vehicle getVehicle() { return vehicle; }
    public void setVehicle(Vehicle vehicle) { this.vehicle = vehicle; }
    public User getHandledBy() { return handledBy; }
    public void setHandledBy(User handledBy) { this.handledBy = handledBy; }
    public LocalDateTime getPickupDateTime() { return pickupDateTime; }
    public void setPickupDateTime(LocalDateTime pickupDateTime) { this.pickupDateTime = pickupDateTime; }
    public LocalDateTime getReturnDateTime() { return returnDateTime; }
    public void setReturnDateTime(LocalDateTime returnDateTime) { this.returnDateTime = returnDateTime; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public String getPickupLocation() { return pickupLocation; }
    public void setPickupLocation(String pickupLocation) { this.pickupLocation = pickupLocation; }
    public BigDecimal getDepositAmountRequired() { return depositAmountRequired; }
    public void setDepositAmountRequired(BigDecimal depositAmountRequired) { this.depositAmountRequired = depositAmountRequired; }
    public BookingStatus getStatus() { return status; }
    public void setStatus(BookingStatus status) { this.status = status; }
    public Discount getDiscount() { return discount; }
    public void setDiscount(Discount discount) { this.discount = discount; }
    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }
    public String getCancelReason() { return cancelReason; }
    public void setCancelReason(String cancelReason) { this.cancelReason = cancelReason; }
    public String getBookingCode() { return bookingCode; }
    public void setBookingCode(String bookingCode) { this.bookingCode = bookingCode; }
    public String getExpectedPaymentMethod() { return expectedPaymentMethod; }
    public void setExpectedPaymentMethod(String expectedPaymentMethod) { this.expectedPaymentMethod = expectedPaymentMethod; }
    public RentalType getRentalType() { return rentalType; }
    public void setRentalType(RentalType rentalType) { this.rentalType = rentalType; }
    public Boolean getTermsAgreed() { return termsAgreed; }
    public void setTermsAgreed(Boolean termsAgreed) { this.termsAgreed = termsAgreed; }
    public LocalDateTime getTermsAgreedAt() { return termsAgreedAt; }
    public void setTermsAgreedAt(LocalDateTime termsAgreedAt) { this.termsAgreedAt = termsAgreedAt; }
    public String getTermsVersion() { return termsVersion; }
    public void setTermsVersion(String termsVersion) { this.termsVersion = termsVersion; }
    public boolean isHasFeedback() { return hasFeedback; }
    public void setHasFeedback(boolean hasFeedback) { this.hasFeedback = hasFeedback; }
    public boolean isCanReview() { return canReview; }
    public void setCanReview(boolean canReview) { this.canReview = canReview; }
    public BigDecimal getVehicleRentalFee() { return vehicleRentalFee; }
    public void setVehicleRentalFee(BigDecimal vehicleRentalFee) { this.vehicleRentalFee = vehicleRentalFee; }
    public BigDecimal getPlatformFee() { return platformFee; }
    public void setPlatformFee(BigDecimal platformFee) { this.platformFee = platformFee; }
    public BigDecimal getOwnerPayout() { return ownerPayout; }
    public void setOwnerPayout(BigDecimal ownerPayout) { this.ownerPayout = ownerPayout; }
    public LocalDateTime getPaymentConfirmedAt() { return paymentConfirmedAt; }
    public void setPaymentConfirmedAt(LocalDateTime paymentConfirmedAt) { this.paymentConfirmedAt = paymentConfirmedAt; }
    public String getPaymentOption() { return paymentOption; }
    public void setPaymentOption(String paymentOption) { this.paymentOption = paymentOption; }
    public List<Payment> getPayments() { return payments; }
    public void setPayments(List<Payment> payments) { this.payments = payments; }


    public enum BookingStatus {
        Pending,          // Khách vừa tạo, chờ chủ xe duyệt
        Approved,         // Chủ xe đã duyệt (trạng thái trung gian)
        AwaitingDeposit,  // Đã duyệt, chờ khách thanh toán 20% cọc
        Confirmed,        // Khách đã thanh toán cọc, đơn đã chắc chắn
        Rejected,         // Chủ xe từ chối
        Ongoing,          // Đang trong quá trình thuê (đã nhận xe)
        Completed,        // Đã hoàn tất chuyến đi và thanh toán
        Cancelled,        // Đơn bị hủy
        RefundPending,    // Chờ hoàn tiền
        Refunded,         // Đã hoàn tiền
        LatePickup,       // Quá thởi nhận xe
        NoShow
        // Owner báo cáo customer không đến
    }

    public enum RentalType {
        hourly, daily, monthly
    }
}