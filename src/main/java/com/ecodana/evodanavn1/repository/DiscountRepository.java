package com.ecodana.evodanavn1.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ecodana.evodanavn1.model.Discount;

@Repository
public interface DiscountRepository extends JpaRepository<Discount, String> {
    
    /**
     * Find discounts by voucher code
     * @param voucherCode the voucher code
     * @return optional discount
     */
    Optional<Discount> findByVoucherCode(String voucherCode);
    
    /**
     * Find active discounts
     * @return list of active discounts
     */
    @Query("SELECT d FROM Discount d WHERE d.isActive = true")
    List<Discount> findActiveDiscounts();
    
    /**
     * Find discounts by discount type
     * @param discountType the discount type
     * @return list of discounts with the type
     */
    List<Discount> findByDiscountType(String discountType);
    
    /**
     * Find discounts by category
     * @param discountCategory the discount category
     * @return list of discounts in the category
     */
    List<Discount> findByDiscountCategory(String discountCategory);
    
    /**
     * Find currently valid discounts
     * @param currentDate the current date
     * @return list of currently valid discounts
     */
    @Query("SELECT d FROM Discount d WHERE d.isActive = true AND d.startDate <= :currentDate AND d.endDate >= :currentDate")
    List<Discount> findCurrentlyValidDiscounts(@Param("currentDate") LocalDate currentDate);
    
    /**
     * Check if discount exists by voucher code
     * @param voucherCode the voucher code
     * @return true if exists, false otherwise
     */
    boolean existsByVoucherCode(String voucherCode);
    
    /**
     * Find discounts by minimum order amount
     * @param minOrderAmount the minimum order amount
     * @return list of discounts with minimum order amount
     */
    @Query("SELECT d FROM Discount d WHERE d.minOrderAmount <= :minOrderAmount AND d.isActive = true")
    List<Discount> findApplicableDiscounts(@Param("minOrderAmount") java.math.BigDecimal minOrderAmount);

    /**
     * Find available discounts for customers (active, within date range, not exceeded usage limit)
     * @param currentDate the current date
     * @return list of available discounts
     */
    @Query("SELECT d FROM Discount d WHERE d.isActive = true AND d.startDate <= :currentDate AND d.endDate >= :currentDate AND (d.usageLimit IS NULL OR d.usedCount < d.usageLimit)")
    List<Discount> findAvailableDiscountsForCustomer(@Param("currentDate") LocalDate currentDate);
}
