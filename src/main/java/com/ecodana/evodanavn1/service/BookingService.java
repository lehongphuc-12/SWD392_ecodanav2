package com.ecodana.evodanavn1.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.time.LocalTime;
import com.ecodana.evodanavn1.model.User;
import com.ecodana.evodanavn1.model.Vehicle;
import com.ecodana.evodanavn1.model.VehicleConditionLogs;
import com.ecodana.evodanavn1.model.Payment;
import com.ecodana.evodanavn1.repository.PaymentRepository;
import com.ecodana.evodanavn1.repository.VehicleConditionLogsRepository;
import com.ecodana.evodanavn1.repository.VehicleRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import com.ecodana.evodanavn1.model.BookingApproval;
import com.ecodana.evodanavn1.repository.BookingApprovalRepository;
import com.ecodana.evodanavn1.model.Booking;
import com.ecodana.evodanavn1.repository.BookingRepository;
import com.ecodana.evodanavn1.model.RefundRequest;

@Service
public class BookingService {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private VehicleService vehicleService;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private VehicleConditionLogsRepository vehicleConditionLogsRepository;

    @Autowired
    private BookingApprovalRepository bookingApprovalRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private BankAccountService bankAccountService;

    @Autowired
    private RefundRequestService refundRequestService;

    private static final Logger logger = LoggerFactory.getLogger(BookingService.class);

    @Value("${booking.owner-approval-timeout-minutes}")
    private int ownerApprovalTimeoutMinutes;

