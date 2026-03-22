package com.ecodana.evodanavn1.controller.admin;

import com.ecodana.evodanavn1.model.Discount;
import com.ecodana.evodanavn1.repository.DiscountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/api/discounts")
public class DiscountApiController {

    @Autowired
    private DiscountRepository discountRepository;

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllDiscounts() {
        try {
            List<Discount> discounts = discountRepository.findAll();
            
            List<Map<String, Object>> discountsData = discounts.stream().map(discount -> {
                Map<String, Object> data = new HashMap<>();
                data.put("discountId", discount.getDiscountId());
                data.put("discountName", discount.getDiscountName());
                data.put("description", discount.getDescription());
                data.put("discountType", discount.getDiscountType());
                data.put("discountValue", discount.getDiscountValue());
                data.put("startDate", discount.getStartDate());
                data.put("endDate", discount.getEndDate());
                data.put("isActive", discount.getIsActive());
                data.put("voucherCode", discount.getVoucherCode());
                data.put("minOrderAmount", discount.getMinOrderAmount());
                data.put("maxDiscountAmount", discount.getMaxDiscountAmount());
                data.put("usageLimit", discount.getUsageLimit());
                data.put("usedCount", discount.getUsedCount());
                data.put("discountCategory", discount.getDiscountCategory());
                data.put("createdDate", discount.getCreatedDate());
                return data;
            }).collect(Collectors.toList());
            
            return ResponseEntity.ok(discountsData);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getDiscountById(@PathVariable String id) {
        try {
            return discountRepository.findById(id)
                .map(discount -> {
                    Map<String, Object> data = new HashMap<>();
                    data.put("discountId", discount.getDiscountId());
                    data.put("discountName", discount.getDiscountName());
                    data.put("description", discount.getDescription());
                    data.put("discountType", discount.getDiscountType());
                    data.put("discountValue", discount.getDiscountValue());
                    data.put("startDate", discount.getStartDate());
                    data.put("endDate", discount.getEndDate());
                    data.put("isActive", discount.getIsActive());
                    data.put("voucherCode", discount.getVoucherCode());
                    data.put("minOrderAmount", discount.getMinOrderAmount());
                    data.put("maxDiscountAmount", discount.getMaxDiscountAmount());
                    data.put("usageLimit", discount.getUsageLimit());
                    data.put("usedCount", discount.getUsedCount());
                    data.put("discountCategory", discount.getDiscountCategory());
                    data.put("createdDate", discount.getCreatedDate());
                    return ResponseEntity.ok(data);
                })
                .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        try {
            List<Discount> allDiscounts = discountRepository.findAll();
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("total", allDiscounts.size());
            stats.put("active", allDiscounts.stream().filter(d -> d.getIsActive()).count());
            stats.put("inactive", allDiscounts.stream().filter(d -> !d.getIsActive()).count());
            stats.put("expired", allDiscounts.stream().filter(d -> d.getEndDate().isBefore(LocalDate.now())).count());
            
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createDiscount(@RequestBody Map<String, Object> discountData) {
        try {
            Discount discount = new Discount();
            discount.setDiscountId(UUID.randomUUID().toString());
            discount.setDiscountName((String) discountData.get("discountName"));
            discount.setDescription((String) discountData.get("description"));
            discount.setDiscountType((String) discountData.get("discountType"));
            discount.setDiscountValue(new BigDecimal(discountData.get("discountValue").toString()));
            discount.setStartDate(LocalDate.parse((String) discountData.get("startDate")));
            discount.setEndDate(LocalDate.parse((String) discountData.get("endDate")));
            discount.setIsActive((Boolean) discountData.get("isActive"));
            discount.setVoucherCode((String) discountData.get("voucherCode"));
            discount.setMinOrderAmount(new BigDecimal(discountData.get("minOrderAmount").toString()));
            
            if (discountData.containsKey("maxDiscountAmount") && discountData.get("maxDiscountAmount") != null) {
                discount.setMaxDiscountAmount(new BigDecimal(discountData.get("maxDiscountAmount").toString()));
            }
            if (discountData.containsKey("usageLimit") && discountData.get("usageLimit") != null) {
                discount.setUsageLimit((Integer) discountData.get("usageLimit"));
            }
            if (discountData.containsKey("discountCategory")) {
                discount.setDiscountCategory((String) discountData.get("discountCategory"));
            }
            
            discountRepository.save(discount);
            
            return ResponseEntity.ok(Map.of("status", "success", "message", "Discount created successfully", "discountId", discount.getDiscountId()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("status", "error", "message", "Failed to create discount: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateDiscount(@PathVariable String id, @RequestBody Map<String, Object> updates) {
        try {
            Optional<Discount> discountOpt = discountRepository.findById(id);
            if (!discountOpt.isPresent()) {
                return ResponseEntity.status(404).body(Map.of("status", "error", "message", "Discount not found"));
            }
            
            Discount discount = discountOpt.get();
            
            if (updates.containsKey("discountName")) discount.setDiscountName((String) updates.get("discountName"));
            if (updates.containsKey("description")) discount.setDescription((String) updates.get("description"));
            if (updates.containsKey("discountType")) discount.setDiscountType((String) updates.get("discountType"));
            if (updates.containsKey("discountValue")) discount.setDiscountValue(new BigDecimal(updates.get("discountValue").toString()));
            if (updates.containsKey("startDate")) discount.setStartDate(LocalDate.parse((String) updates.get("startDate")));
            if (updates.containsKey("endDate")) discount.setEndDate(LocalDate.parse((String) updates.get("endDate")));
            if (updates.containsKey("isActive")) discount.setIsActive((Boolean) updates.get("isActive"));
            if (updates.containsKey("voucherCode")) discount.setVoucherCode((String) updates.get("voucherCode"));
            if (updates.containsKey("minOrderAmount")) discount.setMinOrderAmount(new BigDecimal(updates.get("minOrderAmount").toString()));
            if (updates.containsKey("maxDiscountAmount") && updates.get("maxDiscountAmount") != null) {
                discount.setMaxDiscountAmount(new BigDecimal(updates.get("maxDiscountAmount").toString()));
            }
            if (updates.containsKey("usageLimit")) discount.setUsageLimit((Integer) updates.get("usageLimit"));
            if (updates.containsKey("discountCategory")) discount.setDiscountCategory((String) updates.get("discountCategory"));
            
            discountRepository.save(discount);
            
            return ResponseEntity.ok(Map.of("status", "success", "message", "Discount updated successfully"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("status", "error", "message", "Failed to update discount: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteDiscount(@PathVariable String id) {
        try {
            if (!discountRepository.existsById(id)) {
                return ResponseEntity.status(404).body(Map.of("status", "error", "message", "Discount not found"));
            }
            
            discountRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("status", "success", "message", "Discount deleted successfully"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("status", "error", "message", "Failed to delete discount: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/toggle-status")
    public ResponseEntity<Map<String, Object>> toggleStatus(@PathVariable String id) {
        try {
            Optional<Discount> discountOpt = discountRepository.findById(id);
            if (!discountOpt.isPresent()) {
                return ResponseEntity.status(404).body(Map.of("status", "error", "message", "Discount not found"));
            }
            
            Discount discount = discountOpt.get();
            discount.setIsActive(!discount.getIsActive());
            discountRepository.save(discount);
            
            return ResponseEntity.ok(Map.of("status", "success", "message", "Discount status updated", "isActive", discount.getIsActive()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("status", "error", "message", "Failed to toggle status"));
        }
    }
}
