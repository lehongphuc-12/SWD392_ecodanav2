package com.ecodana.evodanavn1.repository;

import com.ecodana.evodanavn1.model.Contract;
import com.ecodana.evodanavn1.model.Contract.ContractStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ContractRepository extends JpaRepository<Contract, String> {
    
    Optional<Contract> findByContractCode(String contractCode);
    
    Optional<Contract> findByBooking_BookingId(String bookingId);
    
    List<Contract> findByUser_Id(String userId);
    
    List<Contract> findByStatus(ContractStatus status);
    
    @Query("SELECT c FROM Contract c WHERE c.user.id = :userId AND c.status = :status")
    List<Contract> findByUserIdAndStatus(@Param("userId") String userId, @Param("status") ContractStatus status);
    
    @Query("SELECT c FROM Contract c WHERE c.createdDate BETWEEN :startDate AND :endDate")
    List<Contract> findByCreatedDateBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT c FROM Contract c WHERE c.signedDate BETWEEN :startDate AND :endDate")
    List<Contract> findBySignedDateBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT COUNT(c) FROM Contract c WHERE c.status = :status")
    long countByStatus(@Param("status") ContractStatus status);
    
    @Query("SELECT c FROM Contract c LEFT JOIN FETCH c.user LEFT JOIN FETCH c.booking b LEFT JOIN FETCH b.vehicle ORDER BY c.createdDate DESC")
    List<Contract> findAllWithDetails();
    
    @Query("SELECT c FROM Contract c LEFT JOIN FETCH c.user LEFT JOIN FETCH c.booking WHERE c.contractId = :contractId")
    Optional<Contract> findByIdWithDetails(@Param("contractId") String contractId);
    
    @Query("SELECT c FROM Contract c WHERE LOWER(c.contractCode) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(c.user.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(c.user.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(c.user.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<Contract> searchContracts(@Param("searchTerm") String searchTerm);
}
