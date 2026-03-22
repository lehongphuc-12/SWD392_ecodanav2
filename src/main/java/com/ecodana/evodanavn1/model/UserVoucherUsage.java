package com.ecodana.evodanavn1.model;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "UserVoucherUsage")
@IdClass(UserVoucherUsage.UserVoucherUsageId.class)
public class UserVoucherUsage {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "UserId", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "DiscountId", nullable = false)
    private Discount discount;

    @Column(name = "UsedAt", nullable = false)
    private LocalDateTime usedAt;

    public UserVoucherUsage() {
        this.usedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Discount getDiscount() { return discount; }
    public void setDiscount(Discount discount) { this.discount = discount; }
    public LocalDateTime getUsedAt() { return usedAt; }
    public void setUsedAt(LocalDateTime usedAt) { this.usedAt = usedAt; }

    // Composite key class
    public static class UserVoucherUsageId implements Serializable {
        private String user;
        private String discount;

        public UserVoucherUsageId() {}

        public UserVoucherUsageId(String user, String discount) {
            this.user = user;
            this.discount = discount;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            UserVoucherUsageId that = (UserVoucherUsageId) o;
            return Objects.equals(user, that.user) && Objects.equals(discount, that.discount);
        }

        @Override
        public int hashCode() {
            return Objects.hash(user, discount);
        }
    }
}