package com.ecodana.evodanavn1.repository;

import com.ecodana.evodanavn1.model.BankAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BankAccountRepository extends JpaRepository<BankAccount, String> {
    
    @Query("SELECT ba FROM BankAccount ba WHERE ba.user.id = :userId ORDER BY ba.createdDate DESC")
    List<BankAccount> findByUserIdOrderByCreatedDateDesc(@Param("userId") String userId);
    
    @Query("SELECT ba FROM BankAccount ba WHERE ba.user.id = :userId AND ba.isDefault = true")
    Optional<BankAccount> findByUserIdAndIsDefaultTrue(@Param("userId") String userId);
    
    @Query("SELECT ba FROM BankAccount ba WHERE ba.user.id = :userId AND ba.isDefault = true")
    Optional<BankAccount> findDefaultBankAccountByUserId(@Param("userId") String userId);
    
    @Query("SELECT COUNT(ba) FROM BankAccount ba WHERE ba.user.id = :userId")
    long countByUserId(@Param("userId") String userId);
}
