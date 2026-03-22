package com.ecodana.evodanavn1.controller.api;

import com.ecodana.evodanavn1.model.Payment;
import com.ecodana.evodanavn1.repository.PaymentRepository;
import com.ecodana.evodanavn1.service.BookingService;
import com.ecodana.evodanavn1.service.PayOSService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import com.ecodana.evodanavn1.client.PayOSClient;
import com.ecodana.evodanavn1.config.PayOSConfig;
import com.ecodana.evodanavn1.model.Booking;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payos")
public class PayOSController {
    private static final Logger logger = LoggerFactory.getLogger(PayOSController.class);

    @Autowired
    private PayOSService payOSService;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private PayOSClient payOSClient;

    @Autowired
    private PaymentRepository paymentRepository;


    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Webhook nhận thông báo từ PayOS khi có sự kiện thanh toán
     */
    @PostMapping("/webhook")
    public ResponseEntity<Map<String, Object>> handleWebhook(@RequestBody String payload) {
        try {
            logger.info("=== WEBHOOK RECEIVED ===");
            logger.info("Payload: {}", payload);

            // Xác thực webhook
            if (!payOSService.verifyWebhook(payload)) {
                logger.warn("Invalid webhook signature");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(createErrorResponse("Invalid signature"));
            }

            // Parse thông tin từ webhook
            JsonNode data = objectMapper.readTree(payload).get("data");
            String orderCode = data.get("orderCode").asText();
            long amount = data.get("amount").asLong();
            String transactionId = data.get("transactionId") != null ? data.get("transactionId").asText() : "";
            String status = data.get("status").asText();

            logger.info("Webhook Details - OrderCode: {}, Amount: {}, Status: {}, TransactionId: {}",
                    orderCode, amount, status, transactionId);

            // Xử lý theo trạng thái thanh toán
            if ("PAID".equals(status)) {
                logger.info("Processing PAID status for order: {}", orderCode);
                payOSService.handlePaymentSuccess(orderCode, amount, transactionId);
                logger.info("Payment successful for order: {}", orderCode);
            } else if ("CANCELLED".equals(status) || "FAILED".equals(status)) {
                // Xử lý khi thanh toán thất bại hoặc bị hủy
                logger.warn("Payment failed or cancelled for order: {}", orderCode);
            }

            logger.info("=== WEBHOOK PROCESSED ===");
            return ResponseEntity.ok(createSuccessResponse("Webhook processed successfully"));

        } catch (Exception e) {
            logger.error("Error processing PayOS webhook", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error processing webhook: " + e.getMessage()));
        }
    }

    /**
     * API để client gọi lấy thông tin thanh toán
     */
    @GetMapping("/payment-info/{orderCode}")
    public ResponseEntity<Map<String, Object>> getPaymentInfo(@PathVariable String orderCode) {
        try {
            String paymentInfo = payOSService.getPaymentInfo(orderCode);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", objectMapper.readTree(paymentInfo));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting payment info for order: " + orderCode, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error getting payment info: " + e.getMessage()));
        }
    }

    /**
     * API để client gọi hoàn tiền
     */
    @PostMapping("/refund")
    public ResponseEntity<Map<String, Object>> processRefund(
            @RequestParam String transactionId,
            @RequestParam long amount,
            @RequestParam(required = false, defaultValue = "Refund by customer") String reason) {
        try {
            String result = payOSService.processRefund(transactionId, amount, reason);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", objectMapper.readTree(result));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error processing refund for transaction: " + transactionId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error processing refund: " + e.getMessage()));
        }
    }

    /**
     * API để client hủy link thanh toán
     */
    @PostMapping("/cancel")
    public ResponseEntity<Map<String, Object>> cancelPaymentLink(@RequestParam String orderCode) {
        try {
            String result = payOSService.cancelPaymentLink(orderCode);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", objectMapper.readTree(result));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error canceling payment link for order: " + orderCode, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error canceling payment link: " + e.getMessage()));
        }
    }

    private Map<String, Object> createSuccessResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", message);
        return response;
    }

    private Map<String, Object> createErrorResponse(String error) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", error);
        return response;
    }
    /**
     * API tạo QR code thanh toán phần còn lại (Dùng cho modal Chuyển khoản của Chủ xe)
     */
    @PostMapping("/create-completion-payment")
    @ResponseBody
    public ResponseEntity<?> createCompletionPaymentLink(@RequestBody Map<String, Object> payload) {
        try {
            String bookingId = (String) payload.get("bookingId");

            // FIX 1: Kiểm tra NULL cho bookingId (Ngăn lỗi: The given id must not be null)
            if (bookingId == null || bookingId.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("success", false, "message", "Thiếu Booking ID. Vui lòng thử lại."));
            }

            long amount;
            Object amountObject = payload.get("amount");

            // FIX 2: Chuyển đổi an toàn sang long (Ngăn lỗi: String cannot be cast to Number)
            if (amountObject == null) {
                amount = 0; // Nếu không có, gán 0 (Service sẽ kiểm tra và ném lỗi nếu <= 0)
            } else if (amountObject instanceof Number) {
                amount = ((Number) amountObject).longValue();
            } else {
                // Xử lý nếu là String (chuỗi số từ JS)
                String amountString = String.valueOf(amountObject);
                amount = Long.parseLong(amountString.trim());
            }

            String description = "Thanh toan hoan tat cho booking " + bookingId;

            // Gọi service
            Map<String, Object> result = payOSService.createCompletionPaymentLink(bookingId, amount, description);

            return ResponseEntity.ok(result);
        } catch (NumberFormatException e) {
            logger.error("Number format error in createCompletionPaymentLink:", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", "Lỗi định dạng số tiền. Vui lòng kiểm tra lại số dư còn lại."));
        } catch (Exception e) {
            logger.error("Lỗi khi tạo link thanh toán hoàn tất:", e);

            // FIX 3: Trích xuất lỗi PayOS chi tiết
            String errorMessage = e.getMessage();
            if (errorMessage.contains("PayOS says:")) {
                String payosDetail = errorMessage.substring(errorMessage.indexOf("PayOS says:") + 12);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("success", false, "message", "Lỗi PayOS: " + payosDetail));
            }

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Lỗi tạo mã thanh toán: " + errorMessage));
        }
    }


    /**
     * Polling kiểm tra trạng thái thanh toán
     */
    @GetMapping("/check-payment-status/{orderCode}")
    @ResponseBody
    @Transactional
    public ResponseEntity<?> checkPaymentStatus(@PathVariable String orderCode) {
        try {
            String status = payOSService.getPaymentStatus(orderCode); // Trả về PAID, PENDING, v.v.

            if ("PAID".equals(status)) {
                paymentRepository.findByOrderCode(orderCode).ifPresent(payment -> {
                    if (payment.getPaymentStatus() != Payment.PaymentStatus.Completed) {
                        payment.setPaymentStatus(Payment.PaymentStatus.Completed);
                        paymentRepository.save(payment);
                        logger.info("Payment status for order code {} updated to COMPLETED via polling.", orderCode);
                    }
                });
            }

            return ResponseEntity.ok(Map.of("status", status));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Lỗi check status: " + e.getMessage()));
        }
    }
}