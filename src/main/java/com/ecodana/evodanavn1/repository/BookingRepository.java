package com.ecodana.evodanavn1.repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ecodana.evodanavn1.model.Booking;

@Repository
public interface BookingRepository extends JpaRepository<Booking, String> {

    @Query("SELECT b FROM Booking b LEFT JOIN FETCH b.payments WHERE b.bookingId = :bookingId")
    Optional<Booking> findByIdWithPayments(@Param("bookingId") String bookingId);

    @Query("SELECT b FROM Booking b WHERE b.user.id = :userId")
    List<Booking> findByUserId(@Param("userId") String userId);

    @Query("SELECT b FROM Booking b WHERE b.vehicle.vehicleId = :vehicleId")
    List<Booking> findByVehicleId(@Param("vehicleId") String vehicleId);

    List<Booking> findByStatus(Booking.BookingStatus status);

    @Query("SELECT b FROM Booking b WHERE b.user.id = :userId AND b.status IN (com.ecodana.evodanavn1.model.Booking$BookingStatus.Approved, com.ecodana.evodanavn1.model.Booking$BookingStatus.Pending)")
    List<Booking> findActiveBookingsByUserId(@Param("userId") String userId);

    @Query("SELECT b FROM Booking b WHERE b.status = com.ecodana.evodanavn1.model.Booking$BookingStatus.Approved")
    List<Booking> findAllActiveBookings();

    @Query("SELECT COUNT(b) > 0 FROM Booking b WHERE b.vehicle.vehicleId = :vehicleId AND b.status IN (com.ecodana.evodanavn1.model.Booking$BookingStatus.Approved, com.ecodana.evodanavn1.model.Booking$BookingStatus.Ongoing)")
    boolean hasActiveBookings(@Param("vehicleId") String vehicleId);

    @Query("SELECT b FROM Booking b WHERE b.status = com.ecodana.evodanavn1.model.Booking$BookingStatus.Pending")
    List<Booking> findAllPendingBookings();

    /**
     * Tìm các booking đang ở trạng thái Pending và được tạo trước mốc thời gian threshold.
     * @param threshold Mốc thời gian (ví dụ: now - 2 giờ)
     * @return Danh sách các booking đã quá hạn
     */
    @Query("SELECT b FROM Booking b WHERE b.status = com.ecodana.evodanavn1.model.Booking$BookingStatus.Pending AND b.createdDate < :threshold")
    List<Booking> findPendingBookingsOlderThan(@Param("threshold") LocalDateTime threshold);

    @Query("SELECT b FROM Booking b LEFT JOIN FETCH b.payments WHERE b.vehicle.ownerId = :ownerId")
    List<Booking> findByVehicleOwnerId(@Param("ownerId") String ownerId);

    Optional<Booking> findByBookingCode(String bookingCode);

    boolean existsByBookingCode(String bookingCode);

    @Query("SELECT b FROM Booking b WHERE b.status = :status AND b.pickupDateTime >= :startDate AND b.returnDateTime <= :endDate")
    List<Booking> findByStatusAndDateRange(@Param("status") Booking.BookingStatus status,
                                           @Param("startDate") java.time.LocalDateTime startDate,
                                           @Param("endDate") java.time.LocalDateTime endDate);

    @Query("SELECT b FROM Booking b ORDER BY b.createdDate DESC")
    List<Booking> findRecentBookings();

    @Query(value = "SELECT CAST(b.CreatedDate AS DATE) as date, COUNT(b.BookingId) as count FROM Booking b " +
            "WHERE b.CreatedDate >= :startDate " +
            "GROUP BY CAST(b.CreatedDate AS DATE) ORDER BY date DESC", nativeQuery = true)
    List<Map<String, Object>> findDailyBookings(@Param("startDate") java.time.LocalDateTime startDate);

