// File: src/main/java/com/example/SpringMongoProject/Service/EmbeddingService.java
package com.example.SpringMongoProject.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value; // Thêm import này
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class EmbeddingService {
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ✅ TỐT HƠN: Đọc URL từ biến môi trường, linh hoạt hơn cho production
    @Value("${EMBEDDING_SERVICE_URL:http://127.0.0.1:5001/embed}") // Giá trị mặc định là localhost
    private String embeddingServiceUrl;

    public List<Double> createEmbedding(String text) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> requestBody = Collections.singletonMap("text", text);
            HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(requestBody, headers);

            // Thực hiện "cuộc gọi" đến dịch vụ Python bằng URL đã được cấu hình
            String response = restTemplate.postForObject(embeddingServiceUrl, requestEntity, String.class);

            // Xử lý kết quả trả về
            JsonNode rootNode = objectMapper.readTree(response);
            JsonNode embeddingNode = rootNode.path("embedding");
            List<Double> embedding = new ArrayList<>();
            if (embeddingNode.isArray()) {
                for (JsonNode node : embeddingNode) {
                    embedding.add(node.asDouble());
                }
            }
            return embedding;

        } catch (Exception e) {
            System.err.println("LỖI NGHIÊM TRỌNG: Không thể kết nối đến dịch vụ embedding Python tại: " + embeddingServiceUrl);
            System.err.println("Chi tiết lỗi: " + e.getMessage());
            throw new RuntimeException("Không thể tạo embedding do dịch vụ AI không phản hồi.", e);
        }
    }
}