package com.example.SpringMongoProject.Controller;

import com.example.SpringMongoProject.Service.GeminiService;
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
    public ResponseEntity<Map<String, Object>> askChatbot(@RequestBody String prompt) {
        try {
            // Phương thức askGemini() giờ đây trả về Map<String, Object>
            Map<String, Object> response = geminiService.askGemini(prompt);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "message", "Đã xảy ra lỗi: " + e.getMessage()));
        }
    }
}