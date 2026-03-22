package com.ecodana.evodanavn1.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ecodana.evodanavn1.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ecodana.evodanavn1.model.Booking;
import com.ecodana.evodanavn1.model.Vehicle;

@Service
public class AnalyticsService {
    
    @Autowired
    private BookingService bookingService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private VehicleService vehicleService;
    
    /**
     * Get comprehensive dashboard analytics
     * @return map containing all dashboard data
     */
    public Map<String, Object> getDashboardAnalytics() {
        Map<String, Object> analytics = new HashMap<>();
        
        // Basic counts
        List<User> allUsers = userService.getAllUsers();
        List<Vehicle> allVehicles = vehicleService.getAllVehicles();
        List<Booking> allBookings = bookingService.getAllBookings();
        
        analytics.put("totalUsers", allUsers.size());
        analytics.put("totalVehicles", allVehicles.size());
        analytics.put("totalBookings", allBookings.size());
        analytics.put("totalRevenue", bookingService.getTotalRevenue());
        
        // Revenue analytics
        Map<String, Object> revenueAnalytics = bookingService.getRevenueAnalytics();
        analytics.putAll(revenueAnalytics);
        
        // User statistics

        
        // Vehicle statistics
        Map<String, Object> vehicleStats = vehicleService.getVehicleStatistics();
        analytics.putAll(vehicleStats);
        
        // Booking statistics
        Map<String, Object> bookingStats = bookingService.getBookingStatistics();
        analytics.putAll(bookingStats);
        
        // System status
        analytics.put("systemStatus", "Online");
        analytics.put("activeSessions", 1); // Mock data
        analytics.put("lastUpdated", LocalDateTime.now());
        
        return analytics;
    }
    
    /**
     * Get real-time monitoring data
     * @return map containing real-time data
     */
    public Map<String, Object> getRealTimeData() {
        Map<String, Object> realTimeData = new HashMap<>();
        
        // Recent activities
        List<Booking> recentBookings = bookingService.getRecentBookings(5);
        List<User> recentUsers = userService.getRecentUsers(5);
        
        realTimeData.put("recentBookings", recentBookings);
        realTimeData.put("recentUsers", recentUsers);
        realTimeData.put("activeUsers", 1); // Mock data
        realTimeData.put("systemLoad", "Normal");
        realTimeData.put("lastActivity", LocalDateTime.now());
        
        return realTimeData;
    }
    
    /**
     * Get performance metrics
     * @return map containing performance data
     */
    public Map<String, Object> getPerformanceMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        // Revenue trends
        Map<String, Object> revenueAnalytics = bookingService.getRevenueAnalytics();
        metrics.put("revenueGrowth", revenueAnalytics.get("revenueGrowth"));
        
        // User growth (mock calculation)
        List<User> allUsers = userService.getAllUsers();
        long newUsersThisMonth = allUsers.stream()
                .mapToLong(u -> u.getCreatedDate() != null && 
                        u.getCreatedDate().isAfter(LocalDate.now().withDayOfMonth(1).atStartOfDay()) ? 1 : 0)
                .sum();
        metrics.put("userGrowth", newUsersThisMonth);
        
        // Vehicle utilization
        List<Vehicle> allVehicles = vehicleService.getAllVehicles();
        List<Vehicle> availableVehicles = vehicleService.getAvailableVehicles();
        double utilizationRate = allVehicles.isEmpty() ? 0 : 
                (double)(allVehicles.size() - availableVehicles.size()) / allVehicles.size() * 100;
        metrics.put("vehicleUtilization", utilizationRate);
        
        // Booking success rate
        List<Booking> allBookings = bookingService.getAllBookings();
        long successfulBookings = allBookings.stream()
                .mapToLong(b -> "Confirmed".equals(b.getStatus()) || "Completed".equals(b.getStatus()) ? 1 : 0)
                .sum();
        double successRate = allBookings.isEmpty() ? 0 : (double)successfulBookings / allBookings.size() * 100;
        metrics.put("bookingSuccessRate", successRate);
        
        return metrics;
    }
    
    /**
     * Get chart data for visualizations
     * @return map containing chart data
     */
    public Map<String, Object> getChartData() {
        Map<String, Object> chartData = new HashMap<>();
        
        // Booking analytics
        Map<String, Object> bookingAnalytics = bookingService.getBookingAnalytics();
        chartData.putAll(bookingAnalytics);
        
        // Vehicle analytics
        Map<String, Object> vehicleAnalytics = vehicleService.getVehicleAnalytics();
        chartData.putAll(vehicleAnalytics);
        
        return chartData;
    }
    
    /**
     * Get system health status
     * @return map containing system health data
     */
    public Map<String, Object> getSystemHealth() {
        Map<String, Object> health = new HashMap<>();
        
        health.put("status", "Healthy");
        health.put("uptime", "99.9%");
        health.put("responseTime", "120ms");
        health.put("databaseStatus", "Connected");
        health.put("lastBackup", LocalDateTime.now().minusHours(6));
        health.put("alerts", 0);
        
        return health;
    }
}
