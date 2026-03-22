package com.ecodana.evodanavn1.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "Notification")
public class Notification {
    @Id
    @Column(name = "NotificationId", length = 36)
    private String notificationId;
    
    @Column(name = "UserId", length = 36, nullable = false)
    private String userId;
    
    @Column(name = "Message", columnDefinition = "TEXT", nullable = false)
    private String message;
    
    @Column(name = "CreatedDate", nullable = false)
    private LocalDateTime createdDate;
    
    @Column(name = "IsRead", nullable = false)
    private Boolean isRead = false;
    
    @Column(name = "RelatedId", length = 36)
    private String relatedId; // ID của booking, payment, etc.
    
    @Column(name = "NotificationType", length = 50)
    private String notificationType; // "BOOKING", "PAYMENT", "CONTRACT", etc.
    
    // Constructors
    public Notification() {
        this.createdDate = LocalDateTime.now();
        this.isRead = false;
    }
    
    public Notification(String userId, String message) {
        this();
        this.userId = userId;
        this.message = message;
    }
    
    public Notification(String userId, String message, String relatedId, String notificationType) {
        this();
        this.userId = userId;
        this.message = message;
        this.relatedId = relatedId;
        this.notificationType = notificationType;
    }
    
    // Getters/Setters
    public String getNotificationId() { return notificationId; }
    public void setNotificationId(String notificationId) { this.notificationId = notificationId; }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }
    
    public Boolean getIsRead() { return isRead; }
    public void setIsRead(Boolean isRead) { this.isRead = isRead; }
    
    public String getRelatedId() { return relatedId; }
    public void setRelatedId(String relatedId) { this.relatedId = relatedId; }
    
    public String getNotificationType() { return notificationType; }
    public void setNotificationType(String notificationType) { this.notificationType = notificationType; }
}