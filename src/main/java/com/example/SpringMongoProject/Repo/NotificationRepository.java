package com.example.SpringMongoProject.Repo;

import com.example.SpringMongoProject.Entity.Notification;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends MongoRepository<Notification, String> {
    // Tìm tất cả thông báo chưa đọc cho một người nhận cụ thể (ví dụ: "admin")
    List<Notification> findByRecipientIdAndIsReadFalse(String recipientId);
}