// File: src/main/java/com/example/SpringMongoProject/Service/GeminiService.java
package com.example.SpringMongoProject.Service;

import com.example.SpringMongoProject.Entity.Tour;
import com.example.SpringMongoProject.Repo.TourRepository;
import com.example.SpringMongoProject.dto.ChatMessage;
import com.example.SpringMongoProject.dto.ExtractedEntities;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class GeminiService {

    @Autowired
    private Client client;
    @Autowired
    private TourService tourService;
    @Autowired
    private EmbeddingService embeddingService;
    @Autowired
    private TourVectorCache tourVectorCache;
    @Autowired
    private TourRepository tourRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();


    public Map<String, Object> askGeminiWithMemory(List<ChatMessage> history) {
        try {
            String latestPrompt = history.get(history.size() - 1).getText();
            StringBuilder context = new StringBuilder();
            history.stream().limit(Math.max(0, history.size() - 1)).skip(Math.max(0, history.size() - 5))
                    .forEach(msg -> {
                        String role = msg.isFromUser() ? "User" : "AI";
                        context.append(role).append(": ").append(msg.getText()).append("\n");
                    });

            System.out.println("\n----------- BẮT ĐẦU XỬ LÝ YÊU CẦU -----------");
            System.out.println("[LOG] Câu hỏi mới từ người dùng: " + latestPrompt);

            // Bước 1: Gọi AI một lần duy nhất
            Map<String, Object> aiDecision = getAiDecision(latestPrompt, context.toString());
            ExtractedEntities entities = (ExtractedEntities) aiDecision.get("entities");

            System.out.println("[LOG] Các thực thể AI trích xuất được: " + entities.toString());

            // Bước 2: Tìm kiếm ngữ nghĩa
            List<Double> queryVector = embeddingService.createEmbedding(entities.getKeywords());
            List<String> similarTourIds = tourVectorCache.findSimilarTourIds(queryVector, 100);
            System.out.println("[LOG] Tìm thấy " + similarTourIds.size() + " tour tương đồng ban đầu.");

            if (similarTourIds.isEmpty()) {
                return handleNoResults("Không có tour tương đồng ban đầu", null);
            }

            // Bước 3: Lấy dữ liệu và làm đầy thông tin
            List<Tour> candidateTours = tourRepository.findAllById(similarTourIds);
            tourService.populateDestinationsForTours(candidateTours);

            // Bước 4: Lọc kết quả với logic thông minh
            List<Tour> filteredTours = filterCandidates(candidateTours, entities);
            System.out.println("[LOG] Sau khi lọc, còn lại: " + filteredTours.size() + " tour.");

            // Bước 5: Logic tự động nới lỏng bộ lọc nếu không có kết quả
            if (filteredTours.isEmpty() && (entities.getMaxPrice() != null || entities.getCategory() != null || entities.getDuration() != null)) {
                System.out.println("[LOG] Không có kết quả, thử nới lỏng bộ lọc (giữ lại địa điểm)...");
                ExtractedEntities relaxedEntities = new ExtractedEntities();
                relaxedEntities.setLocation(entities.getLocation()); // Chỉ giữ lại địa điểm
                relaxedEntities.setKeywords(entities.getKeywords());

                filteredTours = filterCandidates(candidateTours, relaxedEntities);
                System.out.println("[LOG] Sau khi nới lỏng, còn lại: " + filteredTours.size() + " tour.");
            }

            if (filteredTours.isEmpty()) {
                return handleNoResults("Không tour nào vượt qua bộ lọc, kể cả khi đã nới lỏng", entities.getLocation());
            }

            // Bước 6: Trả về kết quả
            String action = (String) aiDecision.get("action");
            if ("single_result".equals(action) && !filteredTours.isEmpty()) {
                Tour bestTour = filteredTours.stream()
                        .max(Comparator.comparing(Tour::getRating))
                        .orElse(filteredTours.get(0));
                return handleSingleResult(bestTour);
            } else {
                List<Tour> topTours = filteredTours.stream()
                        .sorted(Comparator.comparing(Tour::getRating).reversed())
                        .limit(10)
                        .collect(Collectors.toList());
                return formatSimpleTourList(topTours);
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("----------- KẾT THÚC (Lý do: Gặp lỗi Exception) -----------");
            return Map.of("success", false, "message", "Đã xảy ra lỗi khi xử lý yêu cầu của bạn: " + e.getMessage());
        }
    }
    private Map<String, Object> getAiDecision(String prompt, String context) throws Exception {
        String decisionPrompt = String.format(
                "Bạn là bộ não của một trợ lý du lịch siêu thông minh. Phân tích CÂU HỎI MỚI NHẤT và NGỮ CẢNH HỘI THOẠI, sau đó trả về một JSON duy nhất chứa 2 phần: `entities` (các thông tin trích xuất) và `action` (hành động tiếp theo).\n\n" +
                        "### QUY TẮC TRÍCH XUẤT `entities` (Rất quan trọng):\n" +
                        "1.  `keywords`: Luôn trả về từ khóa chính, súc tích nhất để tìm kiếm. (ví dụ: 'tour đi biển đà nẵng giá rẻ').\n" +
                        "2.  `location`: Chỉ trả về địa danh (thành phố, quốc gia) nếu được nêu rõ. Nếu không, BẮT BUỘC là null.\n" +
                        "3.  `max_price`: Chỉ trả về SỐ NGUYÊN nếu người dùng nêu rõ. SUY LUẬN:\n" +
                        "    - 'dưới 10 triệu', 'không quá 10 triệu' -> 10000000\n" +
                        "    - 'tầm 7 triệu', 'khoảng 7 triệu' -> 7000000 (sẽ được xử lý khoảng giá sau)\n" +
                        "    - 'giá rẻ', 'tiết kiệm' -> 5000000 (giả định một mức giá hợp lý)\n" +
                        "    - Nếu không đề cập, BẮT BUỘC là null.\n" +
                        "4.  `category`: Chỉ trả về loại hình tour nếu có. SUY LUẬN:\n" +
                        "    - 'đi biển', 'tắm biển', 'nghỉ mát ở biển' -> 'biển'\n" +
                        "    - 'leo núi', 'khám phá', 'trekking' -> 'mạo hiểm'\n" +
                        "    - 'văn hóa', 'lịch sử', 'cổ kính' -> 'văn hóa'\n" +
                        "    - Nếu không đề cập, BẮT BUỘC là null.\n" +
                        "5.  `duration`: Chỉ trả về thời lượng nếu có (ví dụ: '3 ngày', 'cuối tuần'). Nếu không, BẮT BUỘC là null.\n\n" +
                        "### QUY TẮC QUYẾT ĐỊNH `action`:\n" +
                        "1.  `single_result`: Nếu câu hỏi chỉ đích danh 'một tour', '1 chuyến đi', hoặc hỏi về TÊN CỤ THỂ của một tour.\n" +
                        "2.  `list_result`: MẶC ĐỊNH cho các trường hợp còn lại (hỏi chung chung, hỏi về danh sách, hỏi có bộ lọc).\n\n" +
                        "### VÍ DỤ NÂNG CAO:\n" +
                        "- Câu hỏi: 'cho tôi 1 tour hạ long' -> { \"entities\": { \"keywords\": \"tour hạ long\", \"location\": \"hạ long\", ...}, \"action\": \"single_result\" }\n" +
                        "- Câu hỏi: 'các tour ở việt nam' -> { \"entities\": { \"keywords\": \"tour việt nam\", \"location\": \"việt nam\", ...}, \"action\": \"list_result\" }\n" +
                        "- Câu hỏi: 'tour đi biển nào giá tầm 7 triệu không' -> { \"entities\": { \"keywords\": \"tour biển giá tốt\", \"location\": null, \"max_price\": 7000000, \"category\": \"biển\", ... }, \"action\": \"list_result\" }\n" +
                        "- Câu hỏi: 'gợi ý tour cho gia đình đi Đà Nẵng dưới 15 triệu' -> { \"entities\": { \"keywords\": \"tour gia đình Đà Nẵng\", \"location\": \"Đà Nẵng\", \"max_price\": 15000000, \"category\": \"gia đình\", ... }, \"action\": \"list_result\" }\n\n" +
                        "### NGỮ CẢNH HỘI THOẠI:\n%s\n\n" +
                        "### CÂU HỎI MỚI NHẤT:\n\"%s\"\n\n" +
                        "### JSON KẾT QUẢ:\n",
                context.isEmpty() ? "Không có" : context,
                prompt
        );

        GenerateContentResponse response = client.models.generateContent("gemini-1.5-flash-latest", decisionPrompt, null);
        String jsonResponse = extractJson(response.text().trim());
        JsonNode rootNode = objectMapper.readTree(jsonResponse);

        JsonNode entitiesNode = rootNode.path("entities");
        ExtractedEntities entities = new ExtractedEntities();
        entities.setKeywords(entitiesNode.path("keywords").asText(prompt));

        JsonNode locationNode = entitiesNode.path("location");
        if (!locationNode.isMissingNode() && !locationNode.isNull() && locationNode.isTextual()) {
            entities.setLocation(locationNode.asText(null));
        }

        JsonNode priceNode = entitiesNode.path("max_price");
        if (!priceNode.isMissingNode() && !priceNode.isNull() && priceNode.isNumber()) {
            entities.setMaxPrice(priceNode.asLong());
        }

        JsonNode categoryNode = entitiesNode.path("category");
        if (!categoryNode.isMissingNode() && !categoryNode.isNull() && categoryNode.isTextual()) {
            entities.setCategory(categoryNode.asText(null));
        }

        JsonNode durationNode = entitiesNode.path("duration");
        if (!durationNode.isMissingNode() && !durationNode.isNull() && durationNode.isTextual()) {
            entities.setDuration(durationNode.asText(null));
        }

        String action = rootNode.path("action").asText("list_result");
        return Map.of("entities", entities, "action", action);
    }

    /**
     * NÂNG CẤP CUỐI CÙNG: Logic lọc siêu linh hoạt.
     */
    private List<Tour> filterCandidates(List<Tour> candidates, ExtractedEntities entities) {
        return candidates.stream()
                .filter(tour -> {
                    // Location Match (City, Title, Destination Name, Continent)
                    boolean locationMatch = !StringUtils.hasText(entities.getLocation()) ||
                            (tour.getCity() != null && tour.getCity().toLowerCase().contains(entities.getLocation().toLowerCase())) ||
                            (tour.getTitle() != null && tour.getTitle().toLowerCase().contains(entities.getLocation().toLowerCase())) ||
                            (tour.getDestination() != null && tour.getDestination().getName().toLowerCase().contains(entities.getLocation().toLowerCase())) ||
                            (tour.getDestination() != null && tour.getDestination().getContinent().toLowerCase().contains(entities.getLocation().toLowerCase()));

                    // Price Match (Flexible)
                    boolean priceMatch = true;
                    if (entities.getMaxPrice() != null && tour.getPrice() != null) {
                        if (entities.getKeywords().toLowerCase().contains("tầm") || entities.getKeywords().toLowerCase().contains("khoảng")) {
                            long lowerBound = entities.getMaxPrice() - 2000000; // Khoảng giá 4 triệu
                            long upperBound = entities.getMaxPrice() + 2000000;
                            priceMatch = tour.getPrice() >= lowerBound && tour.getPrice() <= upperBound;
                        } else {
                            priceMatch = tour.getPrice() <= entities.getMaxPrice();
                        }
                    }

                    // Category Match (Flexible)
                    boolean categoryMatch = !StringUtils.hasText(entities.getCategory()) ||
                            (tour.getCategory() != null && tour.getCategory().toLowerCase().contains(entities.getCategory().toLowerCase())) ||
                            (tour.getTitle().toLowerCase().contains(entities.getCategory().toLowerCase())) ||
                            (tour.getDescription().toLowerCase().contains(entities.getCategory().toLowerCase()));

                    // Duration Match (Flexible)
                    boolean durationMatch = !StringUtils.hasText(entities.getDuration()) ||
                            (tour.getDuration() != null && tour.getDuration().toLowerCase().contains(entities.getDuration().replace(" ", "").toLowerCase()));

                    return locationMatch && priceMatch && categoryMatch && durationMatch;
                })
                .collect(Collectors.toList());
    }
    // ... (Các hàm tiện ích khác giữ nguyên không đổi)
    private Map<String, Object> handleNoResults(String reason, String location) {
        System.out.println("----------- KẾT THÚC (Lý do: " + reason + ") -----------");
        String message = "Xin lỗi, tôi không tìm thấy tour nào phù hợp với các tiêu chí của bạn.";
        if(StringUtils.hasText(location)){
            message += String.format(" Bạn có muốn xem các tour khác ở %s không?", location);
        }
        return Map.of("success", true, "response", message);
    }
    private Map<String, Object> handleSingleResult(Tour tour) {
        System.out.println("----------- KẾT THÚC (Lý do: Trả về 1 kết quả duy nhất) -----------");
        return formatBasicTourInfo(tour);
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
            String formattedPrice = (tour.getPrice() != null) ? String.format("%,d", tour.getPrice()).replace(',', '.') : "N/A";
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