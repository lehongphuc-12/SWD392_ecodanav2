package com.ecodana.evodanavn1.model;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "Contract")
public class Contract {
    @Id
    @Column(name = "ContractId", length = 36)
    private String contractId;

    @Column(name = "ContractCode", length = 30, nullable = false, unique = true)
    private String contractCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "UserId", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "BookingId", nullable = false)
    private Booking booking;

    @Column(name = "CreatedDate", nullable = false)
    private LocalDateTime createdDate;

    @Column(name = "SignedDate")
    private LocalDateTime signedDate;

    @Column(name = "CompletedDate")
    private LocalDateTime completedDate;

    @Column(name = "Status", length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    private ContractStatus status = ContractStatus.Draft;

    @Column(name = "TermsAccepted", nullable = false)
    private Boolean termsAccepted = false;

    @Column(name = "SignatureData", columnDefinition = "TEXT")
    private String signatureData;

    @Column(name = "SignatureMethod", length = 20)
    private String signatureMethod;

    @Column(name = "ContractPdfUrl", length = 500)
    private String contractPdfUrl;

    @Column(name = "Notes", length = 500)
    private String notes;

    @Column(name = "CancellationReason", length = 500)
    private String cancellationReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CitizenIdSnapshotId")
    private UserDocument citizenIdSnapshot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "DriverLicenseSnapshotId")
    private UserDocument driverLicenseSnapshot;

    // Constructors
    public Contract() {
        this.createdDate = LocalDateTime.now();
    }

    // Getters/Setters
    public String getContractId() { return contractId; }
    public void setContractId(String contractId) { this.contractId = contractId; }

    public String getContractCode() { return contractCode; }
    public void setContractCode(String contractCode) { this.contractCode = contractCode; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Booking getBooking() { return booking; }
    public void setBooking(Booking booking) { this.booking = booking; }

    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }

    public LocalDateTime getSignedDate() { return signedDate; }
    public void setSignedDate(LocalDateTime signedDate) { this.signedDate = signedDate; }

    public LocalDateTime getCompletedDate() { return completedDate; }
    public void setCompletedDate(LocalDateTime completedDate) { this.completedDate = completedDate; }

    public ContractStatus getStatus() { return status; }
    public void setStatus(ContractStatus status) { this.status = status; }

    public Boolean getTermsAccepted() { return termsAccepted; }
    public void setTermsAccepted(Boolean termsAccepted) { this.termsAccepted = termsAccepted; }

    public String getSignatureData() { return signatureData; }
    public void setSignatureData(String signatureData) { this.signatureData = signatureData; }

    public String getSignatureMethod() { return signatureMethod; }
    public void setSignatureMethod(String signatureMethod) { this.signatureMethod = signatureMethod; }

    public String getContractPdfUrl() { return contractPdfUrl; }
    public void setContractPdfUrl(String contractPdfUrl) { this.contractPdfUrl = contractPdfUrl; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getCancellationReason() { return cancellationReason; }
    public void setCancellationReason(String cancellationReason) { this.cancellationReason = cancellationReason; }

    public UserDocument getCitizenIdSnapshot() { return citizenIdSnapshot; }
    public void setCitizenIdSnapshot(UserDocument citizenIdSnapshot) { this.citizenIdSnapshot = citizenIdSnapshot; }

    public UserDocument getDriverLicenseSnapshot() { return driverLicenseSnapshot; }
    public void setDriverLicenseSnapshot(UserDocument driverLicenseSnapshot) { this.driverLicenseSnapshot = driverLicenseSnapshot; }

    public enum ContractStatus {
        Draft, Signed, Completed, Cancelled
    }
}
