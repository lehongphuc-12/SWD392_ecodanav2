package com.ecodana.evodanavn1.repository;

import com.ecodana.evodanavn1.model.VehicleCategories;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VehicleCategoryRepository extends JpaRepository<VehicleCategories, Integer> {
    Optional<VehicleCategories> findByCategoryName(String categoryName);
    boolean existsByCategoryName(String categoryName);
}
