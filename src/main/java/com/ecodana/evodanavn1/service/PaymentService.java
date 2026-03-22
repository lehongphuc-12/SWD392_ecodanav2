package com.ecodana.evodanavn1.service;

import com.ecodana.evodanavn1.model.Booking;
import com.ecodana.evodanavn1.model.Payment;
import com.ecodana.evodanavn1.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    /**
     * Lấy tất cả các khoản thanh toán liên quan đến các xe của một chủ sở hữu.
     * @param ownerId ID của chủ xe
     * @return Danh sách các khoản thanh toán
     */
    public List<Payment> getPaymentsForOwner(String ownerId) {
        return paymentRepository.findPaymentsByVehicleOwnerId(ownerId);
    }

    /**
     * Tính toán các chỉ số thống kê thanh toán cho chủ xe.
     * Total Revenue: Tổng tiền từ các booking đã 'Completed'.
     * Net Revenue: Total Revenue trừ đi các khoản đã 'Refunded'.
     * @param ownerId ID của chủ xe
     * @return Map chứa totalRevenue và netRevenue
     */
    public Map<String, BigDecimal> getOwnerPaymentStatistics(String ownerId) {
        List<Payment> ownerPayments = getPaymentsForOwner(ownerId);

        // Doanh thu tổng là tổng các khoản thanh toán 'COMPLETED' từ các booking đã 'COMPLETED'.
        BigDecimal totalRevenue = ownerPayments.stream()
                .filter(p -> p.getBooking() != null && p.getBooking().getStatus() == Booking.BookingStatus.Completed)
                .map(Payment::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Tiền hoàn lại là tổng các khoản thanh toán có trạng thái 'Refunded'.
        BigDecimal totalRefunds = ownerPayments.stream()
                .filter(p -> p.getPaymentStatus() == Payment.PaymentStatus.Refunded)
                .map(Payment::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Doanh thu thực nhận = Doanh thu tổng - Tiền hoàn lại.
        BigDecimal netRevenue = totalRevenue.subtract(totalRefunds);

        return Map.of("totalRevenue", totalRevenue, "netRevenue", netRevenue);
    }
}