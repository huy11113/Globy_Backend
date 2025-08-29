package com.example.SpringMongoProject.Entity;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Document(collection = "notifications")
public class Notification {
    @Id
    private String id;

    private String message; // Nội dung thông báo, vd: "User A vừa đặt tour B"
    private String bookingId; // ID của booking liên quan
    private boolean isRead = false; // Trạng thái đã đọc hay chưa
    private String recipientId; // ID của người nhận (trong trường hợp này là admin)

    @CreatedDate
    private LocalDateTime createdAt;
}