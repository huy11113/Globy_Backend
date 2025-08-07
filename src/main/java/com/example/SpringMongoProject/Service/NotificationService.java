package com.example.SpringMongoProject.Service;

import com.example.SpringMongoProject.Entity.Notification;
import com.example.SpringMongoProject.Repo.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    public List<Notification> getUnreadAdminNotifications() {
        return notificationRepository.findByRecipientIdAndIsReadFalse("admin");
    }

    public boolean markAsRead(String notificationId) {
        return notificationRepository.findById(notificationId).map(notification -> {
            notification.setRead(true);
            notificationRepository.save(notification);
            return true;
        }).orElse(false);
    }
    // Thêm phương thức này vào BÊN TRONG class NotificationService

    public List<Notification> getUnreadUserNotifications(String userId) {
        return notificationRepository.findByRecipientIdAndIsReadFalse(userId);
    }
}