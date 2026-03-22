package com.ecodana.evodanavn1.controller.api;

import com.ecodana.evodanavn1.model.RefundRequest;
import com.ecodana.evodanavn1.repository.RefundRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Public API for customers to access refund information
 */
@RestController
@RequestMapping("/api/refund-requests")
@org.springframework.stereotype.Controller("customerRefundRequestApiController")
public class RefundRequestApiController {

    @Autowired
    private RefundRequestRepository refundRequestRepository;

    /**
     * Get refund request by booking ID (public API for customers)
     */
    @GetMapping("/by-booking/{bookingId}")
    public ResponseEntity<Map<String, Object>> getRefundRequestByBookingId(@PathVariable String bookingId) {
        try {
            Optional<RefundRequest> refundRequest = refundRequestRepository.findByBookingBookingId(bookingId);
            
            if (refundRequest.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of("status", "error", "message", "Refund request not found"));
            }
            
            RefundRequest refund = refundRequest.get();
            Map<String, Object> data = new HashMap<>();
            data.put("refundRequestId", refund.getRefundRequestId());
            data.put("refundAmount", refund.getRefundAmount());
            data.put("status", refund.getStatus().toString());
            data.put("transferProofImagePath", refund.getTransferProofImagePath());
            
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("status", "error", "message", "Server error"));
        }
    }
}
