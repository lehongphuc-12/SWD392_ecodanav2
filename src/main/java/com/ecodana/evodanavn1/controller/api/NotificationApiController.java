package com.ecodana.evodanavn1.controller.api;

import com.ecodana.evodanavn1.model.Notification;
import com.ecodana.evodanavn1.model.User;
import com.ecodana.evodanavn1.service.NotificationService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notifications")
public class NotificationApiController {
    
    @Autowired
    private NotificationService notificationService;
    
    /**
     * Get all notifications for current user
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getNotifications(HttpSession session) {
        User user = (User) session.getAttribute("currentUser");
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        
        System.out.println("=== Getting notifications for user: " + user.getId() + " ===");
        
        List<Notification> notifications = notificationService.getNotificationsByUserId(user.getId());
        long unreadCount = notificationService.countUnreadNotifications(user.getId());
        
        System.out.println("Found " + notifications.size() + " notifications, " + unreadCount + " unread");
        
        List<Map<String, Object>> notificationList = notifications.stream()
            .map(this::convertToMap)
            .collect(Collectors.toList());
        
        Map<String, Object> response = new HashMap<>();
        response.put("notifications", notificationList);
        response.put("unreadCount", unreadCount);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get unread notifications count
     */
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Object>> getUnreadCount(HttpSession session) {
        User user = (User) session.getAttribute("currentUser");
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        
        long unreadCount = notificationService.countUnreadNotifications(user.getId());
        return ResponseEntity.ok(Map.of("unreadCount", unreadCount));
    }
    
    /**
     * Mark notification as read
     */
    @PostMapping("/{id}/mark-read")
    public ResponseEntity<Map<String, Object>> markAsRead(@PathVariable String id, HttpSession session) {
        User user = (User) session.getAttribute("currentUser");
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        
        notificationService.markAsRead(id);
        return ResponseEntity.ok(Map.of("success", true));
    }
    
    /**
     * Mark all notifications as read
     */
    @PostMapping("/mark-all-read")
    public ResponseEntity<Map<String, Object>> markAllAsRead(HttpSession session) {
        User user = (User) session.getAttribute("currentUser");
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        
        notificationService.markAllAsRead(user.getId());
        return ResponseEntity.ok(Map.of("success", true));
    }
    
    /**
     * Delete notification
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteNotification(@PathVariable String id, HttpSession session) {
        User user = (User) session.getAttribute("currentUser");
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        
        notificationService.deleteNotification(id);
        return ResponseEntity.ok(Map.of("success", true));
    }
    
    private Map<String, Object> convertToMap(Notification notification) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", notification.getNotificationId());
        map.put("message", notification.getMessage());
        map.put("createdDate", notification.getCreatedDate().toString());
        map.put("isRead", notification.getIsRead());
        map.put("relatedId", notification.getRelatedId());
        map.put("notificationType", notification.getNotificationType());
        return map;
    }
}
