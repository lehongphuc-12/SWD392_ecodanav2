package com.ecodana.evodanavn1.repository;

import com.ecodana.evodanavn1.model.InappropriateWord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InappropriateWordRepository extends JpaRepository<InappropriateWord, String> {
    List<InappropriateWord> findByIsActiveTrueOrderByWordAsc();
    Optional<InappropriateWord> findByWordIgnoreCase(String word);
    boolean existsByWordIgnoreCase(String word);
}


