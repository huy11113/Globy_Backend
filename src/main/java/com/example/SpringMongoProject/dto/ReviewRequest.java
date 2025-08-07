package com.example.SpringMongoProject.dto;

import lombok.Data;

@Data
public class ReviewRequest {
    private String userId;
    private String tourId;
    private Integer rating;
    private String comment;
}