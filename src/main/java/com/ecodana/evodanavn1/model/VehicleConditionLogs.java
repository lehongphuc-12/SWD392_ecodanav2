package com.ecodana.evodanavn1.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "VehicleConditionLogs")
public class VehicleConditionLogs {

    @Id
    @Column(name = "LogId", length = 36)
    private String logId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "BookingId", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "VehicleId", nullable = false)
    private Vehicle vehicle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "StaffId")
    private User staff;

    @Column(name = "CheckType", nullable = false)
    private String checkType; // ENUM('Pickup', 'Return')

    @Column(name = "CheckTime", nullable = false)
    private LocalDateTime checkTime;

    @Column(name = "Odometer")
    private Integer odometer;

    @Column(name = "FuelLevel", length = 20)
    private String fuelLevel;

    @Column(name = "ConditionStatus", length = 100)
    private String conditionStatus;

    @Column(name = "ConditionDescription", length = 1000)
    private String conditionDescription;

    @Column(name = "DamageImages", columnDefinition = "JSON")
    private String damageImages;

    @Column(name = "Note", length = 255)
    private String note;

    public VehicleConditionLogs() {
        this.checkTime = LocalDateTime.now();
    }

    // Getters and Setters
    public String getLogId() { return logId; }
    public void setLogId(String logId) { this.logId = logId; }
    public Booking getBooking() { return booking; }
    public void setBooking(Booking booking) { this.booking = booking; }
    public Vehicle getVehicle() { return vehicle; }
    public void setVehicle(Vehicle vehicle) { this.vehicle = vehicle; }
    public User getStaff() { return staff; }
    public void setStaff(User staff) { this.staff = staff; }
    public String getCheckType() { return checkType; }
    public void setCheckType(String checkType) { this.checkType = checkType; }
    public LocalDateTime getCheckTime() { return checkTime; }
    public void setCheckTime(LocalDateTime checkTime) { this.checkTime = checkTime; }
    public Integer getOdometer() { return odometer; }
    public void setOdometer(Integer odometer) { this.odometer = odometer; }
    public String getFuelLevel() { return fuelLevel; }
    public void setFuelLevel(String fuelLevel) { this.fuelLevel = fuelLevel; }
    public String getConditionStatus() { return conditionStatus; }
    public void setConditionStatus(String conditionStatus) { this.conditionStatus = conditionStatus; }
    public String getConditionDescription() { return conditionDescription; }
    public void setConditionDescription(String conditionDescription) { this.conditionDescription = conditionDescription; }
    public String getDamageImages() { return damageImages; }
    public void setDamageImages(String damageImages) { this.damageImages = damageImages; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}