    @Value("${booking.customer-payment-timeout-minutes}")
    private int customerPaymentTimeoutMinutes;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }

    public List<Booking> getBookingsByUser(User user) {
        return bookingRepository.findByUserId(user.getId());
    }
    public List<Booking> getBookingsByUserId(String userId) {
        return bookingRepository.findByUserId(userId);
    }

    public void addBooking(Booking booking) {
        if (booking.getBookingId() == null) {
            booking.setBookingId(UUID.randomUUID().toString());
        }
        if (booking.getBookingCode() == null) {
            booking.setBookingCode("BK" + System.currentTimeMillis());
        }
        bookingRepository.save(booking);
    }

    public List<Booking> getActiveBookingsByUser(User user) {
        return bookingRepository.findActiveBookingsByUserId(user.getId());
    }

    public List<Booking> getActiveBookings() {
        return bookingRepository.findAllActiveBookings();
    }

    public List<Booking> getPendingBookings() {
        return bookingRepository.findAllPendingBookings();
    }

    public BigDecimal getTodayRevenue() {
        List<Booking> completedBookings = bookingRepository.findByStatus(Booking.BookingStatus.Completed);
        return completedBookings.stream()
                .map(Booking::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getTotalRevenue() {
        List<Booking> revenueBookings = bookingRepository.findAll();
        return revenueBookings.stream()
                .filter(b -> b.getStatus() == Booking.BookingStatus.Approved || b.getStatus() == Booking.BookingStatus.Completed)
                .map(Booking::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public List<Object> getReviewsByUser(User user) {
        return List.of();
    }

    @Transactional
    public Optional<Booking> findById(String bookingId) {
        Optional<Booking> bookingOpt = bookingRepository.findByIdWithPayments(bookingId);
        bookingOpt.ifPresent(this::calculateAndSetRemainingAmount);
        return bookingOpt;
    }

    private void calculateAndSetRemainingAmount(Booking booking) {
        if (booking == null || booking.getTotalAmount() == null) {
            if (booking != null) {
                booking.setRemainingAmount(BigDecimal.ZERO);
            }
            return;
        }

        BigDecimal paidAmount = booking.getPayments().stream()
                .filter(p -> p.getPaymentStatus() == Payment.PaymentStatus.Completed)
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal remainingAmount = booking.getTotalAmount().subtract(paidAmount);
        booking.setRemainingAmount(remainingAmount);
    }

    public Booking getBookingById(String bookingId) {
        return bookingRepository.findById(bookingId).orElse(null);
    }

    public Booking updateBooking(Booking booking) {
        return bookingRepository.save(booking);
    }

    public Booking updateBookingDetails(String bookingId, Map<String, String> data) {
        return bookingRepository.findById(bookingId)
                .map(booking -> {
                    if (data.containsKey("pickupDateTime")) {
                        booking.setPickupDateTime(LocalDateTime.parse(data.get("pickupDateTime")));
                    }
                    if (data.containsKey("returnDateTime")) {
                        booking.setReturnDateTime(LocalDateTime.parse(data.get("returnDateTime")));
                    }
                    if (data.containsKey("totalAmount")) {
                        booking.setTotalAmount(new BigDecimal(data.get("totalAmount")));
                    }
                    if (data.containsKey("status")) {
                        booking.setStatus(Booking.BookingStatus.valueOf(data.get("status")));
                    }
                    return bookingRepository.save(booking);
                })
                .orElse(null);
    }

    public void deleteBooking(String bookingId) {
        bookingRepository.deleteById(bookingId);
    }

    public Map<String, Object> getRevenueAnalytics() {
        Map<String, Object> analytics = new HashMap<>();
        analytics.put("todayRevenue", getTodayRevenue());
        analytics.put("monthRevenue", getThisMonthRevenue());
        analytics.put("totalRevenue", getTotalRevenue());
        analytics.put("revenueGrowth", 15.5); // Mock data
        return analytics;
    }

    public BigDecimal getThisMonthRevenue() {
        LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        LocalDateTime endOfMonth = LocalDateTime.now().withDayOfMonth(LocalDate.now().lengthOfMonth()).withHour(23).withMinute(59).withSecond(59);
        List<Booking> monthBookings = bookingRepository.findByStatusAndDateRange(Booking.BookingStatus.Completed, startOfMonth, endOfMonth);
        return monthBookings.stream()
                .map(Booking::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Map<String, Object> getBookingStatistics() {
        Map<String, Object> stats = new HashMap<>();
        List<Booking> allBookings = getAllBookings();
        stats.put("totalBookings", allBookings.size());
        stats.put("pendingBookings", getPendingBookings().size());
        stats.put("activeBookings", getActiveBookings().size());
        stats.put("cancelledBookings", allBookings.stream().filter(b -> b.getStatus() == Booking.BookingStatus.Cancelled).count());
        return stats;
    }

    public List<Booking> getRecentBookings(int limit) {
        return bookingRepository.findRecentBookings().stream().limit(limit).collect(Collectors.toList());
    }

    public List<Booking> getBookingsByStatus(Booking.BookingStatus status) {
        return bookingRepository.findByStatus(status);
    }

    public Booking updateBookingStatus(String bookingId, String status) {
        return bookingRepository.findById(bookingId)
                .map(booking -> {
                    booking.setStatus(Booking.BookingStatus.valueOf(status));
                    return bookingRepository.save(booking);
                })
                .orElse(null);
    }

    public Map<String, Object> getBookingAnalytics() {
        Map<String, Object> analytics = new HashMap<>();
        try {
            LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
            analytics.put("dailyBookings", bookingRepository.findDailyBookings(sevenDaysAgo));
        } catch (Exception e) {
            analytics.put("dailyBookings", List.of());
        }
        try {
            LocalDateTime sixMonthsAgo = LocalDateTime.now().minusMonths(6);
            analytics.put("monthlyRevenue", bookingRepository.findMonthlyRevenue(sixMonthsAgo));
        } catch (Exception e) {
            analytics.put("monthlyRevenue", List.of());
        }
        try {
            analytics.put("vehiclePopularity", bookingRepository.findVehiclePopularity());
        } catch (Exception e) {
            analytics.put("vehiclePopularity", List.of());
        }
        return analytics;
    }

    // Owner Management Methods
    public List<Booking> getBookingsByOwnerId(String ownerId) {
        return bookingRepository.findByVehicleOwnerId(ownerId);
    }

    public Booking approveBooking(String bookingId, User approver) {
        return bookingRepository.findById(bookingId)
                .map(booking -> {
                    booking.setStatus(Booking.BookingStatus.AwaitingDeposit);
                    booking.setHandledBy(approver);

                    // Determine deposit amount based on payment option
                    String paymentOption = booking.getPaymentOption();
                    if ("FULL".equals(paymentOption)) {
                        booking.setDepositAmountRequired(booking.getTotalAmount());
                    } else { // Default to DEPOSIT (20%)
                        BigDecimal deposit = booking.getTotalAmount().multiply(new BigDecimal("0.2")).setScale(2, RoundingMode.HALF_UP);
                        booking.setDepositAmountRequired(deposit);
                    }

                    BookingApproval approval = new BookingApproval();
                    approval.setApprovalId(UUID.randomUUID().toString());
                    approval.setBooking(booking);
                    approval.setStaff(approver);
                    approval.setApprovalStatus("Approved");
                    approval.setApprovalDate(LocalDateTime.now());
                    approval.setNote("Owner approved.");
                    bookingApprovalRepository.save(approval);

                    return bookingRepository.save(booking);
                })
                .orElse(null);
    }

    public Booking rejectBooking(String bookingId, String reason, User rejector) {
        return bookingRepository.findById(bookingId)
                .map(booking -> {
                    booking.setStatus(Booking.BookingStatus.Rejected);
                    booking.setCancelReason(reason);
                    booking.setHandledBy(rejector);
                    Booking updatedBooking = bookingRepository.save(booking);

                    // Update vehicle status back to Available if no other active bookings exist
                    updateVehicleStatusOnBookingCompletionOrCancellation(booking.getVehicle());

                    return updatedBooking;
                })
                .orElse(null);
    }

    /**
     * SỬA ĐỔI: Phương thức completeBooking mới, xử lý đầy đủ nghiệp vụ
     * @param bookingId ID của booking
     * @param completer Người dùng (owner) thực hiện
     * @param notes Ghi chú khi trả xe
     * @param imageUrls Danh sách ảnh khi trả xe
     * @param setMaintenance Cờ (true/false) yêu cầu bảo trì xe
     * @return Booking đã cập nhật
     * @throws Exception
     */
    @Transactional
    public Booking completeBooking(String bookingId, User completer, String notes, List<String> imageUrls, boolean setMaintenance) throws Exception {
        logger.info("Completing booking {}. Set Maintenance: {}", bookingId, setMaintenance);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Booking với ID: " + bookingId));

        // 1. Chỉ cho phép hoàn thành khi status là Ongoing
        if (booking.getStatus() != Booking.BookingStatus.Ongoing) {
            throw new IllegalStateException("Không thể hoàn thành. Trạng thái booking không phải là 'Ongoing'.");
        }

        Vehicle vehicle = booking.getVehicle();
        if (vehicle == null) {
            throw new RuntimeException("Không tìm thấy Vehicle cho booking này.");
        }

        // 2. Cập nhật trạng thái Booking
        booking.setStatus(Booking.BookingStatus.Completed);
        booking.setHandledBy(completer); // Ghi nhận người xử lý

        // 3. Tạo Log ghi nhận tình trạng xe lúc TRẢ (Return)
        VehicleConditionLogs log = new VehicleConditionLogs();
        log.setLogId(UUID.randomUUID().toString());
        log.setBooking(booking);
        log.setVehicle(vehicle);
        log.setStaff(completer); // Ghi nhận owner là người nhận xe
        log.setCheckType("Return"); // Đánh dấu đây là log lúc Trả xe
        log.setCheckTime(LocalDateTime.now());
        log.setNote(notes);
        // Lưu ý: Chúng ta chưa yêu cầu owner nhập Odometer khi trả xe, nên tạm thời không set
        // log.setOdometer(odometer);

        // Lưu ảnh dưới dạng JSON
        if (imageUrls != null && !imageUrls.isEmpty()) {
            log.setDamageImages(objectMapper.writeValueAsString(imageUrls));
        }

        // 4. Lưu log
        vehicleConditionLogsRepository.save(log);

        // 5. Cập nhật trạng thái Vehicle dựa trên lựa chọn
        if (setMaintenance) {
            logger.info("Setting vehicle {} to Maintenance.", vehicle.getLicensePlate());
            vehicle.setStatus(Vehicle.VehicleStatus.Maintenance);
            vehicleRepository.save(vehicle);
        } else {
            logger.info("Setting vehicle {} to Available (if no other bookings).", vehicle.getLicensePlate());
            // Sử dụng logic cũ để set Available nếu xe rảnh
            updateVehicleStatusOnBookingCompletionOrCancellation(vehicle);
        }

        // 6. Cập nhật trạng thái Payment
        List<Payment> payments = paymentRepository.findByBookingId(bookingId);
        for (Payment payment : payments) {
            if (payment.getPaymentStatus() != Payment.PaymentStatus.Refunded && payment.getPaymentStatus() != Payment.PaymentStatus.Completed) {
                // Mark any pending payments as completed upon booking completion
                payment.setPaymentStatus(Payment.PaymentStatus.Completed);
                payment.setPaymentDate(LocalDateTime.now());
            }
        }
        paymentRepository.saveAll(payments);

        // 7. Recalculate RemainingAmount and DepositAmountRequired based on all completed payments
        BigDecimal totalPaid = payments.stream()
                .filter(p -> p.getPaymentStatus() == Payment.PaymentStatus.Completed)
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal remainingAmount = booking.getTotalAmount().subtract(totalPaid);
        booking.setRemainingAmount(remainingAmount);

        // If the booking is fully paid (remaining amount is zero or less due to rounding/overpayment)
        if (remainingAmount.compareTo(BigDecimal.ZERO) <= 0) {
            // If 100% of the total amount has been paid, set DepositAmountRequired to totalAmount
            // and ensure RemainingAmount is zero.
            booking.setDepositAmountRequired(booking.getTotalAmount());
            booking.setRemainingAmount(BigDecimal.ZERO);
        } else {
            // If there's still a remaining amount after completion, it indicates an unhandled payment
            // or a partial payment scenario. In this case, DepositAmountRequired should retain its
            // original value (e.g., the initial 20% deposit), and RemainingAmount reflects the unpaid balance.
            logger.warn("Booking {} completed with remaining amount: {}. This indicates an unhandled payment or partial payment.", bookingId, remainingAmount);
            // DepositAmountRequired is not changed here, it keeps its value set during approval.
            // RemainingAmount is already set to totalAmount - totalPaid.
        }

        // 8. Lưu booking đã cập nhật
        return bookingRepository.save(booking);
    }

    public Booking cancelBooking(String bookingId, String reason) {
        return bookingRepository.findById(bookingId)
                .map(booking -> {
                    // Chỉ cho phép hủy nếu đang Pending hoặc AwaitingDeposit (chưa trả tiền)
                    if (booking.getStatus() == Booking.BookingStatus.Pending || booking.getStatus() == Booking.BookingStatus.AwaitingDeposit) {
                        booking.setStatus(Booking.BookingStatus.Cancelled);
                        booking.setCancelReason(reason);
                        Booking updatedBooking = bookingRepository.save(booking);
                        updateVehicleStatusOnBookingCompletionOrCancellation(booking.getVehicle());
                        return updatedBooking;
                    } else {
                        // Nếu đã Confirmed (đã trả tiền), phải dùng logic cancelCar/processCancellationAndRefund
                        throw new IllegalStateException("Không thể hủy đơn đã thanh toán. Vui lòng sử dụng chức năng Hủy Xe.");
                    }
                })
                .orElse(null);
    }

    /**
     * Updates the vehicle status to 'Available' if there are no other active
     * (Approved, Ongoing) bookings for it.
     * @param vehicle The vehicle to check and update.
     */
    private void updateVehicleStatusOnBookingCompletionOrCancellation(Vehicle vehicle) {
        if (vehicle == null) {
            return;
        }
        // Check if there are any other 'Approved' or 'Ongoing' bookings for this vehicle
        boolean hasOtherActiveBookings = bookingRepository.hasActiveBookings(vehicle.getVehicleId());

        if (!hasOtherActiveBookings) {
            vehicle.setStatus(Vehicle.VehicleStatus.Available);
            vehicleService.updateVehicle(vehicle);
        }
    }

    /**
     * Tác vụ định kỳ: Tự động từ chối các booking 'Pending' đã quá hạn.
     * Chạy mỗi 15 phút.
     */
    @Scheduled(fixedRate = 15000) // 15000ms = 15 giây
    @Transactional
    public void autoRejectExpiredBookings() {
        logger.info("Chạy tác vụ tự động hủy booking quá hạn (Test)...");

        // 1. Xác định mốc thời gian timeout (theo phút)
        LocalDateTime timeoutThreshold = LocalDateTime.now().minusMinutes(ownerApprovalTimeoutMinutes);

        // 2. Tìm tất cả booking 'Pending' đã quá hạn
        List<Booking> expiredBookings = bookingRepository.findPendingBookingsOlderThan(timeoutThreshold);

        if (expiredBookings.isEmpty()) {
            logger.info("Không có booking nào quá hạn (Timeout = {} phút).", ownerApprovalTimeoutMinutes);
            return;
        }

        logger.warn("Tìm thấy {} booking quá hạn. Bắt đầu xử lý...", expiredBookings.size());

        // 3. Xử lý từng booking
        for (Booking booking : expiredBookings) {
            try {
                // Lấy thông tin xe trước khi thay đổi
                Vehicle vehicle = booking.getVehicle();
                String customerId = booking.getUser().getId();
                String bookingCode = booking.getBookingCode();

                // 4. Cập nhật trạng thái Booking
                booking.setStatus(Booking.BookingStatus.Rejected);
                booking.setCancelReason("Chủ xe không phản hồi yêu cầu (tự động hủy).");
                bookingRepository.save(booking);

                // 5. Mở lại xe cho người khác đặt
                // (Logic này đã bao gồm việc kiểm tra các booking khác của xe)
                updateVehicleStatusOnBookingCompletionOrCancellation(vehicle);

                // 6. Gửi thông báo cho khách hàng
                // (Phương thức này đã tồn tại trong NotificationService)
                notificationService.notifyBookingAutoRejected(booking);

                logger.info("Đã tự động hủy booking: {} (Khách: {}, Xe: {})", bookingCode, customerId, vehicle.getLicensePlate());

            } catch (Exception e) {
                logger.error("Lỗi khi tự động hủy booking {}: {}", booking.getBookingId(), e.getMessage(), e);
            }
        }
        bookingRepository.saveAll(expiredBookings); // Save all changes at once
        logger.info("Hoàn tất tác vụ tự động hủy booking.");
    }

    /**
     * Tác vụ định kỳ: Tự động hủy các booking 'AwaitingDeposit' (chờ Khách) đã quá hạn.
     * Chạy mỗi 5 phút (300000ms).
     */
    @Scheduled(fixedRate = 15000) // 5 phút
    @Transactional
    public void autoCancelExpiredPayments() {
        logger.info("Chạy tác vụ tự động HỦY (AwaitingDeposit) booking quá hạn (Customer)...");

        // 1. Xác định mốc thời gian timeout
        LocalDateTime timeoutThreshold = LocalDateTime.now().minusMinutes(customerPaymentTimeoutMinutes);

        // 2. Tìm tất cả booking 'AwaitingDeposit' đã quá hạn
        List<Booking> expiredBookings = bookingApprovalRepository.findBookingsWithExpiredPaymentWindow(
                Booking.BookingStatus.AwaitingDeposit,
                timeoutThreshold
        );

        if (expiredBookings.isEmpty()) {
            logger.info("Không có booking (AwaitingDeposit) nào quá hạn (Timeout = {} phút).", customerPaymentTimeoutMinutes);
            return;
        }

        logger.warn("Tìm thấy {} booking (AwaitingDeposit) quá hạn thanh toán. Bắt đầu xử lý...", expiredBookings.size());

        // 3. Xử lý từng booking
        for (Booking booking : expiredBookings) {
            try {
                Vehicle vehicle = booking.getVehicle();

                // 4. Cập nhật trạng thái Booking
                booking.setStatus(Booking.BookingStatus.Cancelled);
                booking.setCancelReason("Khách hàng không thanh toán cọc (tự động hủy).");
                bookingRepository.save(booking);

                // 5. Mở lại xe cho người khác đặt
                updateVehicleStatusOnBookingCompletionOrCancellation(vehicle);

                // 6. Gửi thông báo cho khách hàng
                notificationService.createNotification(
                        booking.getUser().getId(),
                        String.format("Đơn hàng #%s đã bị hủy do quá hạn thanh toán cọc.", booking.getBookingCode()),
                        booking.getBookingId(),
                        "BOOKING_CANCELLED"
                );

                // (Tùy chọn) Gửi thông báo cho Owner
                if (vehicle.getOwnerId() != null) {
                    notificationService.createNotification(
                            vehicle.getOwnerId(),
                            String.format("Đơn hàng #%s đã bị hủy do khách không thanh toán.", booking.getBookingCode()),
                            booking.getBookingId(),
                            "BOOKING_CANCELLED"
                    );
                }

                logger.info("Đã tự động HỦY (Cancelled) booking: {}", booking.getBookingCode());

            } catch (Exception e) {
                logger.error("Lỗi khi tự động HỦY (Cancelled) booking {}: {}", booking.getBookingId(), e.getMessage(), e);
            }
        }
        bookingRepository.saveAll(expiredBookings); // Save all changes at once
        logger.info("Hoàn tất tác vụ tự động HỦY (Cancelled) booking.");
    }

    public Map<String, Long> getBookingCountsByStatus() {
        Map<String, Long> counts = new HashMap<>();
        List<Booking> allBookings = getAllBookings();

        counts.put("pending", allBookings.stream()
                .filter(b -> b.getStatus() == Booking.BookingStatus.Pending).count());
        counts.put("approved", allBookings.stream()
                .filter(b -> b.getStatus() == Booking.BookingStatus.Approved).count());
        counts.put("ongoing", allBookings.stream()
                .filter(b -> b.getStatus() == Booking.BookingStatus.Ongoing).count());
        counts.put("completed", allBookings.stream()
                .filter(b -> b.getStatus() == Booking.BookingStatus.Completed).count());
        counts.put("rejected", allBookings.stream()
                .filter(b -> b.getStatus() == Booking.BookingStatus.Rejected).count());
        counts.put("cancelled", allBookings.stream()
                .filter(b -> b.getStatus() == Booking.BookingStatus.Cancelled).count());

        return counts;
    }

    /**
     * Logic hủy phức tạp (cho trạng thái Confirmed - đã thanh toán)
     * Tính toán phí hủy và số tiền hoàn lại dựa trên chính sách.
     * @param bookingId ID đơn hàng
     * @param reason Lý do hủy
     * @param canceller Người thực hiện hủy (để kiểm tra quyền)
     * @param bankAccountId ID tài khoản ngân hàng để nhận hoàn tiền
     * @return Map chứa kết quả
     */
    @Transactional
    public Map<String, Object> processCancellationAndRefund(String bookingId, String reason, User canceller, String bankAccountId) {
        return processCancellationAndRefund(bookingId, reason, canceller, bankAccountId, null);
    }

    /**
     * Logic hủy phức tạp (cho trạng thái Confirmed - đã thanh toán)
     * Tính toán phí hủy và số tiền hoàn lại dựa trên chính sách.
     * @param bookingId ID đơn hàng
     * @param reason Lý do hủy
     * @param canceller Người thực hiện hủy (để kiểm tra quyền)
     * @param bankAccountId ID tài khoản ngân hàng để nhận hoàn tiền
     * @param customRefundAmount Số tiền hoàn tùy chỉnh (nếu null sẽ tính toán)
     * @return Map chứa kết quả
     */
    @Transactional
    public Map<String, Object> processCancellationAndRefund(String bookingId, String reason, User canceller, String bankAccountId, BigDecimal customRefundAmount) {
        Map<String, Object> result = new HashMap<>();

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy Booking."));

        // Kiểm tra quyền sở hữu
        if (!booking.getUser().getId().equals(canceller.getId())) {
            throw new IllegalStateException("Bạn không có quyền hủy đơn đặt xe này.");
        }

        // Chỉ xử lý các booking đã thanh toán hoặc đang diễn ra
        if (booking.getStatus() != Booking.BookingStatus.Confirmed &&
                booking.getStatus() != Booking.BookingStatus.Ongoing &&
                booking.getStatus() != Booking.BookingStatus.AwaitingDeposit) {

            // Nếu là Pending, dùng logic cancelBooking đơn giản
            if(booking.getStatus() == Booking.BookingStatus.Pending) {
                cancelBooking(bookingId, reason);
                result.put("success", true);
                result.put("message", "Đã hủy đơn (chưa thanh toán).");
                return result;
            }
            throw new IllegalStateException("Trạng thái đơn hàng không hợp lệ để hủy và hoàn tiền (" + booking.getStatus() + ").");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime paymentTime = booking.getPaymentConfirmedAt();
        LocalDateTime tripStartTime = booking.getPickupDateTime();

        // Kiểm tra nếu chuyến đi đã bắt đầu
        if (now.isAfter(tripStartTime)) {
            throw new IllegalStateException("Không thể hủy chuyến đi đã bắt đầu hoặc đã kết thúc.");
        }

        // Nếu AwaitingDeposit (chủ xe duyệt nhưng khách chưa trả tiền) -> Hủy đơn giản
        if (booking.getStatus() == Booking.BookingStatus.AwaitingDeposit) {
            booking.setStatus(Booking.BookingStatus.Cancelled);
            booking.setCancelReason(reason + " | Khách hủy trước khi thanh toán cọc.");
            bookingRepository.save(booking);
            updateVehicleStatusOnBookingCompletionOrCancellation(booking.getVehicle());
            result.put("success", true);
            result.put("message", "Đã hủy đơn (chưa thanh toán).");
            return result;
        }

        // --- Bắt đầu logic tính phí khi đã thanh toán (Status = Confirmed) ---
        if (paymentTime == null) {
            // Trường hợp hy hữu: status là Confirmed nhưng không có paymentTime
            throw new IllegalStateException("Không thể hủy vì không tìm thấy thời điểm thanh toán. Vui lòng liên hệ CSKH.");
        }

        // Tìm tất cả payment (Completed hoặc Pending - cả hai đều có thể hoàn tiền)
        List<Payment> payments = paymentRepository.findByBookingId(bookingId); // Dòng này đã được thêm vào
        List<Payment> refundablePayments = payments.stream()
                .filter(p -> p.getPaymentStatus() == Payment.PaymentStatus.Completed ||
                        p.getPaymentStatus() == Payment.PaymentStatus.Pending)
                .filter(p -> p.getPaymentType() == Payment.PaymentType.Deposit ||
                        p.getPaymentType() == Payment.PaymentType.FinalPayment)
                .toList();

        System.out.println("=== REFUND CALCULATION DEBUG ===");
        System.out.println("Booking ID: " + bookingId);
        System.out.println("Total payments found: " + payments.size());
        System.out.println("Refundable payments (Completed or Pending): " + refundablePayments.size());

        for (Payment payment : refundablePayments) {
            System.out.println("Payment: " + payment.getPaymentId() + ", Amount: " + payment.getAmount() +
                    ", Type: " + payment.getPaymentType() + ", Status: " + payment.getPaymentStatus());
        }

        if (refundablePayments.isEmpty()) {
            System.out.println("WARNING: No refundable payments found, but booking status is Confirmed");

            // Fallback: Change to RefundPending for admin review
            booking.setStatus(Booking.BookingStatus.RefundPending);
            booking.setCancelReason(reason + " | Không tìm thấy giao dịch thanh toán. Chờ admin xử lý.");
            bookingRepository.save(booking);
            updateVehicleStatusOnBookingCompletionOrCancellation(booking.getVehicle());

            // Tạo RefundRequest để admin xử lý (với refund amount = 0, admin sẽ xử lý thủ công)
            try {
                RefundRequest refundRequest = refundRequestService.createRefundRequest(booking, canceller, reason, bankAccountId, BigDecimal.ZERO);
                System.out.println("RefundRequest created (no payments): " + refundRequest.getRefundRequestId());
                result.put("refundRequestId", refundRequest.getRefundRequestId());
            } catch (Exception e) {
                System.out.println("Error creating RefundRequest: " + e.getMessage());
                e.printStackTrace();
            }

            // Notify admin
            notificationService.notifyAdminRefundRequest(booking, BigDecimal.ZERO, "Không tìm thấy giao dịch thanh toán hoàn tất. Vui lòng kiểm tra và xử lý thủ công.");

            result.put("success", true);
            result.put("message", "Đã gửi yêu cầu hủy xe đến admin để xử lý. Không tìm thấy giao dịch thanh toán để hoàn tiền.");
            return result;
        }

        long hoursSincePayment = Duration.between(paymentTime, now).toHours();
        System.out.println("Hours since payment: " + hoursSincePayment);

        // Tính hoàn tiền cho từng loại payment
        BigDecimal totalRefundAmount = BigDecimal.ZERO;
        StringBuilder refundMessageBuilder = new StringBuilder();

        for (Payment payment : refundablePayments) {
            BigDecimal paymentRefundAmount = BigDecimal.ZERO;
            String paymentRefundMessage = "";

            if (hoursSincePayment < 2) {
                // Hủy trong 2 giờ -> Hoàn 100% cho cả Deposit và FinalPayment
                paymentRefundAmount = payment.getAmount();
                paymentRefundMessage = payment.getPaymentType() + ": Hoàn 100% (" + payment.getAmount() + " ₫)";
            } else {
                // Hủy sau 2 giờ -> Hoàn 70% (trừ 30% phí hủy)
                BigDecimal penalty = payment.getAmount().multiply(new BigDecimal("0.30")).setScale(2, RoundingMode.HALF_UP);
                paymentRefundAmount = payment.getAmount().subtract(penalty);
                paymentRefundMessage = payment.getPaymentType() + ": Hoàn 70% (" + paymentRefundAmount + " ₫, phí hủy 30%: " + penalty + " ₫)";
            }

            totalRefundAmount = totalRefundAmount.add(paymentRefundAmount);

            if (refundMessageBuilder.length() > 0) {
                refundMessageBuilder.append("; ");
            }
            refundMessageBuilder.append(paymentRefundMessage);

            System.out.println("Payment type: " + payment.getPaymentType() + ", Refund: " + paymentRefundAmount);
        }

        String refundMessage = refundMessageBuilder.toString();
        if (hoursSincePayment < 2) {
            refundMessage = "Hủy trong 2 giờ sau khi thanh toán. " + refundMessage;
        } else {
            refundMessage = "Hủy sau 2 giờ. " + refundMessage;
        }

        System.out.println("Total refund amount: " + totalRefundAmount);
        System.out.println("Refund message: " + refundMessage);

        // Đảm bảo số tiền hoàn không bị âm
        if (totalRefundAmount.compareTo(BigDecimal.ZERO) < 0) {
            totalRefundAmount = BigDecimal.ZERO;
        }

        // Cập nhật trạng thái Booking thành RefundPending (chờ admin duyệt)
        booking.setStatus(Booking.BookingStatus.RefundPending);
        booking.setCancelReason(reason + " | " + refundMessage);
        bookingRepository.save(booking);

        // Mở lại xe
        updateVehicleStatusOnBookingCompletionOrCancellation(booking.getVehicle());

        // Tạo RefundRequest để admin xử lý (Payment sẽ được tạo khi admin duyệt)
        try {
            RefundRequest refundRequest = refundRequestService.createRefundRequest(booking, canceller, reason, bankAccountId, totalRefundAmount);
            System.out.println("RefundRequest created: " + refundRequest.getRefundRequestId());
            result.put("refundRequestId", refundRequest.getRefundRequestId());
        } catch (Exception e) {
            System.out.println("Error creating RefundRequest: " + e.getMessage());
            e.printStackTrace();
            // Vẫn tiếp tục, RefundRequest có thể được tạo sau từ sync endpoint
        }

        // Gửi thông báo cho admin về yêu cầu hoàn tiền
        notificationService.notifyAdminRefundRequest(booking, totalRefundAmount, refundMessage);

        result.put("success", true);
        result.put("message", "Đã gửi yêu cầu hủy xe đến admin. " + refundMessage + " Số tiền hoàn dự kiến: " + totalRefundAmount.setScale(0, RoundingMode.HALF_UP) + " ₫");
        return result;
    }

    @Deprecated
    public Booking cancelCar(String bookingId, String reason) {
        return bookingRepository.findById(bookingId)
                .map(booking -> {
                    // Set status back to Cancelled
                    booking.setStatus(Booking.BookingStatus.Cancelled);
                    booking.setCancelReason(reason);

                    // Update vehicle status back to Available when car is cancelled after payment
                    Booking updatedBooking = bookingRepository.save(booking);
                    updateVehicleStatusOnBookingCompletionOrCancellation(booking.getVehicle());

                    return updatedBooking;
                })
                .orElse(null);
    }


    /**
     * Xử lý nghiệp vụ Giao xe (Handover)
     * @param bookingId ID của booking
     * @param owner Người dùng (owner) thực hiện giao xe
     * @param imageUrls Danh sách URL ảnh đã tải lên Cloudinary
     * @param odometer Số Odometer lúc giao
     * @param notes Ghi chú lúc giao
     * @return Booking đã cập nhật
     * @throws Exception
     */
    @Transactional
    public Booking handoverVehicle(String bookingId, User owner, List<String> imageUrls, int odometer, String notes) throws Exception {

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Booking với ID: " + bookingId));

        // 1. Chỉ cho phép giao xe khi status là Confirmed
        if (booking.getStatus() != Booking.BookingStatus.Confirmed) {
            throw new IllegalStateException("Không thể giao xe. Trạng thái booking không phải là 'Confirmed'.");
        }

        Vehicle vehicle = booking.getVehicle();
        if (vehicle == null) {
            throw new RuntimeException("Không tìm thấy Vehicle cho booking này.");
        }

        // 2. Cập nhật trạng thái Booking và Vehicle
        booking.setStatus(Booking.BookingStatus.Ongoing);
        vehicle.setStatus(Vehicle.VehicleStatus.Rented);

        // 3. Tạo Log ghi nhận tình trạng xe lúc giao (Pickup)
        VehicleConditionLogs log = new VehicleConditionLogs();
        log.setLogId(UUID.randomUUID().toString());
        log.setBooking(booking);
        log.setVehicle(vehicle);
        log.setStaff(owner); // Ghi nhận owner là người giao xe
        log.setCheckType("Pickup"); // Đánh dấu đây là log lúc Giao xe
        log.setCheckTime(LocalDateTime.now());
        log.setOdometer(odometer);
        log.setNote(notes);

        // Lưu ảnh dưới dạng JSON
        if (imageUrls != null && !imageUrls.isEmpty()) {
            log.setDamageImages(objectMapper.writeValueAsString(imageUrls));
        }

        // 4. Lưu tất cả thay đổi
        vehicleConditionLogsRepository.save(log);
        vehicleRepository.save(vehicle);
        Booking updatedBooking = bookingRepository.save(booking);

        // 5. Gửi thông báo cho khách hàng
        notificationService.notifyCustomerRentalStarted(updatedBooking);

        return updatedBooking;
    }

    /**
     * Lấy phân tích doanh thu chi tiết cho một chủ xe cụ thể.
     * Chỉ tính các booking đã 'Completed' (Hoàn thành) hoặc 'Confirmed' (Đã thanh toán cọc).
     * @param ownerId ID của chủ xe
     * @return Map chứa revenueToday, revenueThisMonth, revenueThisYear, totalRevenueAllTime
     */
    /**
     * Lấy phân tích doanh thu chi tiết cho một chủ xe cụ thể.
     * Trả về cả Doanh thu Gốc (totalAmount) và Thực nhận (ownerPayout).
     * @param ownerId ID của chủ xe
     * @return Map chứa revenue... (tính theo TotalAmount) và totalPayoutAllTime (tính theo OwnerPayout)
     */
    public Map<String, Object> getOwnerRevenueAnalytics(String ownerId) {
        Map<String, Object> analytics = new HashMap<>();
        LocalDateTime now = LocalDateTime.now();

        LocalDateTime startOfToday = now.toLocalDate().atStartOfDay();
        LocalDateTime startOfMonth = now.withDayOfMonth(1).toLocalDate().atStartOfDay();
        LocalDateTime startOfYear = now.withDayOfYear(1).toLocalDate().atStartOfDay();

        List<Booking> ownerBookings = bookingRepository.findByVehicleOwnerId(ownerId);

        // 1. Lọc tất cả các booking phát sinh doanh thu/payout
        // (Bao gồm cả NoShow vì chủ xe vẫn hưởng 1 phần)
        List<Booking> revenueBookings = ownerBookings.stream()
                .filter(b -> b.getStatus() == Booking.BookingStatus.Completed ||
                        b.getStatus() == Booking.BookingStatus.Confirmed ||
                        b.getStatus() == Booking.BookingStatus.Ongoing ||
                        b.getStatus() == Booking.BookingStatus.NoShow)
                .collect(Collectors.toList());

        // 2. Tính toán DOANH THU GỐC (TotalAmount - theo ý của bạn)
        BigDecimal revenueToday = revenueBookings.stream()
                .filter(b -> b.getCreatedDate().isAfter(startOfToday))
                .map(Booking::getTotalAmount) // Dùng Tổng tiền gộp
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal revenueThisMonth = revenueBookings.stream()
                .filter(b -> b.getCreatedDate().isAfter(startOfMonth))
                .map(Booking::getTotalAmount) // Dùng Tổng tiền gộp
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal revenueThisYear = revenueBookings.stream()
                .filter(b -> b.getCreatedDate().isAfter(startOfYear))
                .map(Booking::getTotalAmount) // Dùng Tổng tiền gộp
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalRevenueAllTime = revenueBookings.stream()
                .map(Booking::getTotalAmount) // Dùng Tổng tiền gộp
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 3. Tính toán TIỀN THỰC NHẬN (OwnerPayout)
        BigDecimal totalPayoutAllTime = revenueBookings.stream()
                .map(Booking::getOwnerPayout) // Dùng Thực nhận
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 4. Trả về kết quả
        // Các giá trị này (Today, Month, Year) là DOANH THU GỐC (TotalAmount)
        analytics.put("revenueToday", revenueToday != null ? revenueToday : BigDecimal.ZERO);
        analytics.put("revenueThisMonth", revenueThisMonth != null ? revenueThisMonth : BigDecimal.ZERO);
        analytics.put("revenueThisYear", revenueThisYear != null ? revenueThisYear : BigDecimal.ZERO);

        // "totalRevenueAllTime" là Doanh Thu Gốc (TotalAmount)
        analytics.put("totalRevenueAllTime", totalRevenueAllTime != null ? totalRevenueAllTime : BigDecimal.ZERO);

        // "totalPayoutAllTime" là Tiền Thực Nhận (OwnerPayout)
        analytics.put("totalPayoutAllTime", totalPayoutAllTime != null ? totalPayoutAllTime : BigDecimal.ZERO);

        return analytics;
    }

    /**
     * Lấy dữ liệu biểu đồ doanh thu cho owner (Ngày, Tháng, Năm)
     * @param ownerId ID của chủ xe
     * @return Map chứa dữ liệu cho 3 biểu đồ
     */
    public Map<String, Object> getOwnerRevenueChartData(String ownerId) {
        Map<String, Object> chartData = new HashMap<>();

        // 1. Dữ liệu 7 ngày qua (Daily)
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7).with(LocalTime.MIN);
        List<Map<String, Object>> dailyResults = bookingRepository.findDailyRevenueForOwner(ownerId, sevenDaysAgo);
        chartData.put("dailyLabels", dailyResults.stream().map(r -> r.get("period").toString()).collect(Collectors.toList()));
        chartData.put("dailyData", dailyResults.stream().map(r -> (BigDecimal) r.get("revenue")).collect(Collectors.toList()));

        // 2. Dữ liệu 12 tháng qua (Monthly)
        LocalDateTime twelveMonthsAgo = LocalDateTime.now().minusMonths(12).withDayOfMonth(1).with(LocalTime.MIN);
        List<Map<String, Object>> monthlyResults = bookingRepository.findMonthlyRevenueForOwner(ownerId, twelveMonthsAgo);
        chartData.put("monthlyLabels", monthlyResults.stream().map(r -> r.get("period").toString()).collect(Collectors.toList()));
        chartData.put("monthlyData", monthlyResults.stream().map(r -> (BigDecimal) r.get("revenue")).collect(Collectors.toList()));

        // 3. Dữ liệu 5 năm qua (Yearly)
        LocalDateTime fiveYearsAgo = LocalDateTime.now().minusYears(5).withDayOfYear(1).with(LocalTime.MIN);
        List<Map<String, Object>> yearlyResults = bookingRepository.findYearlyRevenueForOwner(ownerId, fiveYearsAgo);
        chartData.put("yearlyLabels", yearlyResults.stream().map(r -> r.get("period").toString()).collect(Collectors.toList()));
        chartData.put("yearlyData", yearlyResults.stream().map(r -> (BigDecimal) r.get("revenue")).collect(Collectors.toList()));

        return chartData;
    }

    /**
     * Get all payments for a booking
     */
    public List<Payment> getPaymentsByBookingId(String bookingId) {
        return paymentRepository.findByBookingId(bookingId);
    }

    /**
     * Update payment status
     */
    public Payment updatePayment(Payment payment) {
        return paymentRepository.save(payment);
    }

    /**
     * Tự động xử lý và chuyển các đơn hàng trễ hẹn sang trạng thái NoShow.
     * Phương thức này được gọi bởi BookingScheduler.
     *
     * @param gracePeriodInMinutes Khoảng thời gian cho phép trễ (tính bằng phút).
     */
    @Transactional
    public void processNoShowBookings(int gracePeriodInMinutes) {
        // 1. Xác định mốc thời gian giới hạn
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(gracePeriodInMinutes);

        // 2. Tìm các đơn hàng 'Confirmed' có thời gian nhận xe đã qua mốc giới hạn
        List<Booking> lateBookings = bookingRepository.findByStatusAndPickupDateTimeBefore(Booking.BookingStatus.Confirmed, threshold);

        if (lateBookings.isEmpty()) {
            return; // Không có đơn hàng nào cần xử lý
        }

        logger.info("Tìm thấy {} đơn hàng trễ hẹn (No-Show). Bắt đầu xử lý...", lateBookings.size());

        for (Booking booking : lateBookings) {
            try {
                // 3. Cập nhật trạng thái đơn hàng
                booking.setStatus(Booking.BookingStatus.NoShow);
                booking.setCancelReason("Tự động hủy do khách không đến nhận xe sau " + gracePeriodInMinutes + " phút.");

                // 4. Mở lại xe cho người khác đặt
                updateVehicleStatusOnBookingCompletionOrCancellation(booking.getVehicle());

                // 5. Gửi thông báo (tùy chọn)
                notificationService.createNotification(booking.getUser().getId(), "Đơn hàng #" + booking.getBookingCode() + " đã bị hủy do bạn không đến nhận xe.", booking.getBookingId(), "BOOKING_NOSHOW");
                if (booking.getVehicle().getOwnerId() != null) {
                    notificationService.createNotification(booking.getVehicle().getOwnerId(), "Đơn hàng #" + booking.getBookingCode() + " đã được chuyển sang trạng thái 'Không đến'.", booking.getBookingId(), "BOOKING_NOSHOW");
                }

                logger.info("Đã chuyển đơn hàng {} sang trạng thái NoShow.", booking.getBookingCode());
            } catch (Exception e) {
                logger.error("Lỗi khi xử lý No-Show cho booking {}: {}", booking.getBookingId(), e.getMessage(), e);
            }
        }
        bookingRepository.saveAll(lateBookings);
        logger.info("Hoàn tất xử lý {} đơn hàng No-Show.", lateBookings.size());
    }
}