package com.ecodana.evodanavn1.service;

import com.ecodana.evodanavn1.model.*;
import com.ecodana.evodanavn1.repository.RefundRequestRepository;
import com.ecodana.evodanavn1.repository.PaymentRepository;
import com.ecodana.evodanavn1.repository.BookingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefundRequestService {

    @Autowired
    private RefundRequestRepository refundRequestRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private BankAccountService bankAccountService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private com.ecodana.evodanavn1.repository.BankAccountRepository bankAccountRepository;

    @Transactional
    public RefundRequest createRefundRequest(Booking booking, User user, String cancelReason) {
        return createRefundRequest(booking, user, cancelReason, null, null);
    }

    @Transactional
    public RefundRequest createRefundRequest(Booking booking, User user, String cancelReason, String bankAccountId) {
        return createRefundRequest(booking, user, cancelReason, bankAccountId, null);
    }

    @Transactional
    public RefundRequest createRefundRequest(Booking booking, User user, String cancelReason, String bankAccountId, BigDecimal customRefundAmount) {
        // Check if refund request already exists
        Optional<RefundRequest> existingRequest = refundRequestRepository.findByBookingBookingId(booking.getBookingId());
        if (existingRequest.isPresent()) {
            throw new IllegalStateException("Yêu cầu hoàn tiền cho đơn hàng này đã tồn tại!");
        }

        // Get bank account - use provided ID or default
        Optional<BankAccount> selectedBankAccount = Optional.empty();
        
        if (bankAccountId != null && !bankAccountId.isEmpty()) {
            // Use the provided bank account ID
            selectedBankAccount = bankAccountRepository.findById(bankAccountId);
        } else {
            // Fall back to default bank account
            selectedBankAccount = bankAccountService.getDefaultBankAccount(user.getId());
        }

        // Calculate refund amount - use custom amount if provided, otherwise calculate
        BigDecimal refundAmount = customRefundAmount != null ? customRefundAmount : calculateRefundAmount(booking);

        // Check if within 2 hours of payment
        boolean isWithinTwoHours = isWithinTwoHoursOfPayment(booking);

        // Create refund request
        RefundRequest refundRequest = new RefundRequest();
        refundRequest.setRefundRequestId(UUID.randomUUID().toString());
        refundRequest.setBooking(booking);
        refundRequest.setUser(user);
        
        // Set bank account if available, otherwise leave null (admin can add later)
        if (selectedBankAccount.isPresent()) {
            refundRequest.setBankAccount(selectedBankAccount.get());
        }
        
        refundRequest.setRefundAmount(refundAmount);
        refundRequest.setCancelReason(cancelReason);
        refundRequest.setStatus(RefundRequest.RefundStatus.Pending);
        refundRequest.setWithinTwoHours(isWithinTwoHours);

        RefundRequest savedRequest = refundRequestRepository.save(refundRequest);

        // Note: Booking status should already be set to RefundPending by the caller (BookingService)
        // Only update if booking is not already in RefundPending status
        if (booking.getStatus() != Booking.BookingStatus.RefundPending) {
            booking.setStatus(Booking.BookingStatus.RefundPending);
            booking.setCancelReason(cancelReason);
            bookingRepository.save(booking);
        }

        // Send notification to admin
        notifyAdminOfRefundRequest(savedRequest);

        return savedRequest;
    }

    private BigDecimal calculateRefundAmount(Booking booking) {
        // Get all payments
        List<Payment> payments = paymentRepository.findByBookingId(booking.getBookingId());
        System.out.println("DEBUG: Total payments found: " + payments.size());
        for (Payment p : payments) {
            System.out.println("  - Payment: " + p.getPaymentId() + ", Type: " + p.getPaymentType() + ", Status: " + p.getPaymentStatus() + ", Amount: " + p.getAmount());
        }
        
        // ONLY refund payments that are actually COMPLETED (đã thanh toán thực sự)
        // Do NOT include Pending payments - they haven't been paid yet!
        List<Payment> refundablePayments = payments.stream()
                .filter(p -> p.getPaymentStatus() == Payment.PaymentStatus.Completed)
                .filter(p -> p.getPaymentType() == Payment.PaymentType.Deposit || 
                            p.getPaymentType() == Payment.PaymentType.FinalPayment)
                .toList();
        
        System.out.println("DEBUG: Refundable payments (COMPLETED only) found: " + refundablePayments.size());
        for (Payment p : refundablePayments) {
            System.out.println("  - Refundable: " + p.getPaymentId() + ", Type: " + p.getPaymentType() + ", Status: " + p.getPaymentStatus() + ", Amount: " + p.getAmount());
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime paymentTime = booking.getPaymentConfirmedAt();

        if (paymentTime == null) {
            // Fallback: sum all refundable payments
            BigDecimal fallbackAmount = refundablePayments.stream()
                    .map(Payment::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            System.out.println("DEBUG: paymentTime is null, using fallback amount: " + fallbackAmount);
            return fallbackAmount;
        }

        long hoursSincePayment = Duration.between(paymentTime, now).toHours();
        System.out.println("DEBUG: Hours since payment: " + hoursSincePayment);

        BigDecimal totalRefundAmount = BigDecimal.ZERO;

        // Calculate refund for each payment type separately
        for (Payment payment : refundablePayments) {
            BigDecimal paymentRefundAmount;

            if (hoursSincePayment < 2) {
                // Hủy trong 2 giờ -> Hoàn 100% cho cả Deposit và FinalPayment
                paymentRefundAmount = payment.getAmount();
            } else {
                // Hủy sau 2 giờ -> Hoàn 70% (trừ 30% phí hủy)
                BigDecimal penalty = payment.getAmount().multiply(new BigDecimal("0.30"));
                paymentRefundAmount = payment.getAmount().subtract(penalty);
            }

            totalRefundAmount = totalRefundAmount.add(paymentRefundAmount);
            System.out.println("DEBUG: Payment " + payment.getPaymentId() + " refund: " + paymentRefundAmount);
        }

        System.out.println("DEBUG: Total refund amount: " + totalRefundAmount);
        return totalRefundAmount.max(BigDecimal.ZERO);
    }

    private boolean isWithinTwoHoursOfPayment(Booking booking) {
        if (booking.getPaymentConfirmedAt() == null) {
            return false;
        }
        
        LocalDateTime now = LocalDateTime.now();
        long hoursSincePayment = Duration.between(booking.getPaymentConfirmedAt(), now).toHours();
        return hoursSincePayment < 2;
    }

    private void notifyAdminOfRefundRequest(RefundRequest refundRequest) {
        String message = String.format(
            "Yêu cầu hoàn tiền mới từ %s cho đơn hàng #%s. Số tiền: %s VNĐ. %s",
            refundRequest.getUser().getUsername(),
            refundRequest.getBooking().getBookingCode(),
            refundRequest.getRefundAmount(),
            refundRequest.isWithinTwoHours() ? "⚡ KHẨN CẤP - Trong vòng 2 giờ!" : ""
        );

        notificationService.createNotificationForAllAdmins(
            message,
            refundRequest.getRefundRequestId(),
            "REFUND_REQUEST"
        );
    }

    public List<RefundRequest> getRefundRequestsByUserId(String userId) {
        return refundRequestRepository.findByUserIdOrderByCreatedDateDesc(userId);
    }

    public List<RefundRequest> getPendingRefundRequests() {
        return refundRequestRepository.findPendingRequestsOrderByCreatedDate();
    }

    public List<RefundRequest> getUrgentRefundRequests() {
        return refundRequestRepository.findUrgentPendingRequests();
    }

    public long countPendingRequests() {
        return refundRequestRepository.countPendingRequests();
    }

    public Optional<RefundRequest> getRefundRequestById(String refundRequestId) {
        return refundRequestRepository.findById(refundRequestId);
    }

    public List<RefundRequest> getRefundRequestsByBookingId(String bookingId) {
        Optional<RefundRequest> refundRequest = refundRequestRepository.findByBookingBookingId(bookingId);
        return refundRequest.map(List::of).orElse(List.of());
    }

    @Transactional
    public void approveRefundRequest(String refundRequestId, String adminUserId, String adminNotes) {
        RefundRequest refundRequest = refundRequestRepository.findById(refundRequestId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy yêu cầu hoàn tiền"));

        refundRequest.setStatus(RefundRequest.RefundStatus.Refunded);
        refundRequest.setProcessedBy(adminUserId);
        refundRequest.setProcessedDate(LocalDateTime.now());
        refundRequest.setAdminNotes(adminNotes);

        refundRequestRepository.save(refundRequest);

        // Update existing Payment: Completed -> Refunded, PayOS -> PayOS_Refund
        Booking booking = refundRequest.getBooking();
        List<Payment> existingPayments = paymentRepository.findByBookingId(booking.getBookingId());
        for (Payment payment : existingPayments) {
            // Only update Completed payments (Deposit/FinalPayment)
            if (payment.getPaymentStatus() == Payment.PaymentStatus.Completed && 
                (payment.getPaymentType() == Payment.PaymentType.Deposit || 
                 payment.getPaymentType() == Payment.PaymentType.FinalPayment)) {
                payment.setPaymentStatus(Payment.PaymentStatus.Refunded);
                payment.setPaymentMethod("PayOS_Refund"); // Change method to PayOS_Refund
                payment.setPaymentDate(LocalDateTime.now()); // Set refund date
                payment.setNotes("Đã hoàn tiền - RefundRequest: " + refundRequestId + " - " + adminNotes);
                paymentRepository.save(payment);
                System.out.println("Updated payment " + payment.getPaymentId() + " to Refunded with PayOS_Refund");
            }
        }

        // Update booking status to Refunded
        booking.setStatus(Booking.BookingStatus.Refunded);
        booking.setCancelReason("Admin đã duyệt hoàn tiền");
        bookingRepository.save(booking);

        // Notify customer that refund has been approved
        notificationService.createNotification(
            refundRequest.getUser().getId(),
            "Yêu cầu hoàn tiền đã được duyệt! Admin sẽ chuyển tiền vào tài khoản của bạn trong 1-3 ngày làm việc.",
            refundRequestId,
            "REFUND_APPROVED"
        );
    }

    @Transactional
    public void markRefundTransferred(String refundRequestId, String transferProofImagePath) {
        RefundRequest refundRequest = refundRequestRepository.findById(refundRequestId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy yêu cầu hoàn tiền"));

        // Only allow Pending status
        if (refundRequest.getStatus() != RefundRequest.RefundStatus.Pending) {
            throw new IllegalStateException("Chỉ có thể chuyển tiền cho các yêu cầu đang chờ");
        }

        // Set status to Refunded (Đã hoàn tiền) after upload
        refundRequest.setStatus(RefundRequest.RefundStatus.Refunded);
        refundRequest.setTransferProofImagePath(transferProofImagePath);
        refundRequest.setProcessedDate(LocalDateTime.now());
        refundRequestRepository.save(refundRequest);

        // Update existing Payment: Completed -> Refunded, PayOS -> PayOS_Refund
        Booking booking = refundRequest.getBooking();
        List<Payment> existingPayments = paymentRepository.findByBookingId(booking.getBookingId());
        for (Payment payment : existingPayments) {
            // Only update Completed payments (Deposit/FinalPayment)
            if (payment.getPaymentStatus() == Payment.PaymentStatus.Completed && 
                (payment.getPaymentType() == Payment.PaymentType.Deposit || 
                 payment.getPaymentType() == Payment.PaymentType.FinalPayment)) {
                payment.setPaymentStatus(Payment.PaymentStatus.Refunded);
                payment.setPaymentMethod("PayOS_Refund"); // Change method to PayOS_Refund
                payment.setPaymentDate(LocalDateTime.now()); // Set refund date
                payment.setNotes("Đã hoàn tiền - Ảnh: " + transferProofImagePath);
                paymentRepository.save(payment);
                System.out.println("Updated payment " + payment.getPaymentId() + " to Refunded with PayOS_Refund");
            }
        }

        // Update booking status to Refunded (đã hoàn tiền)
        booking.setStatus(Booking.BookingStatus.Refunded);
        booking.setCancelReason("Admin đã chuyển tiền hoàn lại");
        bookingRepository.save(booking);

        // Notify customer that refund has been completed with amount and image
        String message = String.format(
            "✅ Yêu cầu hoàn tiền của bạn đã được xử lý thành công!\n\n" +
            "Số tiền hoàn: %,.0f VNĐ\n" +
            "Ảnh chứng minh chuyển khoản: %s\n\n" +
            "Tiền sẽ được chuyển vào tài khoản ngân hàng của bạn trong 1-3 ngày làm việc.",
            refundRequest.getRefundAmount(),
            transferProofImagePath != null ? transferProofImagePath : "Không có"
        );
        
        notificationService.createNotification(
            refundRequest.getUser().getId(),
            message,
            refundRequestId,
            "REFUND_COMPLETED"
        );
    }

    @Transactional
    public void rejectRefundRequest(String refundRequestId, String adminUserId, String adminNotes) {
        RefundRequest refundRequest = refundRequestRepository.findById(refundRequestId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy yêu cầu hoàn tiền"));

        refundRequest.setStatus(RefundRequest.RefundStatus.Rejected);
        refundRequest.setProcessedBy(adminUserId);
        refundRequest.setProcessedDate(LocalDateTime.now());
        refundRequest.setAdminNotes(adminNotes);

        refundRequestRepository.save(refundRequest);

        // Update booking status to Cancelled (refund rejected)
        Booking booking = refundRequest.getBooking();
        booking.setStatus(Booking.BookingStatus.Cancelled);
        booking.setCancelReason("Admin từ chối hoàn tiền: " + adminNotes);
        bookingRepository.save(booking);

        // Notify customer
        notificationService.createNotification(
            refundRequest.getUser().getId(),
            "Yêu cầu hoàn tiền đã bị từ chối. Lý do: " + adminNotes,
            refundRequestId,
            "REFUND_REJECTED"
        );
    }
}
