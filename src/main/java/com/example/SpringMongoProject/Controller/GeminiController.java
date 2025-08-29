// File: src/main/java/com/example/SpringMongoProject/Controller/GeminiController.java
package com.example.SpringMongoProject.Controller;

import com.example.SpringMongoProject.Service.GeminiService;
import com.example.SpringMongoProject.dto.ChatRequest; // Thêm import này
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/chatbot")
@CrossOrigin(origins = "http://localhost:5173")
@RequiredArgsConstructor
public class GeminiController {
    private final GeminiService geminiService;

    @PostMapping("/ask")
    // ✅ Endpoint này nhận vào đối tượng ChatRequest có chứa lịch sử chat
    public ResponseEntity<Map<String, Object>> askChatbot(@RequestBody ChatRequest chatRequest) {
        try {
            // ✅ Gọi phương thức askGeminiWithMemory
            Map<String, Object> response = geminiService.askGeminiWithMemory(chatRequest.getHistory());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("success", false, "message", "Đã xảy ra lỗi: " + e.getMessage()));
        }
    }
}