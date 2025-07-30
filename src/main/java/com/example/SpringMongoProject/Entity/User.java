package com.example.SpringMongoProject.Entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.ArrayList;
import java.util.List;

@Data
@Document(collection = "users")
public class User {
    @Id
    private String id;
    private String name;
    private String phoneNumber;
    private String password;
    private String email;
    private String role = "user";
    private String avatar;
    private String resetCode;

    @DBRef(lazy = true)
    private List<Tour> wishlist = new ArrayList<>(); // Luôn khởi tạo để tránh lỗi
}