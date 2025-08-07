package com.example.SpringMongoProject.Service;

import com.example.SpringMongoProject.Entity.Tour;
import com.example.SpringMongoProject.Entity.Destination;
import com.example.SpringMongoProject.Repo.TourRepository;
import com.example.SpringMongoProject.Repo.DestinationRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import com.google.genai.errors.ClientException;
import com.google.genai.types.GenerateContentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
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
            String geminiPrompt = String.format(
                    "Bạn là một trợ lý chatbot du lịch. Hãy phân tích câu hỏi của người dùng và trích xuất các thông tin sau thành một đối tượng JSON.\n" +
                            "- intent (ý định): Ví dụ: \"find_tour\", \"find_destination\" (tìm điểm đến), \"greeting\".\n" +
                            "- entities (thực thể): Một đối tượng chứa các thông tin cụ thể.\n" +
                            "    - destination (điểm đến): Tên địa điểm du lịch (ví dụ: \"Sapa\", \"Hạ Long\", \"Vietnam\").\n" +
                            "    - duration (thời lượng): Thời gian du lịch (ví dụ: \"4\"). Chỉ trả về số.\n" +
                            "    - tags (thẻ): Các từ khóa mô tả tour (ví dụ: \"trekking\", \"adventure\", \"beach\"). Hãy trả về bằng tiếng Anh để khớp với dữ liệu gốc.\n" +
                            "    - budget (ngân sách): Mức giá tối đa. Chỉ trả về số.\n" +
                            "    - continent (châu lục): Tên châu lục (ví dụ: \"Europe\", \"Asia\").\n\n" +
                            "Nếu không tìm thấy thông tin nào, hãy để giá trị null. Chỉ trả về đối tượng JSON, không thêm bất kỳ văn bản nào khác. Câu trả lời của bạn nên được bao bọc trong một cặp dấu ngoặc nhọn {}.\n\n" +
                            "Câu hỏi của người dùng:\n\"%s\"",
                    prompt
            );

            GenerateContentResponse response = client.models.generateContent(
                    "gemini-2.5-flash",
                    geminiPrompt,
                    null
            );

            String geminiResponseRaw = response.text().trim();
            System.out.println("Phản hồi từ Gemini (thô): " + geminiResponseRaw);

            String geminiResponseJson = "";
            int startIndex = geminiResponseRaw.indexOf('{');
            int endIndex = geminiResponseRaw.lastIndexOf('}');

            if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
                geminiResponseJson = geminiResponseRaw.substring(startIndex, endIndex + 1);
            } else {
                return Map.of("success", false, "message", "Xin lỗi, Gemini không thể tạo phản hồi JSON hợp lệ cho yêu cầu này. Phản hồi thô: " + geminiResponseRaw);
            }

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(geminiResponseJson);

            String intent = rootNode.path("intent").asText();

            if ("find_tour".equals(intent)) {
                JsonNode entities = rootNode.path("entities");
                String destination = entities.path("destination").asText(null);
                String duration = entities.path("duration").asText(null);
                String continent = entities.path("continent").asText(null);

                List<String> tags = new ArrayList<>();
                if (entities.has("tags") && entities.path("tags").isArray()) {
                    for (JsonNode tagNode : entities.path("tags")) {
                        tags.add(tagNode.asText());
                    }
                }

                Double budget = null;
                if (entities.has("budget") && (entities.path("budget").isTextual() || entities.path("budget").isNumber())) {
                    try {
                        budget = entities.path("budget").asDouble();
                    } catch (Exception e) { }
                }

                final String finalDestination = destination;
                final String finalDuration = duration;
                final Double finalBudget = budget;
                final String finalContinent = continent;

                List<Tour> filteredTours = tourRepository.findAll().stream()
                        .filter(tour -> tour.matches(finalDestination, finalDuration, finalBudget, tags, finalContinent, destinationRepository.findAll()))
                        .collect(Collectors.toList());

                if (filteredTours.size() == 1) {
                    return formatBasicTourInfo(filteredTours.get(0));
                } else if (filteredTours.size() > 1) {
                    return formatSimpleTourList(filteredTours);
                } else {
                    return Map.of("success", true, "response", "Xin lỗi, tôi không tìm thấy tour nào phù hợp với yêu cầu của bạn.");
                }
            } else if ("find_destination".equals(intent)) {
                String destinationName = rootNode.path("entities").path("destination").asText(null);
                String continentName = rootNode.path("entities").path("continent").asText(null);

                if (destinationName != null) {
                    return findDestinationInfoByName(destinationName);
                } else if (continentName != null) {
                    return findDestinationInfoByContinent(continentName);
                }
                return Map.of("success", true, "response", "Bạn muốn tìm hiểu về điểm đến nào?");
            } else if ("greeting".equals(intent)) {
                return Map.of("success", true, "response", "Chào bạn! Tôi có thể giúp gì cho bạn? Bạn muốn tìm tour nào?");
            }

            return Map.of("success", true, "response", "Xin lỗi, tôi không hiểu yêu cầu của bạn. Bạn có thể thử tìm kiếm tour hoặc hỏi về một điểm đến.");

        } catch (ClientException e) {
            e.printStackTrace();
            return Map.of("success", false, "message", "Đã xảy ra lỗi API với Gemini: " + e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
            return Map.of("success", false, "message", "Đã xảy ra lỗi khi xử lý yêu cầu của bạn.");
        }
    }

    private Map<String, Object> findDestinationInfoByName(String name) {
        for (Destination dest : destinations) {
            if (normalizeString(dest.getName()).contains(normalizeString(name))) {
                return Map.of("success", true, "response", formatDestinationInfo(dest));
            }
        }
        return Map.of("success", true, "response", "Xin lỗi, tôi không tìm thấy thông tin chi tiết về điểm đến này.");
    }

    private Map<String, Object> findDestinationInfoByContinent(String continent) {
        StringBuilder sb = new StringBuilder();
        sb.append("Các điểm đến ở ");
        sb.append(continent);
        sb.append(":\n");
        boolean found = false;
        for (Destination dest : destinations) {
            if (normalizeString(dest.getContinent()).contains(normalizeString(continent))) {
                sb.append(String.format("- %s\n", dest.getName()));
                found = true;
            }
        }
        if (found) {
            return Map.of("success", true, "response", sb.toString());
        }
        return Map.of("success", true, "response", "Xin lỗi, tôi không tìm thấy thông tin chi tiết về điểm đến này.");
    }

    private String formatDestinationInfo(Destination dest) {
        return String.format("Thông tin về %s:\n%s\nChâu lục: %s",
                dest.getName(), dest.getDescription(), dest.getContinent());
    }

    private Map<String, Object> formatBasicTourInfo(Tour tour) {
        Map<String, Object> tourData = new HashMap<>();
        tourData.put("text", String.format("Tìm thấy tour: %s. Giá: $%s. Thời lượng: %s. Mô tả: %s.",
                tour.getTitle(), tour.getPrice(), tour.getDuration(), tour.getDescription()));
        tourData.put("image", tour.getImage());
        tourData.put("link", "/tours/" + tour.getId());
        return Map.of("success", true, "response", tourData);
    }

    private Map<String, Object> formatSimpleTourList(List<Tour> tours) {
        if (tours.isEmpty()) {
            return Map.of("success", true, "response", "Xin lỗi, tôi không tìm thấy tour nào phù hợp với yêu cầu của bạn.");
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Đây là danh sách các tour phù hợp:\n");
        for (Tour tour : tours) {
            sb.append(String.format("- %s (Giá: $%.2f, Thời lượng: %s)\n",
                    tour.getTitle(), tour.getPrice(), tour.getDuration()));
        }
        return Map.of("success", true, "response", sb.toString());
    }
}