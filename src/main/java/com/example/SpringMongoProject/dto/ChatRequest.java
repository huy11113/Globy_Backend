// File mới: src/main/java/com/example/SpringMongoProject/dto/ChatRequest.java
package com.example.SpringMongoProject.dto;

import lombok.Data;
import java.util.List;

@Data
public class ChatRequest {
    private List<ChatMessage> history;
}