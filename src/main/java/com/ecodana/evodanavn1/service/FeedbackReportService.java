package com.ecodana.evodanavn1.service;

import com.ecodana.evodanavn1.model.FeedbackReport;
import com.ecodana.evodanavn1.model.User;
import com.ecodana.evodanavn1.model.UserFeedback;
import com.ecodana.evodanavn1.repository.FeedbackReportRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class FeedbackReportService {

    @Autowired
    private FeedbackReportRepository feedbackReportRepository;

    public FeedbackReport createReport(User reporter, UserFeedback feedback, String reason) {
        FeedbackReport report = new FeedbackReport();
        report.setReportId(UUID.randomUUID().toString());
        report.setReporter(reporter);
        report.setFeedback(feedback);
        report.setReason(reason);
        report.setCreatedDate(LocalDateTime.now());
        report.setStatus(FeedbackReport.Status.Pending);
        return feedbackReportRepository.save(report);
    }

    public List<FeedbackReport> getAllReports() {
        return feedbackReportRepository.findAllByOrderByCreatedDateDesc();
    }

    public FeedbackReport resolveReport(String reportId) {
        return feedbackReportRepository.findById(reportId).map(r -> {
            r.setStatus(FeedbackReport.Status.Resolved);
            return feedbackReportRepository.save(r);
        }).orElse(null);
    }
}


