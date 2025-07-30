package com.example.SpringMongoProject.Entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Date;

@Data
@Document(collection = "payments")
public class Payment {
    @Id
    private String id;
    private String userId;
    private Double amount;
    private String method; // "credit_card", "paypal", "momo"
    private String status = "pending";
    private Date paidAt;
    private String bookingId;   // ID của Booking hoặc HotelBooking
    private String bookingModel; // "Booking" hoặc "HotelBooking"
}