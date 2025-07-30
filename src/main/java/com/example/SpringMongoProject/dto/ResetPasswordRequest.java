package com.example.SpringMongoProject.dto;

import lombok.Data;

@Data
public class ResetPasswordRequest {
    private String phoneNumber;
    private String resetCode;
    private String newPassword;
}