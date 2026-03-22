package com.ecodana.evodanavn1.controller.api;

import com.ecodana.evodanavn1.model.Discount;
import com.ecodana.evodanavn1.service.DiscountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/discounts")
public class DiscountValidationController {

    @Autowired
    private DiscountService discountService;

    /**
     * Validate discount code and calculate discount amount
     * @param code discount voucher code
     * @param amount order amount
     * @return validation result with discount details
     */
    @GetMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateDiscount(
            @RequestParam String code,
            @RequestParam BigDecimal amount) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Find discount by voucher code
            Optional<Discount> discountOpt = discountService.findByVoucherCode(code);
            
            if (!discountOpt.isPresent()) {
                response.put("valid", false);
                response.put("message", "Mã giảm giá không tồn tại!");
                return ResponseEntity.ok(response);
            }
            
            Discount discount = discountOpt.get();
            
            // Check if discount is active
            if (!discount.getIsActive()) {
                response.put("valid", false);
                response.put("message", "Mã giảm giá đã bị vô hiệu hóa!");
                return ResponseEntity.ok(response);
            }
            
            // Check date validity
            LocalDate today = LocalDate.now();
            if (today.isBefore(discount.getStartDate())) {
                response.put("valid", false);
                response.put("message", "Mã giảm giá chưa có hiệu lực!");
                return ResponseEntity.ok(response);
            }
            
            if (today.isAfter(discount.getEndDate())) {
                response.put("valid", false);
                response.put("message", "Mã giảm giá đã hết hạn!");
                return ResponseEntity.ok(response);
            }
            
            // Check minimum order amount
            if (amount.compareTo(discount.getMinOrderAmount()) < 0) {
                response.put("valid", false);
                response.put("message", "Đơn hàng chưa đủ giá trị tối thiểu để áp dụng mã giảm giá!");
                response.put("minOrderAmount", discount.getMinOrderAmount());
                return ResponseEntity.ok(response);
            }
            
            // Check usage limit
            if (discount.getUsageLimit() != null && 
                discount.getUsedCount() >= discount.getUsageLimit()) {
                response.put("valid", false);
                response.put("message", "Mã giảm giá đã hết lượt sử dụng!");
                return ResponseEntity.ok(response);
            }
            
            // Calculate discount amount
            BigDecimal discountAmount = discountService.calculateDiscountAmount(discount, amount);
            
            // Build discount data
            Map<String, Object> discountData = new HashMap<>();
            discountData.put("discountId", discount.getDiscountId());
            discountData.put("discountName", discount.getDiscountName());
            discountData.put("voucherCode", discount.getVoucherCode());
            discountData.put("discountType", discount.getDiscountType());
            discountData.put("discountValue", discount.getDiscountValue());
            discountData.put("maxDiscountAmount", discount.getMaxDiscountAmount());
            
            response.put("valid", true);
            response.put("message", "Mã giảm giá hợp lệ!");
            response.put("discount", discountData);
            response.put("calculatedAmount", discountAmount);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            e.printStackTrace();
            response.put("valid", false);
            response.put("message", "Có lỗi xảy ra khi kiểm tra mã giảm giá!");
            return ResponseEntity.status(500).body(response);
        }
    }
}
