package com.example.SpringMongoProject.dto;

import lombok.Data;

@Data
public class PaymentRequest {
    private String bookingId;


    private Long amount;

    private String method;
}