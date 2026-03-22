package com.ecodana.evodanavn1.repository;

import com.ecodana.evodanavn1.model.RefundRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RefundRequestRepository extends JpaRepository<RefundRequest, String> {
    
    List<RefundRequest> findByUserIdOrderByCreatedDateDesc(String userId);
    
    List<RefundRequest> findByStatusOrderByCreatedDateDesc(RefundRequest.RefundStatus status);
    
    Optional<RefundRequest> findByBookingBookingId(String bookingId);
    
    @Query("SELECT DISTINCT rr FROM RefundRequest rr LEFT JOIN FETCH rr.bankAccount LEFT JOIN FETCH rr.booking LEFT JOIN FETCH rr.user ORDER BY rr.createdDate DESC")
    List<RefundRequest> findAllWithRelations();
    
    @Query("SELECT rr FROM RefundRequest rr WHERE rr.status = com.ecodana.evodanavn1.model.RefundRequest$RefundStatus.Pending ORDER BY rr.createdDate DESC")
    List<RefundRequest> findPendingRequestsOrderByCreatedDate();
    
    @Query("SELECT COUNT(rr) FROM RefundRequest rr WHERE rr.status = com.ecodana.evodanavn1.model.RefundRequest$RefundStatus.Pending")
    long countPendingRequests();
    
    @Query("SELECT rr FROM RefundRequest rr WHERE rr.isWithinTwoHours = true AND rr.status = com.ecodana.evodanavn1.model.RefundRequest$RefundStatus.Pending ORDER BY rr.createdDate DESC")
    List<RefundRequest> findUrgentPendingRequests();
}
