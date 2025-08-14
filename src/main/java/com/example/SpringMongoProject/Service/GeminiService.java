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

            ExtractedEntities entities = extractEntitiesFromPrompt(latestPrompt, context.toString());
            List<Double> queryVector = embeddingService.createEmbedding(entities.getKeywords());
            List<String> similarTourIds = tourVectorCache.findSimilarTourIds(queryVector, 50);

            if (similarTourIds.isEmpty()) {
                return Map.of("success", true, "response", "Xin lỗi, tôi không tìm thấy tour nào phù hợp.");
            }

            List<Tour> candidateTours = tourRepository.findAllById(similarTourIds);
            List<Tour> filteredTours = filterCandidates(candidateTours, entities);

            if (filteredTours.isEmpty()) {
                return Map.of("success", true, "response", "Xin lỗi, tôi không tìm thấy tour nào phù hợp với các tiêu chí của bạn.");
            }
            if (filteredTours.size() == 1) {
                tourService.populateDestinationsForTours(filteredTours);
                return formatBasicTourInfo(filteredTours.get(0));
            }

            return rankAndSelectTours(filteredTours, latestPrompt, entities.getLocation());

        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("success", false, "message", "Đã xảy ra lỗi khi xử lý yêu cầu của bạn: " + e.getMessage());
        }
    }

    private List<Tour> filterCandidates(List<Tour> candidates, ExtractedEntities entities) {
        return candidates.stream()
                .filter(tour -> {
                    boolean priceMatch = entities.getMaxPrice() == null || (tour.getPrice() != null && tour.getPrice() <= entities.getMaxPrice());
                    boolean categoryMatch = !StringUtils.hasText(entities.getCategory()) || (tour.getCategory() != null && tour.getCategory().equalsIgnoreCase(entities.getCategory()));
                    boolean durationMatch = !StringUtils.hasText(entities.getDuration()) || (tour.getDuration() != null && tour.getDuration().contains(entities.getDuration()));
                    return priceMatch && categoryMatch && durationMatch;
                })
                .collect(Collectors.toList());
    }

    private ExtractedEntities extractEntitiesFromPrompt(String prompt, String context) throws Exception {
        String extractionPrompt = String.format(
                "Bạn là một hệ thống phân tích ngôn ngữ du lịch... (Nội dung prompt chi tiết giữ nguyên)",
                context.isEmpty() ? "Không có" : context, prompt);

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

        return entities;
    }

    private Map<String, Object> rankAndSelectTours(List<Tour> candidateTours, String prompt, String primaryLocation) throws Exception {
        StringBuilder tourKnowledgeBase = new StringBuilder();
        candidateTours.forEach(tour -> tourKnowledgeBase.append(String.format("id: \"%s\", title: \"%s\", city: \"%s\"\n",
                tour.getId(), tour.getTitle(), tour.getCity())));

        String rankingPrompt = String.format(
                "Bạn là một chuyên gia tư vấn du lịch... (Nội dung prompt xếp hạng giữ nguyên)",
                tourKnowledgeBase.toString(), primaryLocation != null ? primaryLocation : "Không có", prompt);

        GenerateContentResponse response = client.models.generateContent("gemini-1.5-flash-latest", rankingPrompt, null);
        JsonNode rootNode = objectMapper.readTree(extractJson(response.text().trim()));

        String bestMatchId = rootNode.path("best_match_id").asText(null);
        if (bestMatchId != null && !bestMatchId.isEmpty()) {
            return candidateTours.stream().filter(t -> t.getId().equals(bestMatchId)).findFirst()
                    .map(tour -> {
                        tourService.populateDestinationsForTours(List.of(tour));
                        return formatBasicTourInfo(tour);
                    })
                    .orElseGet(() -> { // Dự phòng nếu ID không hợp lệ
                        tourService.populateDestinationsForTours(candidateTours);
                        return formatSimpleTourList(candidateTours);
                    });
        }

        List<String> rankedTourIds = new ArrayList<>();
        rootNode.path("ranked_tour_ids").forEach(idNode -> rankedTourIds.add(idNode.asText()));
        if (rankedTourIds.isEmpty()) {
            tourService.populateDestinationsForTours(candidateTours);
            return formatSimpleTourList(candidateTours);
        }

        List<Tour> finalTours = rankedTourIds.stream()
                .map(id -> candidateTours.stream().filter(t -> t.getId().equals(id)).findFirst().orElse(null))
                .filter(Objects::nonNull).collect(Collectors.toList());

        tourService.populateDestinationsForTours(finalTours);
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