package com.ecodana.evodanavn1.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ecodana.evodanavn1.model.Vehicle;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, String> {

    List<Vehicle> findByStatus(String status);

    List<Vehicle> findByVehicleType(Vehicle.VehicleType vehicleType);

    List<Vehicle> findByOwnerId(String ownerId);

    @Query("SELECT v FROM Vehicle v WHERE v.status = 'Available'")
    List<Vehicle> findAvailableVehicles();

    List<Vehicle> findByCategory_CategoryId(Integer categoryId);

    List<Vehicle> findByTransmissionType_TransmissionTypeId(Integer transmissionTypeId);

    Optional<Vehicle> findByLicensePlate(String licensePlate);

    boolean existsByLicensePlate(String licensePlate);

    @Query(value = "SELECT * FROM Vehicle v WHERE CAST(JSON_VALUE(v.RentalPrices, '$.daily') AS DECIMAL(10,2)) BETWEEN :minPrice AND :maxPrice", nativeQuery = true)
    List<Vehicle> findByPriceRange(@Param("minPrice") BigDecimal minPrice, @Param("maxPrice") BigDecimal maxPrice);

    List<Vehicle> findBySeats(Integer seats);

    List<Vehicle> findByRequiresLicense(Boolean requiresLicense);

    @Query(value = "SELECT v.* FROM Vehicle v " +
            "WHERE LOWER(v.VehicleModel) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(v.LicensePlate) LIKE LOWER(CONCAT('%', :keyword, '%'))", nativeQuery = true)
    List<Vehicle> searchVehicles(@Param("keyword") String keyword);

    @Query(value = "SELECT v.Status as status, COUNT(v.VehicleId) as count FROM Vehicle v GROUP BY v.Status", nativeQuery = true)
    List<Map<String, Object>> findStatusDistribution();

    @Query(value = "SELECT vc.CategoryName as category, COUNT(v.VehicleId) as count " +
            "FROM Vehicle v " +
            "JOIN VehicleCategories vc ON v.CategoryId = vc.CategoryId " +
            "GROUP BY vc.CategoryName", nativeQuery = true)
    List<Map<String, Object>> findCategoryDistribution();

}
