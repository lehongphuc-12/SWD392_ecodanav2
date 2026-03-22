package com.ecodana.evodanavn1.model;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@Entity
@Table(name = "FeedbackReport")
public class FeedbackReport {

    @Id
    @Column(name = "ReportId", length = 36)
    private String reportId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "FeedbackId", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private UserFeedback feedback;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ReporterId", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User reporter;

    @Column(name = "Reason", length = 1000)
    private String reason;

    @Column(name = "CreatedDate", nullable = false)
    private LocalDateTime createdDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "Status", length = 20, nullable = false)
    private Status status = Status.Pending;

    public enum Status { Pending, Resolved }

    public FeedbackReport() {
        this.createdDate = LocalDateTime.now();
    }

    public String getReportId() { return reportId; }
    public void setReportId(String reportId) { this.reportId = reportId; }
    public UserFeedback getFeedback() { return feedback; }
    public void setFeedback(UserFeedback feedback) { this.feedback = feedback; }
    public User getReporter() { return reporter; }
    public void setReporter(User reporter) { this.reporter = reporter; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
}


