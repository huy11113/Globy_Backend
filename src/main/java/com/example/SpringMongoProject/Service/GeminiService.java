package com.example.SpringMongoProject.Service;

import com.example.SpringMongoProject.Entity.Tour;
import com.example.SpringMongoProject.Entity.Destination;
import com.example.SpringMongoProject.Repo.TourRepository;
import com.example.SpringMongoProject.Repo.DestinationRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import com.google.genai.errors.ClientException;
import com.google.genai.types.GenerateContentResponse;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.text.Normalizer;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.Optional;
import java.util.Map;
import java.util.HashMap;

@Service
public class GeminiService {
    private final com.google.genai.Client client;
    private final TourRepository tourRepository;
    private final DestinationRepository destinationRepository;

    private List<Tour> tours;
    private List<Destination> destinations;

    public GeminiService(com.google.genai.Client client, TourRepository tourRepository, DestinationRepository destinationRepository) {
        this.client = client;
        this.tourRepository = tourRepository;
        this.destinationRepository = destinationRepository;
    }

    @PostConstruct
    public void loadDataFromDb() {
        this.tours = tourRepository.findAll();
        this.destinations = destinationRepository.findAll();
        System.out.println("Đã tải thành công " + tours.size() + " tour du lịch từ MongoDB.");
        System.out.println("Đã tải thành công " + destinations.size() + " điểm đến từ MongoDB.");
    }

    public static String normalizeString(String input) {
        if (input == null) return null;
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(normalized).replaceAll("").toLowerCase();
    }

    public Map<String, Object> askGemini(String prompt) {
        try {
            // 1. ✅ Xây dựng "Kiến thức" đầy đủ hơn, bao gồm cả GIÁ TIỀN
            StringBuilder tourKnowledgeBase = new StringBuilder();
            for (Tour tour : this.tours) {
                tourKnowledgeBase.append(String.format("id: \"%s\", title: \"%s\", price: %d, description: \"%s\", tags: \"%s\"\n",
                        tour.getId(),
                        tour.getTitle(),
                        tour.getPrice(), // Cung cấp giá cho AI
                        tour.getDescription(),
                        tour.getTags() != null ? String.join(", ", tour.getTags()) : ""
                ));
            }

            // 2. ✅ Cập nhật Prompt để AI tự xử lý logic về GIÁ
            String geminiPrompt = String.format(
                    "Bạn là một trợ lý tìm kiếm tour thông minh. Dựa vào danh sách tour CÓ SẴN dưới đây và câu hỏi của người dùng, hãy tìm ra các tour phù hợp nhất.\n\n" +
                            "### Nhiệm vụ:\n" +
                            "1. Đọc kỹ câu hỏi của người dùng để hiểu họ muốn tìm tour gì.\n" +
                            "2. **Xử lý yêu cầu về giá:** Nếu người dùng đề cập đến một mức giá hoặc ngân sách (ví dụ: '18 triệu', 'dưới 20 triệu', 'khoảng 15 triệu'), hãy so sánh nó với trường `price` của mỗi tour. Hãy tìm những tour có giá nhỏ hơn hoặc bằng ngân sách đó.\n" +
                            "3. Dựa vào TẤT CẢ tiêu chí (địa điểm, giá, hoạt động...), quét qua 'DANH SÁCH TOUR CÓ SẴN' và tìm tất cả các tour khớp với yêu cầu.\n" +
                            "4. Trả lời bằng một đối tượng JSON DUY NHẤT có cấu trúc sau:\n" +
                            "   { \"found_tour_ids\": [\"id_tour_1\", \"id_tour_2\", ...] }\n" +
                            "   - `found_tour_ids` là một mảng (array) chứa ID của TẤT CẢ các tour bạn tìm thấy.\n" +
                            "   - Nếu không tìm thấy tour nào, trả về mảng rỗng: { \"found_tour_ids\": [] }.\n" +
                            "   - KHÔNG thêm bất kỳ giải thích hay văn bản nào khác ngoài đối tượng JSON này.\n\n" +
                            "### DANH SÁCH TOUR CÓ SẴN (Đơn vị giá là VNĐ):\n" +
                            "%s\n\n" +
                            "### Câu hỏi của người dùng:\n" +
                            "\"%s\"",
                    tourKnowledgeBase.toString(),
                    prompt
            );

            // 3. Gửi yêu cầu đến Gemini
            GenerateContentResponse response = client.models.generateContent(
                    "gemini-1.5-flash-latest",
                    geminiPrompt,
                    null
            );

            String geminiResponseRaw = response.text().trim();
            System.out.println("Phản hồi từ Gemini (thô): " + geminiResponseRaw);

            // 4. Xử lý phản hồi JSON từ Gemini
            String geminiResponseJson = extractJson(geminiResponseRaw);
            if (geminiResponseJson.isEmpty()) {
                return Map.of("success", true, "response", "Xin lỗi, tôi chưa hiểu ý bạn. Bạn có thể diễn đạt khác được không?");
            }

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(geminiResponseJson);

            List<String> foundTourIds = new ArrayList<>();
            if (rootNode.has("found_tour_ids") && rootNode.get("found_tour_ids").isArray()) {
                for (JsonNode idNode : rootNode.get("found_tour_ids")) {
                    foundTourIds.add(idNode.asText());
                }
            }

            // 5. Lấy thông tin tour từ cache và định dạng phản hồi
            List<Tour> filteredTours = this.tours.stream()
                    .filter(tour -> foundTourIds.contains(tour.getId()))
                    .collect(Collectors.toList());

            if (filteredTours.size() == 1) {
                return formatBasicTourInfo(filteredTours.get(0));
            } else if (filteredTours.size() > 1) {
                return formatSimpleTourList(filteredTours);
            } else {
                return Map.of("success", true, "response", "Xin lỗi, tôi không tìm thấy tour nào phù hợp với yêu cầu của bạn.");
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

    // Các hàm format giữ nguyên
    private Map<String, Object> formatBasicTourInfo(Tour tour) {
        Map<String, Object> tourData = new HashMap<>();
        String formattedPrice = String.format("%,d", tour.getPrice()).replace(',', '.');
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
        responseData.put("text", "Tuyệt vời! Tôi đã tìm thấy một vài tour phù hợp với yêu cầu của bạn:");
        responseData.put("tours", tourListForResponse);

        return Map.of("success", true, "response", responseData);
    }
}