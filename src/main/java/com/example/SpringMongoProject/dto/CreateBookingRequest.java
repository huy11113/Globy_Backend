package com.example.SpringMongoProject.dto;

import lombok.Data;
import java.util.Date;

@Data
public class CreateBookingRequest {
    private String userId;
    private String tourId;
    private Date startDate;
    private Integer people;
    private Double totalPrice;

    // ✅ THÊM TRƯỜDNG MỚI Ở ĐÂY
    private String notes;
}