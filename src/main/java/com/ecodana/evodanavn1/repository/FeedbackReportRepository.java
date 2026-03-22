package com.ecodana.evodanavn1.repository;

import com.ecodana.evodanavn1.model.FeedbackReport;
import com.ecodana.evodanavn1.model.UserFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FeedbackReportRepository extends JpaRepository<FeedbackReport, String> {
    List<FeedbackReport> findByFeedback(UserFeedback feedback);
    List<FeedbackReport> findAllByOrderByCreatedDateDesc();
}


