package com.ecodana.evodanavn1.controller.admin;

import com.ecodana.evodanavn1.model.Payment;
import com.ecodana.evodanavn1.model.Payment.PaymentStatus;
import com.ecodana.evodanavn1.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/api/payments")
public class PaymentApiController {

    @Autowired
    private PaymentRepository paymentRepository;

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllPayments() {
        try {
            List<Payment> payments = paymentRepository.findAll();
            
            List<Map<String, Object>> paymentsData = payments.stream().map(payment -> {
                Map<String, Object> data = new HashMap<>();
                data.put("paymentId", payment.getPaymentId());
                data.put("amount", payment.getAmount());
                data.put("paymentMethod", payment.getPaymentMethod());
                data.put("paymentStatus", payment.getPaymentStatus().toString());
                data.put("paymentType", payment.getPaymentType().toString());
                data.put("transactionId", payment.getTransactionId());
                data.put("paymentDate", payment.getPaymentDate());
                data.put("notes", payment.getNotes());
                data.put("createdDate", payment.getCreatedDate());
                
                // Booking info
                if (payment.getBooking() != null) {
                    data.put("bookingCode", payment.getBooking().getBookingCode());
                    data.put("bookingId", payment.getBooking().getBookingId());
                }
                
                // User info
                if (payment.getUser() != null) {
                    data.put("userName", payment.getUser().getFirstName() + " " + payment.getUser().getLastName());
                    data.put("userEmail", payment.getUser().getEmail());
                }
                
                // Contract info
                if (payment.getContract() != null) {
                    data.put("contractCode", payment.getContract().getContractCode());
                }
                
                return data;
            }).collect(Collectors.toList());
            
            return ResponseEntity.ok(paymentsData);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getPaymentById(@PathVariable String id) {
        try {
            return paymentRepository.findById(id)
                .map(payment -> {
                    Map<String, Object> data = new HashMap<>();
                    data.put("paymentId", payment.getPaymentId());
                    data.put("amount", payment.getAmount());
                    data.put("paymentMethod", payment.getPaymentMethod());
                    data.put("paymentStatus", payment.getPaymentStatus().toString());
                    data.put("paymentType", payment.getPaymentType().toString());
                    data.put("transactionId", payment.getTransactionId());
                    data.put("paymentDate", payment.getPaymentDate());
                    data.put("notes", payment.getNotes());
                    data.put("createdDate", payment.getCreatedDate());
                    
                    if (payment.getBooking() != null) {
                        data.put("bookingCode", payment.getBooking().getBookingCode());
                        data.put("bookingId", payment.getBooking().getBookingId());
                    }
                    
                    if (payment.getUser() != null) {
                        data.put("userName", payment.getUser().getFirstName() + " " + payment.getUser().getLastName());
                        data.put("userEmail", payment.getUser().getEmail());
                        data.put("userPhone", payment.getUser().getPhoneNumber());
                    }
                    
                    if (payment.getContract() != null) {
                        data.put("contractCode", payment.getContract().getContractCode());
                    }
                    
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
            List<Payment> allPayments = paymentRepository.findAll();
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("total", allPayments.size());
            stats.put("pending", allPayments.stream().filter(p -> p.getPaymentStatus() == PaymentStatus.Pending).count());
            stats.put("completed", allPayments.stream().filter(p -> p.getPaymentStatus() == PaymentStatus.Completed).count());
            stats.put("failed", allPayments.stream().filter(p -> p.getPaymentStatus() == PaymentStatus.Failed).count());
            stats.put("refunded", allPayments.stream().filter(p -> p.getPaymentStatus() == PaymentStatus.Refunded).count());
            
            // Calculate total revenue
            double totalRevenue = allPayments.stream()
                .filter(p -> p.getPaymentStatus() == PaymentStatus.Completed)
                .mapToDouble(p -> p.getAmount().doubleValue())
                .sum();
            stats.put("totalRevenue", totalRevenue);
            
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

    @PutMapping("/{id}/update")
    public ResponseEntity<Map<String, Object>> updatePayment(@PathVariable String id, @RequestBody Map<String, Object> updates) {
        try {
            System.out.println("DEBUG: Updating payment " + id);
            System.out.println("DEBUG: Updates: " + updates);
            
            Optional<Payment> paymentOpt = paymentRepository.findById(id);
            if (!paymentOpt.isPresent()) {
                return ResponseEntity.status(404).body(Map.of("status", "error", "message", "Payment not found"));
            }
            
            Payment payment = paymentOpt.get();
            
            // Update status
            if (updates.containsKey("paymentStatus")) {
                String statusStr = (String) updates.get("paymentStatus");
                payment.setPaymentStatus(PaymentStatus.valueOf(statusStr));
                
                // Auto-set payment date when status is Completed
                if (statusStr.equals("Completed") && payment.getPaymentDate() == null) {
                    payment.setPaymentDate(java.time.LocalDateTime.now());
                }
            }
            
            // Update amount
            if (updates.containsKey("amount")) {
                Object amountObj = updates.get("amount");
                if (amountObj instanceof Number) {
                    payment.setAmount(new java.math.BigDecimal(amountObj.toString()));
                }
            }
            
            // Update payment method
            if (updates.containsKey("paymentMethod")) {
                payment.setPaymentMethod((String) updates.get("paymentMethod"));
            }
            
            // Update notes
            if (updates.containsKey("notes")) {
                payment.setNotes((String) updates.get("notes"));
            }
            
            // Update transaction ID
            if (updates.containsKey("transactionId")) {
                payment.setTransactionId((String) updates.get("transactionId"));
            }
            
            paymentRepository.save(payment);
            
            return ResponseEntity.ok(Map.of("status", "success", "message", "Payment updated successfully"));
        } catch (Exception e) {
            System.err.println("ERROR: Failed to update payment: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("status", "error", "message", "Failed to update payment: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deletePayment(@PathVariable String id) {
        try {
            if (!paymentRepository.existsById(id)) {
                return ResponseEntity.status(404).body(Map.of("status", "error", "message", "Payment not found"));
            }
            
            paymentRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("status", "success", "message", "Payment deleted successfully"));
        } catch (Exception e) {
            System.err.println("ERROR: Failed to delete payment: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("status", "error", "message", "Failed to delete payment: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/mark-completed")
    public ResponseEntity<Map<String, Object>> markAsCompleted(@PathVariable String id) {
        try {
            Optional<Payment> paymentOpt = paymentRepository.findById(id);
            if (!paymentOpt.isPresent()) {
                return ResponseEntity.status(404).body(Map.of("status", "error", "message", "Payment not found"));
            }
            
            Payment payment = paymentOpt.get();
            payment.setPaymentStatus(PaymentStatus.Completed);
            payment.setPaymentDate(java.time.LocalDateTime.now());
            paymentRepository.save(payment);
            
            return ResponseEntity.ok(Map.of("status", "success", "message", "Payment marked as completed"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("status", "error", "message", "Failed to mark payment as completed"));
        }
    }

    @PostMapping("/{id}/mark-failed")
    public ResponseEntity<Map<String, Object>> markAsFailed(@PathVariable String id) {
        try {
            Optional<Payment> paymentOpt = paymentRepository.findById(id);
            if (!paymentOpt.isPresent()) {
                return ResponseEntity.status(404).body(Map.of("status", "error", "message", "Payment not found"));
            }
            
            Payment payment = paymentOpt.get();
            payment.setPaymentStatus(PaymentStatus.Failed);
            paymentRepository.save(payment);
            
            return ResponseEntity.ok(Map.of("status", "success", "message", "Payment marked as failed"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("status", "error", "message", "Failed to mark payment as failed"));
        }
    }

    @PostMapping("/{id}/refund")
    public ResponseEntity<Map<String, Object>> processRefund(@PathVariable String id) {
        try {
            Optional<Payment> paymentOpt = paymentRepository.findById(id);
            if (!paymentOpt.isPresent()) {
                return ResponseEntity.status(404).body(Map.of("status", "error", "message", "Payment not found"));
            }
            
            Payment payment = paymentOpt.get();
            if (payment.getPaymentStatus() != PaymentStatus.Completed) {
                return ResponseEntity.status(400).body(Map.of("status", "error", "message", "Only completed payments can be refunded"));
            }
            
            payment.setPaymentStatus(PaymentStatus.Refunded);
            paymentRepository.save(payment);
            
            return ResponseEntity.ok(Map.of("status", "success", "message", "Refund processed successfully"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("status", "error", "message", "Failed to process refund"));
        }
    }
}
