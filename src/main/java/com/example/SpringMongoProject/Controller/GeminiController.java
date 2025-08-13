package com.example.SpringMongoProject.Controller;

import com.example.SpringMongoProject.Service.GeminiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import com.example.SpringMongoProject.dto.ChatRequest;

@RestController
@RequestMapping("/api/chatbot")
@CrossOrigin(origins = "http://localhost:5173")
@RequiredArgsConstructor
public class GeminiController {
    private final GeminiService geminiService;

    @PostMapping("/ask")
    // ✅ Sửa lại để nhận vào đối tượng ChatRequest
    public ResponseEntity<Map<String, Object>> askChatbot(@RequestBody ChatRequest chatRequest) {
        try {
            // ✅ Gọi phương thức mới có khả năng xử lý "trí nhớ"
            Map<String, Object> response = geminiService.askGeminiWithMemory(chatRequest.getHistory());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("success", false, "message", "Đã xảy ra lỗi: " + e.getMessage()));
        }
    }
}