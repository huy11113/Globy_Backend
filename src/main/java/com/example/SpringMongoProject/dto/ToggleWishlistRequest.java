package com.example.SpringMongoProject.dto;

import lombok.Data;

@Data
public class ToggleWishlistRequest {
    private String userId;
    private String tourId;
}