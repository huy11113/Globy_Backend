package com.example.SpringMongoProject.Entity;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.Date;

@Data
@Document(collection = "bookings")
public class Booking {
    @Id
    private String id;
    @DBRef
    private User user;
    @DBRef
    private Tour tour;
    private Date startDate;
    private Integer people;

    // ✅ THAY ĐỔI: Chuyển sang Long để lưu tổng tiền VNĐ
    private Long totalPrice;

    private String status = "pending_approval";
    private String notes;
    private Long paymentOrderCode;

    @CreatedDate
    private LocalDateTime createdAt;
}