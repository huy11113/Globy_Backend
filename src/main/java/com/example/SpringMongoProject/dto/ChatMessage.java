// File mới: src/main/java/com/example/SpringMongoProject/dto/ChatMessage.java
package com.example.SpringMongoProject.dto;

import lombok.Data;

@Data
public class ChatMessage {
    private boolean fromUser;
    private String text;
    // Thêm các trường khác nếu frontend có gửi (ví dụ: image, link)
}