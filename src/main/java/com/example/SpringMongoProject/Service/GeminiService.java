package com.example.SpringMongoProject.Service;

import com.example.SpringMongoProject.Entity.Tour;
import com.example.SpringMongoProject.dto.ChatMessage;
import com.example.SpringMongoProject.dto.ExtractedEntities;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class GeminiService {

    @Autowired
    private Client client;
    @Autowired
    private TourService tourService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Phương thức chính, có khả năng xử lý lịch sử hội thoại (trí nhớ).
     * @param history Toàn bộ lịch sử chat từ đầu đến giờ.
     * @return Kết quả trả lời của AI.
     */
    public Map<String, Object> askGeminiWithMemory(List<ChatMessage> history) {
        try {
            // Lấy câu hỏi mới nhất của người dùng
            String latestPrompt = history.get(history.size() - 1).getText();

            // Xây dựng ngữ cảnh từ các lượt trao đổi trước đó
            StringBuilder context = new StringBuilder();
            // Chỉ lấy tối đa 4 lượt gần nhất (2 câu hỏi, 2 câu trả lời) để tránh prompt quá dài
            history.stream().limit(Math.max(0, history.size() - 1)).skip(Math.max(0, history.size() - 5))
                    .forEach(msg -> {
                        String role = msg.isFromUser() ? "User" : "AI";
                        context.append(role).append(": ").append(msg.getText()).append("\n");
                    });

            // Bước 1: Trích xuất thực thể, có xem xét ngữ cảnh
            ExtractedEntities entities = extractEntitiesFromPrompt(latestPrompt, context.toString());

            // Bước 2: Dùng các thực thể đã trích xuất để tìm kiếm
            List<Tour> candidateTours = tourService.findToursWithFilters(entities);

            if (candidateTours.isEmpty()) {
                return Map.of("success", true, "response", "Xin lỗi, tôi không tìm thấy tour nào phù hợp với các tiêu chí của bạn.");
            }
            // Nếu MongoDB chỉ trả về 1 kết quả duy nhất, không cần hỏi lại Gemini
            if (candidateTours.size() == 1) {
                return formatBasicTourInfo(candidateTours.get(0));
            }

            // Bước 3: Dùng AI để xếp hạng và chọn lọc kết quả dựa trên sự liên quan
            return rankAndSelectTours(candidateTours, latestPrompt, entities.getLocation());

        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("success", false, "message", "Đã xảy ra lỗi khi xử lý yêu cầu của bạn: " + e.getMessage());
        }
    }

    /**
     * Trích xuất thông tin từ câu hỏi của người dùng, có xem xét ngữ cảnh.
     */
    private ExtractedEntities extractEntitiesFromPrompt(String prompt, String context) throws Exception {
        String extractionPrompt = String.format(
                "Bạn là một hệ thống phân tích ngôn ngữ du lịch chuyên nghiệp. Dựa vào NGỮ CẢNH HỘI THOẠI và CÂU HỎI MỚI NHẤT, hãy trích xuất các thông tin sau ra định dạng JSON.\n\n" +
                        "### HƯỚNG DẪN CHI TIẾT:\n" +
                        "1. `keywords`: Tổng hợp từ khóa chính về địa điểm và hoạt động. Nếu câu hỏi mới là câu hỏi phụ (ví dụ: 'cái nào rẻ hơn?'), hãy kết hợp nó với từ khóa từ ngữ cảnh.\n" +
                        "2. `location`: Địa danh chính được đề cập. Ví dụ: 'hạ long', 'đà nẵng'. Nếu không có, để là null.\n" +
                        "3. `max_price`: Ngân sách tối đa của người dùng. Luôn trả về một SỐ NGUYÊN. Ví dụ: '10 triệu' -> 10000000. Nếu không đề cập, giá trị phải là null.\n" +
                        "4. `category`: Loại hình chuyến đi như 'gia đình', 'mạo hiểm', 'nghỉ dưỡng'. Nếu không có, giá trị là null.\n" +
                        "5. `duration`: Thời lượng chuyến đi. Ví dụ: '5 ngày'. Nếu không có, giá trị là null.\n\n" +
                        "### NGỮ CẢNH HỘI THOẠI:\n%s\n\n" +
                        "### CÂU HỎI MỚI NHẤT:\n\"%s\"\n\n" +
                        "### JSON KẾT QUẢ (chỉ trả về JSON):\n",
                context.isEmpty() ? "Không có" : context,
                prompt
        );

        GenerateContentResponse response = client.models.generateContent("gemini-1.5-flash-latest", extractionPrompt, null);
        String jsonResponse = extractJson(response.text().trim());
        JsonNode rootNode = objectMapper.readTree(jsonResponse);
        JsonNode entitiesNode = rootNode.path("entities");

        ExtractedEntities entities = new ExtractedEntities();
        entities.setKeywords(entitiesNode.path("keywords").asText(prompt));
        entities.setLocation(entitiesNode.path("location").asText(null));
        entities.setMaxPrice(entitiesNode.path("max_price").isNull() ? null : entitiesNode.path("max_price").asLong());
        entities.setCategory(entitiesNode.path("category").isNull() ? null : entitiesNode.path("category").asText());
        entities.setDuration(entitiesNode.path("duration").isNull() ? null : entitiesNode.path("duration").asText());

        System.out.println("Đã trích xuất -> " + entities);
        return entities;
    }

    /**
     * Dùng AI để xếp hạng và chọn lọc tour từ danh sách ứng viên.
     */
    private Map<String, Object> rankAndSelectTours(List<Tour> candidateTours, String prompt, String primaryLocation) throws Exception {
        StringBuilder tourKnowledgeBase = new StringBuilder();
        candidateTours.forEach(tour -> tourKnowledgeBase.append(String.format("id: \"%s\", title: \"%s\", city: \"%s\", description: \"%s\"\n",
                tour.getId(), tour.getTitle(), tour.getCity(), tour.getDescription())));

        String rankingPrompt = String.format(
                "Bạn là một chuyên gia tư vấn du lịch. Dựa vào câu hỏi của người dùng và danh sách tour CÓ SẴN, hãy xếp hạng độ phù hợp.\n\n" +
                        "### HƯỚNG DẪN RA QUYẾT ĐỊNH:\n" +
                        "1. **Ưu tiên địa điểm chính:** Tour nào có trường 'city' hoặc 'title' khớp với địa điểm chính là '%s' phải được ưu tiên lên hàng đầu.\n" +
                        "2. **Phân tích số lượng:** Nếu câu hỏi của người dùng chứa các từ như 'một', '1', 'duy nhất', 'một chuyến', hoặc 'a trip', HÃY CỐ GẮNG HẾT SỨC để chọn ra MỘT tour tốt nhất và điền vào `best_match_id`.\n\n" +
                        "### Nhiệm vụ:\n" +
                        "1. Phân tích câu hỏi, địa điểm chính và yêu cầu về số lượng.\n" +
                        "2. Sắp xếp lại toàn bộ danh sách tour ứng viên theo mức độ phù hợp.\n" +
                        "3. Trả lời bằng một đối tượng JSON DUY NHẤT có cấu trúc sau:\n" +
                        "   {\n" +
                        "     \"best_match_id\": \"id_cua_tour_phu_hop_nhat_neu_tu_tin\",\n" +
                        "     \"ranked_tour_ids\": [\"id_1\", \"id_2\", \"id_3\", ...]\n" +
                        "   }\n" +
                        "- `best_match_id`: Chỉ điền ID vào đây nếu bạn rất tự tin đó là kết quả duy nhất tốt nhất. Nếu không, để là null.\n" +
                        "- `ranked_tour_ids`: Luôn trả về một mảng chứa ID của các tour, được sắp xếp theo mức độ phù hợp từ cao đến thấp.\n\n" +
                        "### DANH SÁCH TOUR ỨNG VIÊN:\n%s\n\n" +
                        "### Câu hỏi của người dùng:\n\"%s\"",
                primaryLocation != null ? primaryLocation : "bất kỳ",
                tourKnowledgeBase.toString(),
                prompt
        );

        GenerateContentResponse response = client.models.generateContent("gemini-1.5-flash-latest", rankingPrompt, null);
        JsonNode rootNode = objectMapper.readTree(extractJson(response.text().trim()));

        String bestMatchId = rootNode.path("best_match_id").asText(null);
        if (bestMatchId != null && !bestMatchId.isEmpty()) {
            return candidateTours.stream().filter(t -> t.getId().equals(bestMatchId)).findFirst()
                    .map(this::formatBasicTourInfo)
                    .orElseGet(() -> formatSimpleTourList(candidateTours)); // Dự phòng nếu ID không hợp lệ
        }

        List<String> rankedTourIds = new ArrayList<>();
        rootNode.path("ranked_tour_ids").forEach(idNode -> rankedTourIds.add(idNode.asText()));
        if (rankedTourIds.isEmpty()) return formatSimpleTourList(candidateTours); // Dự phòng

        List<Tour> finalTours = rankedTourIds.stream()
                .map(id -> candidateTours.stream().filter(t -> t.getId().equals(id)).findFirst().orElse(null))
                .filter(Objects::nonNull).collect(Collectors.toList());

        return formatSimpleTourList(finalTours);
    }

    private String extractJson(String rawResponse) {
        int startIndex = rawResponse.indexOf('{');
        int endIndex = rawResponse.lastIndexOf('}');
        if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
            return rawResponse.substring(startIndex, endIndex + 1);
        }
        return "{}";
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