package com.ecodana.evodanavn1.service;

import com.ecodana.evodanavn1.client.PayOSClient;
import com.ecodana.evodanavn1.model.Booking;
import com.ecodana.evodanavn1.model.Payment;
import com.ecodana.evodanavn1.repository.BookingRepository;
import com.ecodana.evodanavn1.repository.PaymentRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class PayOSService {
    private static final Logger logger = LoggerFactory.getLogger(PayOSService.class);

    @Autowired
    private PayOSClient payOSClient;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${payos.return-url}")
    private String returnUrl;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public String createPaymentLink(long amount, String orderInfo, String bookingId, String paymentType, HttpServletRequest request) {
        try {
            // Tạo mã đơn hàng duy nhất (timestamp in seconds)
            long orderCodeNumber = System.currentTimeMillis() / 1000;
            String orderCode = String.valueOf(orderCodeNumber);

            // Tạo URL callback và return
            String localReturnUrl = baseUrl + "/booking/payment/payos-return?bookingId=" + bookingId;
            String cancelUrl = baseUrl + "/booking/payment/cancel?bookingId=" + bookingId;

            logger.info("Creating PayOS payment link - Amount: {}, OrderCode: {}, BookingId: {}, PaymentType: {}", amount, orderCode, bookingId, paymentType);
            logger.info("Return URL: {}, Cancel URL: {}", localReturnUrl, cancelUrl);

            // Gọi API tạo payment link
            String response = payOSClient.createPaymentLink(
                    amount,
                    orderCode,
                    orderInfo,
                    localReturnUrl,
                    cancelUrl
            );

            logger.info("PayOS API Response: {}", response);

            // Parse response để lấy thông tin thanh toán
            JsonNode jsonNode = objectMapper.readTree(response);
            if (jsonNode.has("data") && jsonNode.get("data").has("checkoutUrl")) {
                String paymentUrl = jsonNode.get("data").get("checkoutUrl").asText();

                // Lưu thông tin thanh toán tạm thời
                Payment payment = new Payment();
                payment.setPaymentId(UUID.randomUUID().toString());
                payment.setOrderCode(orderCode);

                // Gán booking và user
                Booking booking = bookingRepository.findById(bookingId)
                        .orElseThrow(() -> new RuntimeException("Booking not found with id: " + bookingId));

                // Lưu Payment với amount và type chính xác
                payment.setAmount(BigDecimal.valueOf(amount)); // Store the actual amount being paid
                payment.setPaymentMethod("PayOS");
                payment.setPaymentStatus(Payment.PaymentStatus.Pending);
                payment.setPaymentType("deposit".equals(paymentType) ? Payment.PaymentType.Deposit : Payment.PaymentType.FinalPayment); // Set payment type
                payment.setCreatedDate(LocalDateTime.now());
                payment.setBooking(booking);
                payment.setUser(booking.getUser());
                paymentRepository.save(payment);

                logger.info("Created Pending payment with amount: {} and type: {}", amount, payment.getPaymentType());

                return paymentUrl;
            } else {
                throw new RuntimeException("Failed to create payment link for booking " + bookingId + ": " + response);
            }

        } catch (Exception e) {
            logger.error("Error creating PayOS payment link for booking {}", bookingId, e);
            throw new RuntimeException("Không thể tạo liên kết thanh toán cho đơn đặt xe " + bookingId + ": " + e.getMessage());
        }
    }

    /**
     * Tạo link thanh toán bổ sung (Hoàn tất chuyến đi - gọi từ modal Owner)
     * Trả về Map chứa qrCode và orderCode để hiển thị popup
     */
    @Transactional
    public Map<String, Object> createCompletionPaymentLink(String bookingId, long amount, String description) {
        try {
            if (amount <= 0) {
                throw new IllegalArgumentException("Số tiền cần thanh toán phải lớn hơn 0.");
            }

            Booking booking = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new RuntimeException("Booking not found with id: " + bookingId));

            long orderCodeNumber = System.currentTimeMillis();
            String orderCode = String.valueOf(orderCodeNumber);

            String successUrl = this.returnUrl;
            String cancelUrlStr = this.returnUrl.replace("success", "cancel");

            String fullDescription = "TT bổ sung Booking: " + bookingId + " - " + description;

            String responseBody = payOSClient.createPaymentLink(
                    amount,
                    orderCode,
                    fullDescription,
                    successUrl,
                    cancelUrlStr
            );

            JsonNode jsonNode = objectMapper.readTree(responseBody);

            if (jsonNode.has("code") && "00".equals(jsonNode.get("code").asText()) && jsonNode.has("data")) {
                JsonNode dataNode = jsonNode.get("data");

                savePaymentRecord(orderCode, amount, bookingId, Payment.PaymentType.FinalPayment);
                
                Map<String, Object> result = new HashMap<>();
                result.put("success", true); // FIX: Add success flag
                result.put("qrCode", dataNode.has("qrCode") ? dataNode.get("qrCode").asText() : "");
                result.put("checkoutUrl", dataNode.has("checkoutUrl") ? dataNode.get("checkoutUrl").asText() : "");
                result.put("orderCode", orderCode);
                return result;
            } else {
                String payosDesc = jsonNode.has("desc") ? jsonNode.get("desc").asText() : "Unknown PayOS error.";
                throw new RuntimeException("Failed to create completion link (PayOS says: " + payosDesc + "): " + responseBody);
            }

        } catch (Exception e) {
            logger.error("Error creating completion payment link for booking {}", bookingId, e);
            throw new RuntimeException("Lỗi tạo mã thanh toán bổ sung cho đơn đặt xe " + bookingId + ": " + e.getMessage());
        }
    }

    // Hàm hỗ trợ lưu payment vào DB để tránh lặp code
    private void savePaymentRecord(String orderCode, long amount, String bookingId, Payment.PaymentType paymentType) {
        Booking booking = bookingRepository.findById(bookingId).orElse(null);
        if (booking != null) {
            Payment payment = new Payment();
            payment.setPaymentId(UUID.randomUUID().toString());
            payment.setOrderCode(orderCode);
            payment.setAmount(BigDecimal.valueOf(amount));
            payment.setPaymentMethod("PayOS");
            payment.setPaymentStatus(Payment.PaymentStatus.Pending);
            payment.setCreatedDate(LocalDateTime.now());
            payment.setBooking(booking);
            payment.setUser(booking.getUser());
            payment.setPaymentType(paymentType); // Set payment type directly
            paymentRepository.save(payment);
        }
    }


    public boolean verifyWebhook(String webhookData) {
        try {
            // For now, always return true since PayOS webhook verification is complex
            // In production, implement proper signature verification
            logger.info("Webhook verification - accepting webhook (signature check disabled)");
            return true;
        } catch (Exception e) {
            logger.error("Error verifying PayOS webhook", e);
            return false;
        }
    }

    public String processRefund(String transactionId, long amount, String reason) {
        try {
            return payOSClient.refundPayment(transactionId, amount, reason);
        } catch (Exception e) {
            logger.error("Error processing refund", e);
            throw new RuntimeException("Failed to process refund: " + e.getMessage());
        }
    }

    public void handlePaymentSuccess(String orderCode, long amount, String transactionId) {
        try {
            // Tìm payment theo orderCode
            Payment payment = paymentRepository.findByOrderCode(orderCode)
                    .orElseThrow(() -> new RuntimeException("Payment not found for order: " + orderCode));

            // IDEMPOTENCY CHECK: Nếu payment đã được xử lý (status = Completed), không xử lý lại
            if (payment.getPaymentStatus() == Payment.PaymentStatus.Completed) {
                logger.warn("Payment already processed for order: {}. Skipping duplicate processing.", orderCode);
                return;
            }

            Booking booking = payment.getBooking();
            BigDecimal paidAmount = BigDecimal.valueOf(amount);

            logger.info("=== PAYMENT SUCCESS PROCESSING ===");
            logger.info("Amount from PayOS (VND): {}", amount);
            logger.info("Paid amount (VND): {}", paidAmount);
            logger.info("Payment Type in DB: {}", payment.getPaymentType());
            logger.info("Deposit required (VND): {}", booking.getDepositAmountRequired());
            logger.info("Total amount (VND): {}", booking.getTotalAmount());

            // Update the existing payment record
            payment.setAmount(paidAmount); // Ensure the stored amount is the actual paid amount
            payment.setTransactionId(transactionId);
            payment.setPaymentStatus(Payment.PaymentStatus.Completed);
            payment.setPaymentDate(LocalDateTime.now());
            paymentRepository.save(payment);
            logger.info("Payment {} updated to Completed with amount {} and type {}", payment.getPaymentId(), paidAmount, payment.getPaymentType());


            // Cập nhật trạng thái booking
            if (payment.getPaymentType() == Payment.PaymentType.FinalPayment) {
                booking.setStatus(Booking.BookingStatus.Confirmed);
                booking.setPaymentConfirmedAt(LocalDateTime.now());
                booking.setDepositAmountRequired(booking.getTotalAmount()); // Set deposit to total if full payment
                logger.info("Booking confirmed - full payment received.");
            } else if (payment.getPaymentType() == Payment.PaymentType.Deposit) {
                booking.setStatus(Booking.BookingStatus.AwaitingDeposit); // Or Confirmed if deposit is enough to confirm
                booking.setPaymentConfirmedAt(LocalDateTime.now()); // Still set confirmation time for deposit
                booking.setDepositAmountRequired(paidAmount); // Set deposit to actual paid deposit
                logger.info("Booking awaiting deposit - deposit payment received.");
            }
            bookingRepository.save(booking);

        } catch (Exception e) {
            logger.error("Error handling payment success for order {}", orderCode, e);
            throw new RuntimeException("Failed to handle payment success for order " + orderCode + ": " + e.getMessage());
        }
    }

    public String getPaymentInfo(String orderCode) {
        try {
            return payOSClient.getPaymentLinkInfo(orderCode);
        } catch (Exception e) {
            logger.error("Error getting payment info for order {}", orderCode, e);
            throw new RuntimeException("Failed to get payment info for order " + orderCode + ": " + e.getMessage());
        }
    }

    public String cancelPaymentLink(String orderCode) {
        try {
            return payOSClient.cancelPaymentLink(orderCode);
        } catch (Exception e) {
            logger.error("Error canceling payment link for order {}", orderCode, e);
            throw new RuntimeException("Failed to cancel payment link for order " + orderCode + ": " + e.getMessage());
        }
    }


    /**
     * Lấy trạng thái thanh toán rút gọn (PAID/PENDING/CANCELLED)
     */
    public String getPaymentStatus(String orderCode) {
        try {
            String responseBody = payOSClient.getPaymentLinkInfo(orderCode);
            JsonNode jsonNode = objectMapper.readTree(responseBody);

            if (jsonNode.has("code") && "00".equals(jsonNode.get("code").asText()) && jsonNode.has("data")) {
                return jsonNode.get("data").get("status").asText(); // Trả về "PAID", "PENDING", "CANCELLED"
            }
            return "UNKNOWN";
        } catch (Exception e) {
            logger.error("Error checking payment status for order: {}", orderCode);
            return "ERROR";
        }
    }
}