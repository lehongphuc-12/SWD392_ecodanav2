package com.ecodana.evodanavn1.repository;

import com.ecodana.evodanavn1.model.Booking;
import com.ecodana.evodanavn1.model.BookingApproval;
import com.ecodana.evodanavn1.model.Booking.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookingApprovalRepository extends JpaRepository<BookingApproval, String> {

    /**
     * Tìm các Booking ID đang ở trạng thái AwaitingDeposit
     * và có lần duyệt (Approval) gần nhất trước mốc thời gian timeout.
     * * Logic:
     * 1. Chỉ tìm các booking có status 'AwaitingDeposit'.
     * 2. Nhóm theo BookingId.
     * 3. Tìm ngày duyệt (approvalDate) muộn nhất (MAX) cho mỗi booking.
     * 4. Chỉ giữ lại những booking có ngày duyệt muộn nhất ĐÓ < mốc thời gian timeout.
     */
    @Query("SELECT b FROM Booking b WHERE b.status = :status AND b.bookingId IN (" +
            "SELECT ba.booking.bookingId FROM BookingApproval ba " +
            "WHERE ba.approvalStatus = 'Approved' " +
            "GROUP BY ba.booking.bookingId " +
            "HAVING MAX(ba.approvalDate) < :threshold)")
    List<Booking> findBookingsWithExpiredPaymentWindow(
            @Param("status") BookingStatus status,
            @Param("threshold") LocalDateTime threshold
    );
}