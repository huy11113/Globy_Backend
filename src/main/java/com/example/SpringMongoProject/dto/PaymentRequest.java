package com.example.SpringMongoProject.dto;

import lombok.Data;

@Data
public class PaymentRequest {
    private String bookingId;
    private Double amount;
    private String method; // "credit_card", "paypal", "momo"
}