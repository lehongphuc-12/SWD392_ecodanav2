package com.ecodana.evodanavn1.controller.admin;

import com.ecodana.evodanavn1.model.RefundRequest;
import com.ecodana.evodanavn1.model.Booking;
import com.ecodana.evodanavn1.model.BankAccount;
import com.ecodana.evodanavn1.repository.RefundRequestRepository;
import com.ecodana.evodanavn1.repository.BookingRepository;
import com.ecodana.evodanavn1.service.RefundRequestService;
import com.ecodana.evodanavn1.service.BankAccountService;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/api/refund-requests")
public class RefundRequestApiController {

    @Autowired
    private RefundRequestRepository refundRequestRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private RefundRequestService refundRequestService;

    @Autowired
    private BankAccountService bankAccountService;

    @Autowired
    private com.ecodana.evodanavn1.repository.PaymentRepository paymentRepository;

    @Autowired
    private Cloudinary cloudinary;

    /**
     * Sync Payment refunds to RefundRequests
     * This endpoint creates RefundRequest for all payments with Refund status that don't have a refund request yet
     */
    @PostMapping("/sync-payment-refunds")
    public ResponseEntity<Map<String, Object>> syncPaymentRefunds() {
        try {
            System.out.println("=== SYNC PAYMENT REFUNDS ===");
            int count = 0;
            
            // Get all payments with Refund status
            List<com.ecodana.evodanavn1.model.Payment> refundPayments = paymentRepository.findByPaymentStatus(com.ecodana.evodanavn1.model.Payment.PaymentStatus.Refunded);
            System.out.println("Found Refund payments: " + refundPayments.size());
            
            for (com.ecodana.evodanavn1.model.Payment payment : refundPayments) {
                System.out.println("Processing payment: " + payment.getPaymentId());
                
                // Get booking from payment
                Booking booking = payment.getBooking();
                if (booking == null) continue;
                
                // Check if RefundRequest already exists for this booking
                Optional<RefundRequest> existingRequest = refundRequestRepository.findByBookingBookingId(booking.getBookingId());
                
                if (existingRequest.isEmpty()) {
                    // Create RefundRequest from payment
                    try {
                        RefundRequest refundRequest = new RefundRequest();
                        refundRequest.setRefundRequestId(java.util.UUID.randomUUID().toString());
                        refundRequest.setBooking(booking);
                        refundRequest.setUser(booking.getUser());
                        refundRequest.setRefundAmount(payment.getAmount().abs()); // Get absolute value
                        refundRequest.setCancelReason(payment.getNotes() != null ? payment.getNotes() : "Refund from payment");
                        refundRequest.setStatus(RefundRequest.RefundStatus.Pending);
                        refundRequest.setWithinTwoHours(false);
                        
                        // Try to get bank account
                        Optional<BankAccount> bankAccount = bankAccountService.getDefaultBankAccount(booking.getUser().getId());
                        if (bankAccount.isPresent()) {
                            refundRequest.setBankAccount(bankAccount.get());
                        }
                        
                        refundRequestRepository.save(refundRequest);
                        
                        // Update booking status to RefundPending if not already
                        if (booking.getStatus() != Booking.BookingStatus.RefundPending && 
                            booking.getStatus() != Booking.BookingStatus.Cancelled) {
                            booking.setStatus(Booking.BookingStatus.RefundPending);
                            bookingRepository.save(booking);
                        }
                        
                        count++;
                        System.out.println("Created RefundRequest for booking: " + booking.getBookingId());
                    } catch (Exception e) {
                        System.out.println("Error creating RefundRequest for payment " + payment.getPaymentId() + ": " + e.getMessage());
                    }
                }
            }
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Synced " + count + " payment refunds to RefundRequests",
                "count", count
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                "status", "error",
                "message", "Failed to sync payment refunds: " + e.getMessage()
            ));
        }
    }

    /**
     * Get all refund requests with customer information
     */
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllRefundRequests() {
        try {
            List<RefundRequest> refundRequests = refundRequestRepository.findAll();
            
            List<Map<String, Object>> refundData = refundRequests.stream().map(refund -> {
                Map<String, Object> data = new HashMap<>();
                data.put("refundRequestId", refund.getRefundRequestId());
                data.put("refundAmount", refund.getRefundAmount());
                data.put("status", refund.getStatus().toString());
                data.put("cancelReason", refund.getCancelReason());
                data.put("adminNotes", refund.getAdminNotes());
                data.put("createdDate", refund.getCreatedDate());
                data.put("processedDate", refund.getProcessedDate());
                data.put("isWithinTwoHours", refund.isWithinTwoHours());
                data.put("transferProofImagePath", refund.getTransferProofImagePath());
                
                // Booking info
                if (refund.getBooking() != null) {
                    data.put("bookingCode", refund.getBooking().getBookingCode());
                    data.put("bookingId", refund.getBooking().getBookingId());
                }
                
                // Customer info
                if (refund.getUser() != null) {
                    data.put("customerName", refund.getUser().getFirstName() + " " + refund.getUser().getLastName());
                    data.put("customerEmail", refund.getUser().getEmail());
                    data.put("customerPhone", refund.getUser().getPhoneNumber());
                    data.put("userId", refund.getUser().getId());
                }
                
                // Bank account info
                if (refund.getBankAccount() != null) {
                    data.put("bankName", refund.getBankAccount().getBankName());
                    data.put("bankCode", refund.getBankAccount().getBankCode());
                    data.put("accountNumber", refund.getBankAccount().getAccountNumber());
                    data.put("accountHolder", refund.getBankAccount().getAccountHolderName());
                    data.put("qrCodeImagePath", refund.getBankAccount().getQrCodeImagePath());
                }
                
                return data;
            }).collect(Collectors.toList());
            
            return ResponseEntity.ok(refundData);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Get pending refund requests only (or all if no pending)
     */
    @GetMapping("/pending")
    public ResponseEntity<List<Map<String, Object>>> getPendingRefundRequests() {
        try {
            List<RefundRequest> refundRequests = refundRequestRepository.findPendingRequestsOrderByCreatedDate();
            
            // If no pending requests, return all requests sorted by date
            if (refundRequests.isEmpty()) {
                refundRequests = refundRequestRepository.findAll();
                if (!refundRequests.isEmpty()) {
                    refundRequests.sort((a, b) -> b.getCreatedDate().compareTo(a.getCreatedDate()));
                }
            }
            
            List<Map<String, Object>> refundData = refundRequests.stream().map(refund -> {
                Map<String, Object> data = new HashMap<>();
                data.put("refundRequestId", refund.getRefundRequestId());
                data.put("refundAmount", refund.getRefundAmount());
                data.put("status", refund.getStatus().toString());
                data.put("cancelReason", refund.getCancelReason());
                data.put("createdDate", refund.getCreatedDate());
                data.put("isWithinTwoHours", refund.isWithinTwoHours());
                
                if (refund.getBooking() != null) {
                    data.put("bookingCode", refund.getBooking().getBookingCode());
                    data.put("bookingId", refund.getBooking().getBookingId());
                }
                
                if (refund.getUser() != null) {
                    data.put("customerName", refund.getUser().getFirstName() + " " + refund.getUser().getLastName());
                    data.put("customerEmail", refund.getUser().getEmail());
                    data.put("customerPhone", refund.getUser().getPhoneNumber());
                }
                
                if (refund.getBankAccount() != null) {
                    data.put("bankName", refund.getBankAccount().getBankName());
                    data.put("bankCode", refund.getBankAccount().getBankCode());
                    data.put("accountNumber", refund.getBankAccount().getAccountNumber());
                    data.put("accountHolder", refund.getBankAccount().getAccountHolderName());
                    data.put("qrCodeImagePath", refund.getBankAccount().getQrCodeImagePath());
                }
                
                return data;
            }).collect(Collectors.toList());
            
            return ResponseEntity.ok(refundData);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Get refund request by booking ID
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
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Get refund request by ID with full details
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getRefundRequestById(@PathVariable String id) {
        try {
            return refundRequestRepository.findById(id)
                .map(refund -> {
                    Map<String, Object> data = new HashMap<>();
                    data.put("refundRequestId", refund.getRefundRequestId());
                    data.put("refundAmount", refund.getRefundAmount());
                    data.put("status", refund.getStatus().toString());
                    data.put("cancelReason", refund.getCancelReason());
                    data.put("adminNotes", refund.getAdminNotes());
                    data.put("createdDate", refund.getCreatedDate());
                    data.put("processedDate", refund.getProcessedDate());
                    data.put("isWithinTwoHours", refund.isWithinTwoHours());
                    data.put("transferProofImagePath", refund.getTransferProofImagePath());
                    
                    if (refund.getBooking() != null) {
                        data.put("bookingCode", refund.getBooking().getBookingCode());
                        data.put("bookingId", refund.getBooking().getBookingId());
                    }
                    
                    if (refund.getUser() != null) {
                        data.put("customerName", refund.getUser().getFirstName() + " " + refund.getUser().getLastName());
                        data.put("customerEmail", refund.getUser().getEmail());
                        data.put("customerPhone", refund.getUser().getPhoneNumber());
                        data.put("userId", refund.getUser().getId());
                    }
                    
                    if (refund.getBankAccount() != null) {
                        data.put("bankName", refund.getBankAccount().getBankName());
                        data.put("bankCode", refund.getBankAccount().getBankCode());
                        data.put("accountNumber", refund.getBankAccount().getAccountNumber());
                        data.put("accountHolder", refund.getBankAccount().getAccountHolderName());
                        data.put("qrCodeImagePath", refund.getBankAccount().getQrCodeImagePath());
                    }
                    
                    return ResponseEntity.ok(data);
                })
                .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Approve refund request
     */
    @PostMapping("/{id}/approve")
    public ResponseEntity<Map<String, Object>> approveRefundRequest(
            @PathVariable String id,
            @RequestParam(required = false) String adminNotes) {
        try {
            RefundRequest refund = refundRequestRepository.findById(id)
                    .orElse(null);
            
            if (refund == null) {
                return ResponseEntity.status(404).body(Map.of("status", "error", "message", "Refund request not found"));
            }
            
            if (refund.getStatus() != RefundRequest.RefundStatus.Pending) {
                return ResponseEntity.status(400).body(Map.of("status", "error", "message", "Only pending requests can be approved"));
            }
            
            // Get current admin user ID (you may need to adjust this based on your authentication)
            String adminUserId = "admin"; // TODO: Get from authentication context
            
            refundRequestService.approveRefundRequest(id, adminUserId, adminNotes != null ? adminNotes : "");
            
            return ResponseEntity.ok(Map.of("status", "success", "message", "Refund request approved successfully"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("status", "error", "message", "Failed to approve refund request: " + e.getMessage()));
        }
    }

    /**
     * Reject refund request
     */
    @PostMapping("/{id}/reject")
    public ResponseEntity<Map<String, Object>> rejectRefundRequest(
            @PathVariable String id,
            @RequestParam(required = false) String adminNotes) {
        try {
            RefundRequest refund = refundRequestRepository.findById(id)
                    .orElse(null);
            
            if (refund == null) {
                return ResponseEntity.status(404).body(Map.of("status", "error", "message", "Refund request not found"));
            }
            
            if (refund.getStatus() != RefundRequest.RefundStatus.Pending) {
                return ResponseEntity.status(400).body(Map.of("status", "error", "message", "Only pending requests can be rejected"));
            }
            
            // Get current admin user ID (you may need to adjust this based on your authentication)
            String adminUserId = "admin"; // TODO: Get from authentication context
            
            refundRequestService.rejectRefundRequest(id, adminUserId, adminNotes != null ? adminNotes : "");
            
            return ResponseEntity.ok(Map.of("status", "success", "message", "Refund request rejected successfully"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("status", "error", "message", "Failed to reject refund request: " + e.getMessage()));
        }
    }

    /**
     * Sync RefundPending bookings to RefundRequests
     * This endpoint creates RefundRequest for all bookings with RefundPending status that don't have a refund request yet
     */
    @PostMapping("/sync-pending-bookings")
    public ResponseEntity<Map<String, Object>> syncPendingBookings() {
        try {
            System.out.println("=== SYNC PENDING BOOKINGS ===");
            int count = 0;
            // Get all bookings with RefundPending status
            List<Booking> refundPendingBookings = bookingRepository.findByStatus(Booking.BookingStatus.RefundPending);
            System.out.println("Found RefundPending bookings: " + refundPendingBookings.size());
            
            for (Booking booking : refundPendingBookings) {
                System.out.println("Processing booking: " + booking.getBookingId() + ", status: " + booking.getStatus());
                // Check if RefundRequest already exists for this booking
                Optional<RefundRequest> existingRequest = refundRequestRepository.findByBookingBookingId(booking.getBookingId());
                
                if (existingRequest.isEmpty()) {
                    // Create RefundRequest for this booking
                    try {
                        RefundRequest refundRequest = new RefundRequest();
                        refundRequest.setRefundRequestId(UUID.randomUUID().toString());
                        refundRequest.setBooking(booking);
                        refundRequest.setUser(booking.getUser());
                        refundRequest.setRefundAmount(BigDecimal.ZERO); // Will be calculated by admin
                        refundRequest.setCancelReason(booking.getCancelReason() != null ? booking.getCancelReason() : "Customer cancelled");
                        refundRequest.setStatus(RefundRequest.RefundStatus.Pending);
                        refundRequest.setWithinTwoHours(false);
                        
                        // Try to get bank account
                        Optional<BankAccount> bankAccount = bankAccountService.getDefaultBankAccount(booking.getUser().getId());
                        if (bankAccount.isPresent()) {
                            refundRequest.setBankAccount(bankAccount.get());
                        }
                        
                        refundRequestRepository.save(refundRequest);
                        count++;
                    } catch (Exception e) {
                        System.out.println("Error creating RefundRequest for booking " + booking.getBookingId() + ": " + e.getMessage());
                    }
                }
            }
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Synced " + count + " RefundPending bookings to RefundRequests",
                "count", count
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                "status", "error",
                "message", "Failed to sync bookings: " + e.getMessage()
            ));
        }
    }

    /**
     * Debug endpoint to check all refund payments
     */
    @GetMapping("/debug/refund-payments")
    public ResponseEntity<Map<String, Object>> debugRefundPayments() {
        try {
            List<com.ecodana.evodanavn1.model.Payment> refundPayments = paymentRepository.findByPaymentStatus(com.ecodana.evodanavn1.model.Payment.PaymentStatus.Refunded);
            System.out.println("DEBUG: Found " + refundPayments.size() + " Refunded payments");
            
            List<Map<String, String>> paymentsList = new java.util.ArrayList<>();
            for (com.ecodana.evodanavn1.model.Payment p : refundPayments) {
                Map<String, String> pMap = new HashMap<>();
                pMap.put("paymentId", p.getPaymentId());
                pMap.put("bookingId", p.getBooking() != null ? p.getBooking().getBookingId() : "null");
                pMap.put("bookingCode", p.getBooking() != null ? p.getBooking().getBookingCode() : "null");
                pMap.put("amount", p.getAmount().toString());
                pMap.put("status", p.getPaymentStatus().toString());
                paymentsList.add(pMap);
            }
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "count", refundPayments.size(),
                "payments", paymentsList
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Debug endpoint to check all RefundRequests
     */
    @GetMapping("/debug/all-refund-requests")
    public ResponseEntity<Map<String, Object>> debugAllRefundRequests() {
        try {
            List<RefundRequest> allRequests = refundRequestRepository.findAll();
            System.out.println("DEBUG: Found " + allRequests.size() + " RefundRequests");
            
            List<Map<String, String>> requestsList = new java.util.ArrayList<>();
            for (RefundRequest rr : allRequests) {
                Map<String, String> rrMap = new HashMap<>();
                rrMap.put("refundRequestId", rr.getRefundRequestId());
                rrMap.put("bookingId", rr.getBooking() != null ? rr.getBooking().getBookingId() : "null");
                rrMap.put("bookingCode", rr.getBooking() != null ? rr.getBooking().getBookingCode() : "null");
                rrMap.put("amount", rr.getRefundAmount().toString());
                rrMap.put("status", rr.getStatus().toString());
                rrMap.put("createdDate", rr.getCreatedDate().toString());
                requestsList.add(rrMap);
            }
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "count", allRequests.size(),
                "refundRequests", requestsList
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Debug endpoint to check RefundPending bookings
     */
    @GetMapping("/debug/refund-pending-bookings")
    public ResponseEntity<Map<String, Object>> debugRefundPendingBookings() {
        try {
            List<Booking> refundPendingBookings = bookingRepository.findByStatus(Booking.BookingStatus.RefundPending);
            System.out.println("DEBUG: Found " + refundPendingBookings.size() + " RefundPending bookings");
            
            List<Map<String, String>> bookingsList = new java.util.ArrayList<>();
            for (Booking b : refundPendingBookings) {
                Map<String, String> bMap = new HashMap<>();
                bMap.put("bookingId", b.getBookingId());
                bMap.put("bookingCode", b.getBookingCode());
                bMap.put("status", b.getStatus().toString());
                bMap.put("userId", b.getUser().getId());
                bookingsList.add(bMap);
            }
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "count", refundPendingBookings.size(),
                "bookings", bookingsList
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Mark refund as transferred (Đã chuyển tiền) with transfer proof image
     * Handles both Pending and Approved status
     */
    @PostMapping("/{id}/mark-transferred")
    public ResponseEntity<Map<String, Object>> markRefundTransferred(
            @PathVariable String id,
            @RequestParam(required = false) String transferProofImagePath) {
        try {
            RefundRequest refund = refundRequestRepository.findById(id)
                    .orElse(null);
            
            if (refund == null) {
                return ResponseEntity.status(404).body(Map.of("status", "error", "message", "Refund request not found"));
            }
            
            // Only allow Pending status
            if (refund.getStatus() != RefundRequest.RefundStatus.Pending) {
                return ResponseEntity.status(400).body(Map.of("status", "error", "message", "Only pending requests can be marked as transferred"));
            }
            
            // Now mark as transferred
            refundRequestService.markRefundTransferred(id, transferProofImagePath);
            
            return ResponseEntity.ok(Map.of(
                "status", "success", 
                "message", "Refund marked as transferred successfully",
                "refundRequestId", id
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("status", "error", "message", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(400).body(Map.of("status", "error", "message", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("status", "error", "message", "Failed to mark refund as transferred: " + e.getMessage()));
        }
    }

    /**
     * Mark refund as completed with transfer proof image and message
     */
    @PostMapping("/{id}/mark-completed")
    public ResponseEntity<Map<String, Object>> markRefundCompleted(
            @PathVariable String id,
            @RequestParam(required = false) String transferProofImagePath,
            @RequestParam(required = false) String transferMessage) {
        try {
            RefundRequest refund = refundRequestRepository.findById(id)
                    .orElse(null);
            
            if (refund == null) {
                return ResponseEntity.status(404).body(Map.of("status", "error", "message", "Refund request not found"));
            }
            
            if (refund.getStatus() != RefundRequest.RefundStatus.Pending) {
                return ResponseEntity.status(400).body(Map.of("status", "error", "message", "Only pending requests can be marked as completed"));
            }
            
            // Update refund with transfer proof image
            refund.setStatus(RefundRequest.RefundStatus.Refunded);
            refund.setTransferProofImagePath(transferProofImagePath);
            refund.setProcessedDate(java.time.LocalDateTime.now());
            
            refundRequestRepository.save(refund);
            
            return ResponseEntity.ok(Map.of(
                "status", "success", 
                "message", "Refund marked as completed successfully",
                "refundRequestId", id
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("status", "error", "message", "Failed to mark refund as completed: " + e.getMessage()));
        }
    }

    /**
     * Upload transfer proof image for refund
     */
    @PostMapping("/upload-transfer-proof")
    public ResponseEntity<Map<String, Object>> uploadTransferProof(
            @RequestParam("file") MultipartFile file,
            @RequestParam("refundRequestId") String refundRequestId) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.status(400).body(Map.of("status", "error", "message", "File is empty"));
            }

            // Upload to Cloudinary
            @SuppressWarnings("unchecked")
            Map<String, Object> uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                    "folder", "ecodana/refund-proofs",
                    "resource_type", "auto"
            ));

            String imageUrl = (String) uploadResult.get("secure_url");

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Transfer proof uploaded successfully",
                    "imageUrl", imageUrl
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                    "status", "error",
                    "message", "Failed to upload transfer proof: " + e.getMessage()
            ));
        }
    }

    /**
     * Update refund request status
     */
    @PostMapping("/{id}/update-status")
    public ResponseEntity<Map<String, Object>> updateRefundStatus(
            @PathVariable String id,
            @RequestParam String status) {
        try {
            RefundRequest refund = refundRequestRepository.findById(id)
                    .orElse(null);
            
            if (refund == null) {
                return ResponseEntity.status(404).body(Map.of("status", "error", "message", "Refund request not found"));
            }
            
            // Update status
            try {
                RefundRequest.RefundStatus newStatus = RefundRequest.RefundStatus.valueOf(status);
                refund.setStatus(newStatus);
                refundRequestRepository.save(refund);
                
                return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Status updated successfully",
                    "newStatus", newStatus.toString()
                ));
            } catch (IllegalArgumentException e) {
                return ResponseEntity.status(400).body(Map.of("status", "error", "message", "Invalid status: " + status));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("status", "error", "message", "Failed to update status: " + e.getMessage()));
        }
    }

    /**
     * Get statistics for refund requests
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getRefundStatistics() {
        try {
            List<RefundRequest> allRefunds = refundRequestRepository.findAll();
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("total", allRefunds.size());
            stats.put("pending", allRefunds.stream().filter(r -> r.getStatus() == RefundRequest.RefundStatus.Pending).count());
            stats.put("refunded", allRefunds.stream().filter(r -> r.getStatus() == RefundRequest.RefundStatus.Refunded).count());
            stats.put("rejected", allRefunds.stream().filter(r -> r.getStatus() == RefundRequest.RefundStatus.Rejected).count());
            stats.put("urgent", refundRequestRepository.findUrgentPendingRequests().size());
            
            // Calculate total refund amount for pending requests
            double totalPendingAmount = allRefunds.stream()
                    .filter(r -> r.getStatus() == RefundRequest.RefundStatus.Pending)
                    .mapToDouble(r -> r.getRefundAmount().doubleValue())
                    .sum();
            stats.put("totalPendingAmount", totalPendingAmount);
            
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * API endpoint for admin to get customer's bank accounts by user ID
     * Used by admin refund detail modal to display customer bank account information
     */
    @GetMapping("/customer-bank-accounts/{userId}")
    public ResponseEntity<?> getCustomerBankAccounts(@PathVariable String userId) {
        try {
            if (userId == null || userId.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "User ID is required"
                ));
            }

            List<BankAccount> bankAccounts = bankAccountService.getBankAccountsByUserId(userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("accounts", bankAccounts);
            response.put("count", bankAccounts.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "Error fetching bank accounts: " + e.getMessage()
            ));
        }
    }
}
