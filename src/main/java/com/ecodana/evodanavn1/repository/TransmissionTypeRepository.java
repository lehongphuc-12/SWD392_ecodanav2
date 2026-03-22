package com.ecodana.evodanavn1.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ecodana.evodanavn1.model.TransmissionType;

@Repository
public interface TransmissionTypeRepository extends JpaRepository<TransmissionType, Integer> {
}