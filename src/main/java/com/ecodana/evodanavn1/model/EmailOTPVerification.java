package com.ecodana.evodanavn1.model;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@Entity
@Table(name = "EmailOTPVerification")
public class EmailOTPVerification {

    @Id
    @Column(name = "Id", length = 36)
    private String id;

    @Column(name = "OTP", nullable = false)
    private String otp;

    @Column(name = "ExpiryTime", nullable = false)
    private LocalDateTime expiryTime;

    @Column(name = "IsUsed", nullable = false)
    private boolean isUsed = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "UserId", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @Column(name = "CreatedAt", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "ResendCount", nullable = false)
    private int resendCount;

    @Column(name = "LastResendTime")
    private LocalDateTime lastResendTime;

    @Column(name = "ResendBlockUntil")
    private LocalDateTime resendBlockUntil;

    public EmailOTPVerification() {
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getOtp() { return otp; }
    public void setOtp(String otp) { this.otp = otp; }
    public LocalDateTime getExpiryTime() { return expiryTime; }
    public void setExpiryTime(LocalDateTime expiryTime) { this.expiryTime = expiryTime; }
    public boolean isUsed() { return isUsed; }
    public void setUsed(boolean used) { isUsed = used; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public int getResendCount() { return resendCount; }
    public void setResendCount(int resendCount) { this.resendCount = resendCount; }
    public LocalDateTime getLastResendTime() { return lastResendTime; }
    public void setLastResendTime(LocalDateTime lastResendTime) { this.lastResendTime = lastResendTime; }
    public LocalDateTime getResendBlockUntil() { return resendBlockUntil; }
    public void setResendBlockUntil(LocalDateTime resendBlockUntil) { this.resendBlockUntil = resendBlockUntil; }
}