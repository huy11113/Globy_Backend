package com.example.SpringMongoProject.Entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Date;
// Thêm 2 import này
import java.time.LocalDateTime;
import org.springframework.data.annotation.CreatedDate;


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
    private Double totalPrice;
    private String status = "pending_approval";
    private String notes;
    private Long paymentOrderCode;

    // --- THÊM TRƯỜNG NÀY ---
    @CreatedDate
    private LocalDateTime createdAt;
}