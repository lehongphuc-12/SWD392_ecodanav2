package com.ecodana.evodanavn1.repository;

import com.ecodana.evodanavn1.model.User;
import com.ecodana.evodanavn1.model.UserFavoriteVehicles;
import com.ecodana.evodanavn1.model.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserFavoriteVehiclesRepository extends JpaRepository<UserFavoriteVehicles, UserFavoriteVehicles.UserFavoriteVehicleId> {
    boolean existsByUserAndVehicle(User user, Vehicle vehicle);
    Optional<UserFavoriteVehicles> findByUserAndVehicle(User user, Vehicle vehicle);
    List<UserFavoriteVehicles> findByUser(User user);
    void deleteByUserAndVehicle(User user, Vehicle vehicle);
}

