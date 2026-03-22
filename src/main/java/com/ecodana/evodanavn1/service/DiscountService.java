package com.ecodana.evodanavn1.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ecodana.evodanavn1.model.Discount;
import com.ecodana.evodanavn1.repository.DiscountRepository;

@Service
public class DiscountService {
    
    @Autowired
    private DiscountRepository discountRepository;
    
    /**
     * Get all discounts
     * @return list of all discounts
     */
    public List<Discount> getAllDiscounts() {
        return discountRepository.findAll();
    }
    
    /**
     * Get active discounts
     * @return list of active discounts
     */
    public List<Discount> getActiveDiscounts() {
        return discountRepository.findActiveDiscounts();
    }
    
    /**
     * Get currently valid discounts
     * @return list of currently valid discounts
     */
    public List<Discount> getCurrentlyValidDiscounts() {
        return discountRepository.findCurrentlyValidDiscounts(LocalDate.now());
    }
    
    /**
     * Find discount by voucher code
     * @param voucherCode the voucher code
     * @return optional discount
     */
    public Optional<Discount> findByVoucherCode(String voucherCode) {
        return discountRepository.findByVoucherCode(voucherCode);
    }
    
    /**
     * Get applicable discounts for order amount
     * @param orderAmount the order amount
     * @return list of applicable discounts
     */
    public List<Discount> getApplicableDiscounts(BigDecimal orderAmount) {
        return discountRepository.findApplicableDiscounts(orderAmount);
    }
    
    /**
     * Save discount
     * @param discount the discount to save
     * @return saved discount
     */
    public Discount saveDiscount(Discount discount) {
        return discountRepository.save(discount);
    }
    
    /**
     * Update discount
     * @param discount the discount to update
     * @return updated discount
     */
    public Discount updateDiscount(Discount discount) {
        return discountRepository.save(discount);
    }
    
    /**
     * Delete discount
     * @param discountId the discount ID to delete
     */
    public void deleteDiscount(String discountId) {
        discountRepository.deleteById(discountId);
    }
    
    /**
     * Check if discount is valid
     * @param discount the discount to check
     * @return true if valid, false otherwise
     */
    public boolean isDiscountValid(Discount discount) {
        if (discount == null || !discount.getIsActive()) {
            return false;
        }
        
        LocalDate today = LocalDate.now();
        return !today.isBefore(discount.getStartDate()) && !today.isAfter(discount.getEndDate());
    }
    
    /**
     * Calculate discount amount
     * @param discount the discount
     * @param orderAmount the order amount
     * @return calculated discount amount
     */
    public BigDecimal calculateDiscountAmount(Discount discount, BigDecimal orderAmount) {
        if (!isDiscountValid(discount) || orderAmount.compareTo(discount.getMinOrderAmount()) < 0) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal discountAmount;
        if ("Percentage".equals(discount.getDiscountType())) {
            discountAmount = orderAmount.multiply(discount.getDiscountValue()).divide(new BigDecimal("100"));
        } else {
            discountAmount = discount.getDiscountValue();
        }
        
        // Apply maximum discount limit if set
        if (discount.getMaxDiscountAmount() != null && discountAmount.compareTo(discount.getMaxDiscountAmount()) > 0) {
            discountAmount = discount.getMaxDiscountAmount();
        }
        
        return discountAmount;
    }

    /**
     * Get available discounts for customers (active, within date range, not exceeded usage limit)
     * @return list of available discounts
     */
    public List<Discount> getAvailableDiscountsForCustomer() {
        return discountRepository.findAvailableDiscountsForCustomer(LocalDate.now());
    }
}
