// File mới: src/main/java/com/example/SpringMongoProject/dto/ChatRequest.java
package com.example.SpringMongoProject.dto;

import lombok.Data;
import java.util.List;

@Data
public class ChatRequest {
    // Tên "history" phải khớp với key trong body JSON mà frontend gửi lên
    private List<ChatMessage> history;
}