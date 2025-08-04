package com.example.SpringMongoProject.Entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
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
    private Double totalPrice;
    private String status = "pending_approval"; // Sửa lại trạng thái mặc định

    // ✅ THÊM TRƯỜNG MỚI Ở ĐÂY
    private String notes;
}