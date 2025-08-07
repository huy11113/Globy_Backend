package com.example.SpringMongoProject.Controller;

import com.example.SpringMongoProject.Entity.Notification;
import com.example.SpringMongoProject.Service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/notifications") // Đảm bảo đường dẫn này đúng
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5174"})
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getUnreadNotifications() {
        List<Notification> notifications = notificationService.getUnreadAdminNotifications();
        return ResponseEntity.ok(Map.of(
                "success", true,
                "count", notifications.size(),
                "data", notifications
        ));
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<Map<String, Object>> markNotificationAsRead(@PathVariable String id) {
        boolean success = notificationService.markAsRead(id);
        if (success) {
            return ResponseEntity.ok(Map.of("success", true, "message", "Đã đánh dấu là đã đọc."));
        } else {
            return ResponseEntity.status(404).body(Map.of("success", false, "message", "Không tìm thấy thông báo."));
        }
    }
    // Thêm 2 phương thức mới này vào BÊN TRONG class NotificationController

    /**
     * API để lấy các thông báo chưa đọc của một user cụ thể
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<Map<String, Object>> getUnreadUserNotifications(@PathVariable String userId) {
        List<Notification> notifications = notificationService.getUnreadUserNotifications(userId);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "count", notifications.size(),
                "data", notifications
        ));
    }

    /**
     * API để user đánh dấu một thông báo là đã đọc
     */
    @PostMapping("/user/{notificationId}/read")
    public ResponseEntity<Map<String, Object>> markUserNotificationAsRead(@PathVariable String notificationId) {
        boolean success = notificationService.markAsRead(notificationId);
        if (success) {
            return ResponseEntity.ok(Map.of("success", true, "message", "Đã đánh dấu là đã đọc."));
        } else {
            return ResponseEntity.status(404).body(Map.of("success", false, "message", "Không tìm thấy thông báo."));
        }
    }
}