    @Query(value = "SELECT YEAR(b.CreatedDate) as year, MONTH(b.CreatedDate) as month, SUM(b.TotalAmount) as revenue " +
            "FROM Booking b WHERE b.Status IN ('Approved', 'Completed') " +
            "AND b.CreatedDate >= :startDate " +
            "GROUP BY YEAR(b.CreatedDate), MONTH(b.CreatedDate) ORDER BY year DESC, month DESC", nativeQuery = true)
    List<Map<String, Object>> findMonthlyRevenue(@Param("startDate") java.time.LocalDateTime startDate);

    @Query(value = "SELECT v.VehicleModel as model, COUNT(b.BookingId) as bookingCount " +
            "FROM Booking b " +
            "JOIN Vehicle v ON b.VehicleId = v.VehicleId " +
            "WHERE b.Status IN ('Approved', 'Completed') " +
            "GROUP BY v.VehicleModel ORDER BY bookingCount DESC", nativeQuery = true)
    List<Map<String, Object>> findVehiclePopularity();

    /**
     * Lấy tổng doanh thu mỗi ngày (trong 7 ngày qua) cho một owner
     */
    @Query(value = "SELECT FORMAT(b.CreatedDate, 'yyyy-MM-dd') as period, SUM(b.TotalAmount) as revenue " + // Đảm bảo là TotalAmount
            "FROM Booking b JOIN Vehicle v ON b.VehicleId = v.VehicleId " +
            "WHERE v.OwnerId = :ownerId AND b.Status IN ('Completed', 'Confirmed', 'Ongoing', 'NoShow') AND b.CreatedDate >= :startDate " + // Đảm bảo đã thêm NoShow
            "GROUP BY FORMAT(b.CreatedDate, 'yyyy-MM-dd') ORDER BY period ASC", nativeQuery = true)
    List<Map<String, Object>> findDailyRevenueForOwner(@Param("ownerId") String ownerId, @Param("startDate") LocalDateTime startDate);
    /**
     * Lấy tổng doanh thu mỗi tháng (trong 12 tháng qua) cho một owner
     */
    @Query(value = "SELECT FORMAT(b.CreatedDate, 'yyyy-MM') as period, SUM(b.TotalAmount) as revenue " + // Đã xác nhận dùng TotalAmount
            "FROM Booking b JOIN Vehicle v ON b.VehicleId = v.VehicleId " +
            "WHERE v.OwnerId = :ownerId AND b.Status IN ('Completed', 'Confirmed', 'Ongoing', 'NoShow') AND b.CreatedDate >= :startDate " + // Đã cập nhật Status
            "GROUP BY FORMAT(b.CreatedDate, 'yyyy-MM') ORDER BY period ASC", nativeQuery = true)
    List<Map<String, Object>> findMonthlyRevenueForOwner(@Param("ownerId") String ownerId, @Param("startDate") LocalDateTime startDate);
    /**
     * Lấy tổng doanh thu mỗi năm (trong 5 năm qua) cho một owner
     */
    @Query(value = "SELECT YEAR(b.CreatedDate) as period, SUM(b.TotalAmount) as revenue " + // Đã xác nhận dùng TotalAmount
            "FROM Booking b JOIN Vehicle v ON b.VehicleId = v.VehicleId " +
            "WHERE v.OwnerId = :ownerId AND b.Status IN ('Completed', 'Confirmed', 'Ongoing', 'NoShow') AND b.CreatedDate >= :startDate " + // Đã cập nhật Status
            "GROUP BY YEAR(b.CreatedDate) ORDER BY period ASC", nativeQuery = true)
    List<Map<String, Object>> findYearlyRevenueForOwner(@Param("ownerId") String ownerId, @Param("startDate") LocalDateTime startDate);
    /**
     * Tìm các booking theo trạng thái và có thời gian nhận xe trước một mốc thời gian cụ thể.
     * Phương thức này rất quan trọng cho tác vụ tự động xử lý các đơn hàng No-Show.
     * @param status Trạng thái booking cần tìm (ví dụ: Confirmed).
     * @param deadline Mốc thời gian giới hạn (ví dụ: now - 3 giờ).
     * @return Danh sách các booking thỏa mãn điều kiện.
     */
    List<Booking> findByStatusAndPickupDateTimeBefore(Booking.BookingStatus status, LocalDateTime deadline);
}