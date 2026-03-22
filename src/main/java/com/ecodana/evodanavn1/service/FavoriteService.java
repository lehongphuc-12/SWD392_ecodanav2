package com.ecodana.evodanavn1.service;

import com.ecodana.evodanavn1.model.UserFavoriteVehicles;
import com.ecodana.evodanavn1.model.User;
import com.ecodana.evodanavn1.model.Vehicle;
import com.ecodana.evodanavn1.repository.UserFavoriteVehiclesRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class FavoriteService {
    @Autowired
    private UserFavoriteVehiclesRepository userFavoriteVehiclesRepository;

    @Autowired
    private VehicleService vehicleService;

    public boolean isFavorite(User user, Vehicle vehicle) {
        if (user == null || vehicle == null) return false;
        return userFavoriteVehiclesRepository.existsByUserAndVehicle(user, vehicle);
    }

    public boolean toggleFavorite(User user, String vehicleId) {
        if (user == null) throw new IllegalArgumentException("User is required");
        Vehicle vehicle = vehicleService.getVehicleById(vehicleId).orElse(null);
        if (vehicle == null) throw new IllegalArgumentException("Vehicle not found");

        Optional<UserFavoriteVehicles> existed = userFavoriteVehiclesRepository.findByUserAndVehicle(user, vehicle);
        if (existed.isPresent()) {
            userFavoriteVehiclesRepository.delete(existed.get());
            return false; // now unfavorited
        } else {
            UserFavoriteVehicles fav = new UserFavoriteVehicles();
            fav.setUser(user);
            fav.setVehicle(vehicle);
            userFavoriteVehiclesRepository.save(fav);
            return true; // now favorited
        }
    }

    public List<UserFavoriteVehicles> getFavorites(User user) {
        if (user == null) throw new IllegalArgumentException("User is required");
        return userFavoriteVehiclesRepository.findByUser(user);
    }
}
