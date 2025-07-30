package com.example.SpringMongoProject.dto;

import lombok.Data;

@Data
public class ForgotPasswordRequest {
    private String phoneNumber;
}