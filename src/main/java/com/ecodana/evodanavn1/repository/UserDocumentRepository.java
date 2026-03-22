package com.ecodana.evodanavn1.repository;

import com.ecodana.evodanavn1.model.UserDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserDocumentRepository extends JpaRepository<UserDocument, String> {

    @Query("SELECT d FROM UserDocument d WHERE d.user.id = :userId ORDER BY d.createdDate DESC")
    List<UserDocument> findByUserId(@Param("userId") String userId);

    @Query("SELECT d FROM UserDocument d WHERE d.user.id = :userId AND d.documentType = :documentType")
    Optional<UserDocument> findByUserIdAndDocumentType(@Param("userId") String userId, @Param("documentType") UserDocument.DocumentType documentType);


    @Query("SELECT d FROM UserDocument d WHERE d.user.id = :userId ORDER BY d.createdDate DESC")
    List<UserDocument> findByUserIdOrderByCreatedDateDesc(@Param("userId") String userId);
}