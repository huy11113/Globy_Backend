package com.example.SpringMongoProject.Entity;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Data
@Document(collection = "blog_posts")
public class BlogPost {
    @Id
    private String id;
    private String title;
    private String category;
    private String imageUrl;
    private String excerpt; // Đoạn tóm tắt
    private String content; // Nội dung đầy đủ (HTML/Markdown)
    private String author;

    @CreatedDate
    private LocalDateTime createdAt;
}