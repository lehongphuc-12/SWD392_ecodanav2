package com.ecodana.evodanavn1.repository;

import com.ecodana.evodanavn1.model.UserFeedback;
import com.ecodana.evodanavn1.model.Booking;
import com.ecodana.evodanavn1.model.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserFeedbackRepository extends JpaRepository<UserFeedback, String> {
    
    // Find feedback by booking
    List<UserFeedback> findByBooking(Booking booking);
    
    // Find feedback by vehicle
    List<UserFeedback> findByVehicle(Vehicle vehicle);
    
    // Find feedback by user
    List<UserFeedback> findByUserOrderByCreatedDateDesc(com.ecodana.evodanavn1.model.User user);
    
    // Find feedback for vehicles owned by a specific user
    @Query("SELECT f FROM UserFeedback f WHERE f.vehicle.ownerId = :ownerId ORDER BY f.createdDate DESC")
    List<UserFeedback> findByVehicleOwnerId(@Param("ownerId") String ownerId);
    
    // Find all feedback ordered by creation date
    List<UserFeedback> findAllByOrderByCreatedDateDesc();
    
    // Find feedback with replies
    @Query("SELECT f FROM UserFeedback f WHERE f.staffReply IS NOT NULL ORDER BY f.createdDate DESC")
    List<UserFeedback> findFeedbackWithReplies();
    
    // Find feedback without replies
    @Query("SELECT f FROM UserFeedback f WHERE f.staffReply IS NULL ORDER BY f.createdDate DESC")
    List<UserFeedback> findFeedbackWithoutReplies();
}
