package com.ecodana.evodanavn1.scheduler;

import com.ecodana.evodanavn1.service.BookingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Lớp này chứa các tác vụ được lên lịch liên quan đến Booking.
 */
@Component
public class BookingScheduler {

    private static final Logger logger = LoggerFactory.getLogger(BookingScheduler.class);

    // Thời gian trễ cho phép trước khi chuyển sang NoShow (5 phút)
    private static final int NO_SHOW_GRACE_PERIOD_MINUTES = 5;

    @Autowired
    private BookingService bookingService;

    /**
     * Tác vụ này chạy mỗi phút (60,000 mili giây) để kiểm tra và cập nhật các đơn hàng
     * mà khách không đến nhận xe.
     *
     * Logic:
     * - Quét các đơn hàng có trạng thái 'Confirmed'.
     * - Nếu thời gian hiện tại > (thời gian nhận xe + 5 phút), chuyển trạng thái sang 'NoShow'.
     */
    @Scheduled(fixedRate = 60000) // Chạy mỗi 60 giây
    public void checkForNoShowBookings() {
        logger.debug("Bắt đầu tác vụ quét đơn hàng trễ hẹn (No-Show)...");
        bookingService.processNoShowBookings(NO_SHOW_GRACE_PERIOD_MINUTES);
        logger.debug("Hoàn thành tác vụ quét đơn hàng trễ hẹn.");
    }
}