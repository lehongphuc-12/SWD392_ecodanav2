package com.ecodana.evodanavn1.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "InappropriateWord")
public class InappropriateWord {

    @Id
    @Column(name = "Id", length = 36)
    private String id;

    @Column(name = "Word", length = 255, nullable = false, unique = true)
    private String word;

    @Column(name = "Category", length = 100)
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(name = "Severity", length = 20, nullable = false)
    private Severity severity = Severity.MEDIUM;

    @Column(name = "IsActive", nullable = false)
    private boolean isActive = true;

    @Column(name = "CreatedDate", nullable = false)
    private LocalDateTime createdDate = LocalDateTime.now();

    public enum Severity { LOW, MEDIUM, HIGH }

    public InappropriateWord() {
        this.id = UUID.randomUUID().toString();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getWord() { return word; }
    public void setWord(String word) { this.word = word; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public Severity getSeverity() { return severity; }
    public void setSeverity(Severity severity) { this.severity = severity; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }
}


