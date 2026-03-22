package com.ecodana.evodanavn1.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "BookingSurcharges")
public class BookingSurcharges {

    @Id
    @Column(name = "SurchargeId", length = 36)
    private String surchargeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "BookingId", nullable = false)
    private Booking booking;

    @Column(name = "SurchargeType", length = 50, nullable = false)
    private String surchargeType;

    @Column(name = "Amount", precision = 10, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "Description", length = 255)
    private String description;

    @Column(name = "CreatedDate", nullable = false)
    private LocalDateTime createdDate;

    @Column(name = "SurchargeCategory", length = 50)
    private String surchargeCategory;

    @Column(name = "IsSystemGenerated", nullable = false)
    private boolean isSystemGenerated = false;

    public BookingSurcharges() {
        this.createdDate = LocalDateTime.now();
    }

    // Getters and Setters
    public String getSurchargeId() { return surchargeId; }
    public void setSurchargeId(String surchargeId) { this.surchargeId = surchargeId; }
    public Booking getBooking() { return booking; }
    public void setBooking(Booking booking) { this.booking = booking; }
    public String getSurchargeType() { return surchargeType; }
    public void setSurchargeType(String surchargeType) { this.surchargeType = surchargeType; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }
    public String getSurchargeCategory() { return surchargeCategory; }
    public void setSurchargeCategory(String surchargeCategory) { this.surchargeCategory = surchargeCategory; }
    public boolean isSystemGenerated() { return isSystemGenerated; }
    public void setSystemGenerated(boolean systemGenerated) { isSystemGenerated = systemGenerated; }
}