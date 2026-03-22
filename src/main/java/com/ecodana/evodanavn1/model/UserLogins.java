package com.ecodana.evodanavn1.model;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "UserLogins")
@IdClass(UserLogins.UserLoginId.class)
public class UserLogins {

    @Id
    @Column(name = "LoginProvider", length = 128)
    private String loginProvider;

    @Id
    @Column(name = "ProviderKey", length = 128)
    private String providerKey;

    @Column(name = "ProviderDisplayName", columnDefinition = "TEXT")
    private String providerDisplayName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "UserId", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    // Getters and Setters
    public String getLoginProvider() { return loginProvider; }
    public void setLoginProvider(String loginProvider) { this.loginProvider = loginProvider; }
    public String getProviderKey() { return providerKey; }
    public void setProviderKey(String providerKey) { this.providerKey = providerKey; }
    public String getProviderDisplayName() { return providerDisplayName; }
    public void setProviderDisplayName(String providerDisplayName) { this.providerDisplayName = providerDisplayName; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    // Composite key class
    public static class UserLoginId implements Serializable {
        private String loginProvider;
        private String providerKey;

        public UserLoginId() {}

        public UserLoginId(String loginProvider, String providerKey) {
            this.loginProvider = loginProvider;
            this.providerKey = providerKey;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            UserLoginId that = (UserLoginId) o;
            return Objects.equals(loginProvider, that.loginProvider) && Objects.equals(providerKey, that.providerKey);
        }

        @Override
        public int hashCode() {
            return Objects.hash(loginProvider, providerKey);
        }
    }
}