package com.ecodana.evodanavn1.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "Terms")
public class Terms {
    @Id
    @Column(name = "TermsId", length = 36)
    private String termsId;
    
    @Column(name = "Version", length = 10, nullable = false, unique = true)
    private String version;
    
    @Column(name = "Title", length = 200, nullable = false)
    private String title;
    
    @Column(name = "ShortContent", columnDefinition = "TEXT")
    private String shortContent;
    
    @Column(name = "FullContent", columnDefinition = "TEXT", nullable = false)
    private String fullContent;
    
    @Column(name = "EffectiveDate", nullable = false)
    private LocalDate effectiveDate;
    
    @Column(name = "IsActive", nullable = false)
    private Boolean isActive = true;
    
    @Column(name = "CreatedDate", nullable = false)
    private LocalDateTime createdDate;
    
    // Constructors
    public Terms() {
        this.createdDate = LocalDateTime.now();
        this.isActive = true;
    }
    
    // Getters/Setters
    public String getTermsId() { return termsId; }
    public void setTermsId(String termsId) { this.termsId = termsId; }
    
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getShortContent() { return shortContent; }
    public void setShortContent(String shortContent) { this.shortContent = shortContent; }
    
    public String getFullContent() { return fullContent; }
    public void setFullContent(String fullContent) { this.fullContent = fullContent; }
    
    public LocalDate getEffectiveDate() { return effectiveDate; }
    public void setEffectiveDate(LocalDate effectiveDate) { this.effectiveDate = effectiveDate; }
    
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    
    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }
}