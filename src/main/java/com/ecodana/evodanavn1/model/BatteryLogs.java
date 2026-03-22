package com.ecodana.evodanavn1.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "BatteryLogs")
public class BatteryLogs {

    @Id
    @Column(name = "LogId", length = 36)
    private String logId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "VehicleId", nullable = false)
    private Vehicle vehicle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "BookingId")
    private Booking booking;

    @Column(name = "BatteryLevel", precision = 5, scale = 2, nullable = false)
    private BigDecimal batteryLevel;

    @Column(name = "CheckTime", nullable = false)
    private LocalDateTime checkTime;

    @Column(name = "Note", length = 255)
    private String note;

    public BatteryLogs() {
        this.checkTime = LocalDateTime.now();
    }

    // Getters and Setters
    public String getLogId() { return logId; }
    public void setLogId(String logId) { this.logId = logId; }
    public Vehicle getVehicle() { return vehicle; }
    public void setVehicle(Vehicle vehicle) { this.vehicle = vehicle; }
    public Booking getBooking() { return booking; }
    public void setBooking(Booking booking) { this.booking = booking; }
    public BigDecimal getBatteryLevel() { return batteryLevel; }
    public void setBatteryLevel(BigDecimal batteryLevel) { this.batteryLevel = batteryLevel; }
    public LocalDateTime getCheckTime() { return checkTime; }
    public void setCheckTime(LocalDateTime checkTime) { this.checkTime = checkTime; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}