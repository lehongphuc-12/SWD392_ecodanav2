package com.ecodana.evodanavn1.model;

import java.time.LocalDateTime;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "PasswordResetTokens")
public class PasswordResetToken {

    @Id
    @Column(name = "Id", length = 36)
    private String id;

    @Column(name = "Token", nullable = false, unique = true)
    private String token;

    @Column(name = "ExpiryTime", nullable = false)
    private LocalDateTime expiryTime;

    @Column(name = "IsUsed", nullable = false)
    private boolean isUsed = false;

    @Column(name = "CreatedAt", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToOne(targetEntity = User.class, fetch = FetchType.EAGER, optional = false)
    @JoinColumn(nullable = false, name = "UserId", referencedColumnName = "UserId")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    // Constructors
    public PasswordResetToken() {
        this.id = UUID.randomUUID().toString(); // Generate ID here
        this.createdAt = LocalDateTime.now();
        this.isUsed = false;
    }

    public PasswordResetToken(String token, User user, LocalDateTime expiryTime) {
        this(); // Call default constructor to generate ID
        this.token = token;
        this.user = user;
        this.expiryTime = expiryTime;
    }


    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public LocalDateTime getExpiryTime() { return expiryTime; }
    public void setExpiryTime(LocalDateTime expiryTime) { this.expiryTime = expiryTime; }
    public boolean isUsed() { return isUsed; }
    public void setUsed(boolean isUsed) { this.isUsed = isUsed; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
}