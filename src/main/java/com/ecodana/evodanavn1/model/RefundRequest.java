package com.ecodana.evodanavn1.model;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "RefundRequest")
public class RefundRequest {
    @Id
    @Column(name = "RefundRequestId", length = 36)
    private String refundRequestId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "BookingId", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "UserId", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "BankAccountId", nullable = true)
    private BankAccount bankAccount;

    @Column(name = "RefundAmount", precision = 10, scale = 2, nullable = false)
    private BigDecimal refundAmount;

    @Column(name = "CancelReason", length = 1000, nullable = false)
    private String cancelReason;

    @Column(name = "Status", nullable = false)
    @Enumerated(EnumType.STRING)
    private RefundStatus status = RefundStatus.Pending;

    @Column(name = "AdminNotes", length = 1000)
    private String adminNotes;

    @Column(name = "ProcessedBy", length = 36)
    private String processedBy; // Admin user ID

    @Column(name = "CreatedDate", nullable = false)
    private LocalDateTime createdDate;

    @Column(name = "ProcessedDate")
    private LocalDateTime processedDate;

    @Column(name = "IsWithinTwoHours", nullable = false)
    private boolean isWithinTwoHours = false;

    @Column(name = "TransferProofImagePath", length = 500)
    private String transferProofImagePath; // Ảnh chứng minh chuyển khoản từ Cloudinary

    // Constructors
    public RefundRequest() {
        this.createdDate = LocalDateTime.now();
    }

    // Getters and Setters
    public String getRefundRequestId() {
        return refundRequestId;
    }

    public void setRefundRequestId(String refundRequestId) {
        this.refundRequestId = refundRequestId;
    }

    public Booking getBooking() {
        return booking;
    }

    public void setBooking(Booking booking) {
        this.booking = booking;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public BankAccount getBankAccount() {
        return bankAccount;
    }

    public void setBankAccount(BankAccount bankAccount) {
        this.bankAccount = bankAccount;
    }

    public BigDecimal getRefundAmount() {
        return refundAmount;
    }

    public void setRefundAmount(BigDecimal refundAmount) {
        this.refundAmount = refundAmount;
    }

    public String getCancelReason() {
        return cancelReason;
    }

    public void setCancelReason(String cancelReason) {
        this.cancelReason = cancelReason;
    }

    public RefundStatus getStatus() {
        return status;
    }

    public void setStatus(RefundStatus status) {
        this.status = status;
    }

    public String getAdminNotes() {
        return adminNotes;
    }

    public void setAdminNotes(String adminNotes) {
        this.adminNotes = adminNotes;
    }

    public String getProcessedBy() {
        return processedBy;
    }

    public void setProcessedBy(String processedBy) {
        this.processedBy = processedBy;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public LocalDateTime getProcessedDate() {
        return processedDate;
    }

    public void setProcessedDate(LocalDateTime processedDate) {
        this.processedDate = processedDate;
    }

    public boolean isWithinTwoHours() {
        return isWithinTwoHours;
    }

    public void setWithinTwoHours(boolean withinTwoHours) {
        isWithinTwoHours = withinTwoHours;
    }

    public String getTransferProofImagePath() {
        return transferProofImagePath;
    }

    public void setTransferProofImagePath(String transferProofImagePath) {
        this.transferProofImagePath = transferProofImagePath;
    }

    public enum RefundStatus {
        Pending,    // Chờ admin duyệt
        Rejected,   // Admin từ chối
        Refunded    // Đã hoàn tiền (status cuối cùng)
    }
}
