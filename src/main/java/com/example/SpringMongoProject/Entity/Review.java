package com.example.SpringMongoProject.Entity;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate; // THÊM IMPORT
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime; // THÊM IMPORT

@Data
@Document(collection = "reviews")
public class Review {
    @Id
    private String id;
    @DBRef
    private User user;
    @DBRef
    private Tour tour;
    private Integer rating;
    private String comment;

    // --- THÊM TRƯỜNG MỚI ĐỂ LƯU DẤU THỜI GIAN ---
    @CreatedDate
    private LocalDateTime createdAt;
}