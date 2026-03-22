package com.ecodana.evodanavn1.model;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "UserFavoriteVehicles")
@IdClass(UserFavoriteVehicles.UserFavoriteVehicleId.class)
public class UserFavoriteVehicles {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "UserId", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "VehicleId", nullable = false)
    private Vehicle vehicle;

    // Getters and Setters
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Vehicle getVehicle() { return vehicle; }
    public void setVehicle(Vehicle vehicle) { this.vehicle = vehicle; }

    // Composite key class
    public static class UserFavoriteVehicleId implements Serializable {
        private String user;
        private String vehicle;

        public UserFavoriteVehicleId() {}

        public UserFavoriteVehicleId(String user, String vehicle) {
            this.user = user;
            this.vehicle = vehicle;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            UserFavoriteVehicleId that = (UserFavoriteVehicleId) o;
            return Objects.equals(user, that.user) && Objects.equals(vehicle, that.vehicle);
        }

        @Override
        public int hashCode() {
            return Objects.hash(user, vehicle);
        }
    }
}