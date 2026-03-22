package com.ecodana.evodanavn1.model;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@Entity
@Table(name = "BookingApproval")
public class BookingApproval {

    @Id
    @Column(name = "ApprovalId", length = 36)
    private String approvalId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "BookingId", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "StaffId", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User staff;

    @Column(name = "ApprovalStatus", nullable = false)
    private String approvalStatus; // ENUM('Approved', 'Rejected')

    @Column(name = "ApprovalDate", nullable = false)
    private LocalDateTime approvalDate;

    @Column(name = "Note", length = 500)
    private String note;

    @Column(name = "RejectionReason", length = 500)
    private String rejectionReason;

    public BookingApproval() {
        this.approvalDate = LocalDateTime.now();
    }

    // Getters and Setters
    public String getApprovalId() { return approvalId; }
    public void setApprovalId(String approvalId) { this.approvalId = approvalId; }
    public Booking getBooking() { return booking; }
    public void setBooking(Booking booking) { this.booking = booking; }
    public User getStaff() { return staff; }
    public void setStaff(User staff) { this.staff = staff; }
    public String getApprovalStatus() { return approvalStatus; }
    public void setApprovalStatus(String approvalStatus) { this.approvalStatus = approvalStatus; }
    public LocalDateTime getApprovalDate() { return approvalDate; }
    public void setApprovalDate(LocalDateTime approvalDate) { this.approvalDate = approvalDate; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
}