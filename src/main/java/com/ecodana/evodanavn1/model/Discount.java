package com.ecodana.evodanavn1.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "Discount")
public class Discount {
    @Id
    @Column(name = "DiscountId", length = 36)
    private String discountId;
    
    @Column(name = "DiscountName", length = 100, nullable = false)
    private String discountName;
    
    @Column(name = "Description", length = 255)
    private String description;
    
    @Column(name = "DiscountType", length = 20, nullable = false)
    private String discountType;
    
    @Column(name = "DiscountValue", precision = 10, scale = 2, nullable = false)
    private BigDecimal discountValue;
    
    @Column(name = "StartDate", nullable = false)
    private LocalDate startDate;
    
    @Column(name = "EndDate", nullable = false)
    private LocalDate endDate;
    
    @Column(name = "IsActive", nullable = false)
    private Boolean isActive;
    
    @Column(name = "CreatedDate", nullable = false)
    private LocalDateTime createdDate;
    
    @Column(name = "VoucherCode", length = 20)
    private String voucherCode;
    
    @Column(name = "MinOrderAmount", precision = 10, scale = 2, nullable = false)
    private BigDecimal minOrderAmount;
    
    @Column(name = "MaxDiscountAmount", precision = 10, scale = 2)
    private BigDecimal maxDiscountAmount;
    
    @Column(name = "UsageLimit")
    private Integer usageLimit;
    
    @Column(name = "UsedCount", nullable = false)
    private Integer usedCount = 0;
    
    @Column(name = "DiscountCategory", length = 20, nullable = false)
    private String discountCategory = "General";
    
    // Constructors
    public Discount() {
        this.createdDate = LocalDateTime.now();
        this.usedCount = 0;
    }
    
    // Getters/Setters
    public String getDiscountId() { return discountId; }
    public void setDiscountId(String discountId) { this.discountId = discountId; }
    
    public String getDiscountName() { return discountName; }
    public void setDiscountName(String discountName) { this.discountName = discountName; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getDiscountType() { return discountType; }
    public void setDiscountType(String discountType) { this.discountType = discountType; }
    
    public BigDecimal getDiscountValue() { return discountValue; }
    public void setDiscountValue(BigDecimal discountValue) { this.discountValue = discountValue; }
    
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    
    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }
    
    public String getVoucherCode() { return voucherCode; }
    public void setVoucherCode(String voucherCode) { this.voucherCode = voucherCode; }
    
    public BigDecimal getMinOrderAmount() { return minOrderAmount; }
    public void setMinOrderAmount(BigDecimal minOrderAmount) { this.minOrderAmount = minOrderAmount; }
    
    public BigDecimal getMaxDiscountAmount() { return maxDiscountAmount; }
    public void setMaxDiscountAmount(BigDecimal maxDiscountAmount) { this.maxDiscountAmount = maxDiscountAmount; }
    
    public Integer getUsageLimit() { return usageLimit; }
    public void setUsageLimit(Integer usageLimit) { this.usageLimit = usageLimit; }
    
    public Integer getUsedCount() { return usedCount; }
    public void setUsedCount(Integer usedCount) { this.usedCount = usedCount; }
    
    public String getDiscountCategory() { return discountCategory; }
    public void setDiscountCategory(String discountCategory) { this.discountCategory = discountCategory; }
}
