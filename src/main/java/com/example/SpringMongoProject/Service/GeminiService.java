package com.example.SpringMongoProject.Service;

import com.example.SpringMongoProject.Entity.Tour;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import com.google.genai.errors.ClientException;
import com.google.genai.types.GenerateContentResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class GeminiService {

    @Autowired
    private Client client; // Inject client Gemini

    @Autowired
    private TourService tourService; // Inject TourService để lọc sơ bộ

    private final ObjectMapper objectMapper = new ObjectMapper();

    public Map<String, Object> askGemini(String prompt) {
        try {
            // Bước 1: Dùng Text Search của MongoDB để lọc ra các tour có khả năng liên quan nhất
            // Giới hạn kết quả để không gửi quá nhiều dữ liệu cho Gemini
            List<Tour> candidateTours = tourService.findToursByKeywords(prompt, null);

            // Nếu MongoDB không tìm thấy gì, trả về luôn để tiết kiệm chi phí API
            if (candidateTours.isEmpty()) {
                return Map.of("success", true, "response", "Xin lỗi, tôi không tìm thấy tour nào phù hợp với yêu cầu của bạn.");
            }

            // Bước 2: Xây dựng một "khối kiến thức" thu nhỏ chỉ từ các tour ứng viên
            StringBuilder tourKnowledgeBase = new StringBuilder();
            for (Tour tour : candidateTours) {
                // Cung cấp thông tin cốt lõi để Gemini có thể xếp hạng
                tourKnowledgeBase.append(String.format("id: \"%s\", title: \"%s\", description: \"%s\", price: %d, tags: \"%s\"\n",
                        tour.getId(),
                        tour.getTitle(),
                        tour.getDescription(),
                        tour.getPrice(),
                        tour.getTags() != null ? String.join(", ", tour.getTags()) : ""
                ));
            }

            // Bước 3: Tạo prompt mới, yêu cầu Gemini chọn lọc từ danh sách đã được rút gọn
            String geminiPrompt = String.format(
                    "Bạn là một chuyên gia tư vấn du lịch thông minh. Dựa vào danh sách các tour CÓ SẴN dưới đây và câu hỏi của người dùng, hãy chọn ra những tour phù hợp nhất.\n\n" +
                            "### Nhiệm vụ:\n" +
                            "1. Đọc kỹ câu hỏi của người dùng để hiểu rõ ý định, sở thích và ngân sách của họ.\n" +
                            "2. Từ 'DANH SÁCH TOUR ỨNG VIÊN' được cung cấp, hãy tìm ra ID của những tour khớp với yêu cầu của người dùng nhất.\n" +
                            "3. Trả lời bằng một đối tượng JSON DUY NHẤT có cấu trúc sau:\n" +
                            "   { \"found_tour_ids\": [\"id_tour_phu_hop_nhat_1\", \"id_tour_phu_hop_nhat_2\", ...] }\n" +
                            "   - Nếu không có tour nào trong danh sách ứng viên thực sự phù hợp, hãy trả về một mảng rỗng.\n" +
                            "   - KHÔNG thêm bất kỳ giải thích hay văn bản nào khác ngoài đối tượng JSON này.\n\n" +
                            "### DANH SÁCH TOUR ỨNG VIÊN (Đơn vị giá là VNĐ):\n" +
                            "%s\n\n" +
                            "### Câu hỏi của người dùng:\n" +
                            "\"%s\"",
                    tourKnowledgeBase.toString(),
                    prompt
            );

            // Bước 4: Gọi API Gemini với prompt đã được tối ưu
            GenerateContentResponse response = client.models.generateContent(
                    "gemini-1.5-flash-latest",
                    geminiPrompt,
                    null
            );

            String geminiResponseRaw = response.text().trim();
            System.out.println("Phản hồi từ Gemini (thô): " + geminiResponseRaw);

            // Bước 5: Xử lý phản hồi JSON từ Gemini
            String geminiResponseJson = extractJson(geminiResponseRaw);
            if (geminiResponseJson.isEmpty()) {
                // Nếu Gemini không trả về JSON, ta sẽ tạm trả về kết quả lọc sơ bộ của MongoDB
                return formatSimpleTourList(candidateTours);
            }

            JsonNode rootNode = objectMapper.readTree(geminiResponseJson);
            List<String> finalTourIds = new ArrayList<>();
            if (rootNode.has("found_tour_ids") && rootNode.get("found_tour_ids").isArray()) {
                for (JsonNode idNode : rootNode.get("found_tour_ids")) {
                    finalTourIds.add(idNode.asText());
                }
            }

            // Nếu Gemini không chọn được tour nào, trả về danh sách rỗng
            if (finalTourIds.isEmpty()) {
                return Map.of("success", true, "response", "Xin lỗi, tôi không tìm thấy tour nào phù hợp với yêu cầu của bạn.");
            }

            // Bước 6: Lọc danh sách ứng viên ban đầu để lấy ra các tour cuối cùng do Gemini chọn
            List<Tour> finalTours = candidateTours.stream()
                    .filter(tour -> finalTourIds.contains(tour.getId()))
                    .collect(Collectors.toList());

            // Bước 7: Định dạng kết quả trả về
            if (finalTours.size() == 1) {
                return formatBasicTourInfo(finalTours.get(0));
            } else {
                return formatSimpleTourList(finalTours);
            }

        } catch (ClientException | IOException e) {
            e.printStackTrace();
            return Map.of("success", false, "message", "Đã xảy ra lỗi khi xử lý yêu cầu của bạn: " + e.getMessage());
        }
    }

    private String extractJson(String rawResponse) {
        int startIndex = rawResponse.indexOf('{');
        int endIndex = rawResponse.lastIndexOf('}');
        if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
            return rawResponse.substring(startIndex, endIndex + 1);
        }
        return "";
    }

    private Map<String, Object> formatBasicTourInfo(Tour tour) {
        Map<String, Object> tourData = new HashMap<>();
        tourData.put("text", String.format("Tôi đã tìm thấy một tour rất phù hợp với bạn:\n**%s**", tour.getTitle()));
        tourData.put("image", tour.getImage());
        tourData.put("link", "/tours/" + tour.getId());
        return Map.of("success", true, "response", tourData);
    }

    private Map<String, Object> formatSimpleTourList(List<Tour> tours) {
        List<Map<String, String>> tourListForResponse = new ArrayList<>();
        for (Tour tour : tours) {
            Map<String, String> tourInfo = new HashMap<>();
            String formattedPrice = String.format("%,d", tour.getPrice()).replace(',', '.');

            tourInfo.put("title", tour.getTitle());
            tourInfo.put("details", String.format("Giá: %s VNĐ - %s", formattedPrice, tour.getDuration()));
            tourInfo.put("link", "/tours/" + tour.getId());
            tourInfo.put("image", tour.getImage());
            tourListForResponse.add(tourInfo);
        }

        Map<String, Object> responseData = new HashMap<>();
        responseData.put("text", "Tuyệt vời! Dưới đây là những tour phù hợp nhất tôi tìm thấy cho bạn:");
        responseData.put("tours", tourListForResponse);

        return Map.of("success", true, "response", responseData);
    }
}