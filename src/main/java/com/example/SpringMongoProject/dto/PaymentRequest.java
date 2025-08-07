package com.example.SpringMongoProject.dto;

import lombok.Data;

@Data
public class PaymentRequest {
    private String bookingId;

    // ✅ THAY ĐỔI: Chuyển sang Long
    private Long amount;

    private String method; // "credit_card", "paypal", "momo"
}