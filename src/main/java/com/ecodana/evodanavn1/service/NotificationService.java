package com.ecodana.evodanavn1.service;

import com.ecodana.evodanavn1.model.Notification;
import com.ecodana.evodanavn1.model.User;
import com.ecodana.evodanavn1.repository.NotificationRepository;
import com.ecodana.evodanavn1.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class NotificationService {
    
    @Autowired
    private NotificationRepository notificationRepository;
    
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BankAccountService bankAccountService;
    
    /**
     * Create notification for a specific user
     */
    public Notification createNotification(String userId, String message) {
        Notification notification = new Notification();
        notification.setNotificationId(UUID.randomUUID().toString());
        notification.setUserId(userId);
        notification.setMessage(message);
        return notificationRepository.save(notification);
    }
    
    /**
     * Create notification with related entity
     */
    public Notification createNotification(String userId, String message, String relatedId, String notificationType) {
        Notification notification = new Notification();
        notification.setNotificationId(UUID.randomUUID().toString());
        notification.setUserId(userId);
        notification.setMessage(message);
        notification.setRelatedId(relatedId);
        notification.setNotificationType(notificationType);
        return notificationRepository.save(notification);
    }
    
    /**
     * Create notification for all admins
     */
    public void createNotificationForAllAdmins(String message) {
        List<User> admins = userRepository.findByRoleName("ADMIN");
        for (User admin : admins) {
            createNotification(admin.getId(), message);
        }
    }
    
    /**
     * Create notification for all admins with related entity
     */
    public void createNotificationForAllAdmins(String message, String relatedId, String notificationType) {
        List<User> admins = userRepository.findByRoleName("ADMIN");
        for (User admin : admins) {
            createNotification(admin.getId(), message, relatedId, notificationType);
        }
    }
    
    /**
     * Get all notifications for a user
     */
    public List<Notification> getNotificationsByUserId(String userId) {
        return notificationRepository.findByUserIdOrderByCreatedDateDesc(userId);
    }
    
    /**
     * Get unread notifications for a user
     */
    public List<Notification> getUnreadNotifications(String userId) {
        return notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedDateDesc(userId);
    }
    
    /**
     * Count unread notifications
     */
    public long countUnreadNotifications(String userId) {
        return notificationRepository.countUnreadByUserId(userId);
    }
    
    /**
     * Mark notification as read
     */
    public void markAsRead(String notificationId) {
        notificationRepository.findById(notificationId).ifPresent(notification -> {
            notification.setIsRead(true);
            notificationRepository.save(notification);
        });
    }
    
    /**
     * Mark all notifications as read for a user
     */
    @Transactional
    public void markAllAsRead(String userId) {
        List<Notification> unreadNotifications = getUnreadNotifications(userId);
        for (Notification notification : unreadNotifications) {
            notification.setIsRead(true);
            notificationRepository.save(notification);
        }
    }
    
    /**
     * Delete notification
     */
    public void deleteNotification(String notificationId) {
        notificationRepository.deleteById(notificationId);
    }
    
    /**
     * Gửi thông báo khi có booking mới cho Owner
     */
    public void notifyOwnerNewBooking(com.ecodana.evodanavn1.model.Booking booking) {
        String ownerId = booking.getVehicle().getOwnerId();
        if (ownerId != null) {
            String message = String.format(
                "Bạn có yêu cầu đặt xe mới #%s. Vui lòng phản hồi trong vòng 2 giờ.",
                booking.getBookingCode()
            );
            createNotification(ownerId, message, booking.getBookingId(), "BOOKING_REQUEST");
        }
    }
    
    /**
     * Gửi thông báo khi Owner chấp nhận booking
     */
    public void notifyCustomerBookingApproved(com.ecodana.evodanavn1.model.Booking booking) {
        String customerId = booking.getUser().getId();
        String message = String.format(
            "Yêu cầu đặt xe #%s đã được chấp nhận. Vui lòng thanh toán để xác nhận đơn hàng.",
            booking.getBookingCode()
        );
        createNotification(customerId, message, booking.getBookingId(), "BOOKING_APPROVED");
    }
    
    /**
     * Gửi thông báo khi Owner từ chối booking
     */
    public void notifyCustomerBookingRejected(com.ecodana.evodanavn1.model.Booking booking, String reason) {
        String customerId = booking.getUser().getId();
        String message = String.format(
            "Yêu cầu đặt xe #%s đã bị từ chối. Lý do: %s",
            booking.getBookingCode(),
            reason != null ? reason : "Không có lý do cụ thể"
        );
        createNotification(customerId, message, booking.getBookingId(), "BOOKING_REJECTED");
    }
    
    /**
     * Gửi thông báo khi thanh toán thành công
     */
    public void notifyPaymentSuccess(com.ecodana.evodanavn1.model.Booking booking, 
                                     com.ecodana.evodanavn1.model.Payment payment) {
        // Thông báo cho Customer
        String customerMessage = String.format(
            "Thanh toán thành công %s VNĐ cho đơn hàng #%s. Cảm ơn bạn đã sử dụng dịch vụ!",
            payment.getAmount(),
            booking.getBookingCode()
        );
        createNotification(booking.getUser().getId(), customerMessage, payment.getPaymentId(), "PAYMENT_SUCCESS");
        
        // Thông báo cho Admin
        String adminMessage = String.format(
            "Đơn hàng #%s đã được thanh toán thành công. Số tiền: %s VNĐ. Khách hàng: %s",
            booking.getBookingCode(),
            payment.getAmount(),
            booking.getUser().getUsername()
        );
        createNotificationForAllAdmins(adminMessage, payment.getPaymentId(), "PAYMENT_SUCCESS");
        
        // Thông báo cho Owner
        String ownerId = booking.getVehicle().getOwnerId();
        if (ownerId != null) {
            String ownerMessage = String.format(
                "Đơn đặt xe #%s đã được thanh toán. Vui lòng chuẩn bị xe cho khách hàng.",
                booking.getBookingCode()
            );
            createNotification(ownerId, ownerMessage, booking.getBookingId(), "BOOKING_PAID");
        }
    }
    
    /**
     * Gửi thông báo khi booking bị tự động reject do Owner không phản hồi
     */
    public void notifyBookingAutoRejected(com.ecodana.evodanavn1.model.Booking booking) {
        String customerId = booking.getUser().getId();
        String message = String.format(
            "Yêu cầu đặt xe #%s đã bị hủy do chủ xe không phản hồi trong thời gian quy định.",
            booking.getBookingCode()
        );
        createNotification(customerId, message, booking.getBookingId(), "BOOKING_AUTO_REJECTED");
    }

    /**
     * Gửi thông báo cho khách hàng khi rental bắt đầu (Owner giao xe)
     */
    public void notifyCustomerRentalStarted(com.ecodana.evodanavn1.model.Booking booking) {
        String customerId = booking.getUser().getId();
        String message = String.format(
                "Chuyến đi #%s của bạn đã bắt đầu. Chúc bạn lái xe an toàn!",
                booking.getBookingCode()
        );
        createNotification(customerId, message, booking.getBookingId(), "RENTAL_STARTED");
    }

    /**
     * Gửi thông báo cho admin khi khách hàng yêu cầu hủy chuyến và hoàn tiền
     */
    public void notifyAdminRefundRequest(com.ecodana.evodanavn1.model.Booking booking, BigDecimal refundAmount, String refundMessage) {
        // Lấy ngân hàng mặc định của khách hàng
        String bankInfo = "";
        try {
            var defaultBank = bankAccountService.getDefaultBankAccount(booking.getUser().getId());
            if (defaultBank.isPresent()) {
                var bank = defaultBank.get();
                bankInfo = String.format(
                    "\n\n📱 THÔNG TIN NGÂN HÀNG KHÁCH HÀNG:\n" +
                    "Ngân hàng: %s\n" +
                    "Số tài khoản: %s\n" +
                    "Chủ tài khoản: %s",
                    bank.getBankName(),
                    bank.getAccountNumber(),
                    bank.getAccountHolderName()
                );
            }
        } catch (Exception e) {
            bankInfo = "\n\n⚠️ Không tìm thấy thông tin ngân hàng của khách hàng";
        }

        String message = String.format(
            "🔔 YÊU CẦU HOÀN TIỀN - Đơn hàng #%s\n" +
            "Khách hàng: %s (%s)\n" +
            "Xe: %s\n" +
            "Số tiền hoàn dự kiến: %s ₫\n" +
            "Chi tiết: %s%s\n\n" +
            "✅ Vui lòng xem xét và duyệt hoàn tiền.",
            booking.getBookingCode(),
            booking.getUser().getFirstName() + " " + booking.getUser().getLastName(),
            booking.getUser().getEmail(),
            booking.getVehicle().getLicensePlate(),
            refundAmount.setScale(0, java.math.RoundingMode.HALF_UP),
            refundMessage,
            bankInfo
        );
        createNotificationForAllAdmins(message, booking.getBookingId(), "REFUND_REQUEST");
    }

    /**
     * Gửi thông báo cho Owner khi Customer hủy booking (chưa thanh toán)
     */
    public void notifyOwnerBookingCancelled(com.ecodana.evodanavn1.model.Booking booking, String reason) {
        String ownerId = booking.getVehicle().getOwnerId();
        if (ownerId != null) {
            String message = String.format(
                "Khách hàng đã hủy đơn đặt xe #%s. Lý do: %s",
                booking.getBookingCode(),
                reason != null ? reason : "Không có lý do cụ thể"
            );
            createNotification(ownerId, message, booking.getBookingId(), "BOOKING_CANCELLED_BY_CUSTOMER");
        }
    }
}
