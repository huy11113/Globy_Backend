package com.example.SpringMongoProject.Entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.example.SpringMongoProject.Service.GeminiService;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Data
@Document(collection = "tours")
public class Tour {

    @Id
    @JsonProperty("_id")
    private String id;
    private String title;
    private String city;
    private String description;

    @Field("destinationId")
    private String destinationId;

    @Transient
    private Destination destination;

    private Long price;
    private String duration;
    private String image;
    private Double rating = 0.0;
    private Integer reviewsCount = 0;
    private Boolean featured;

    private List<String> images;
    private String startLocation;
    private String endLocation;
    private List<String> included;
    private List<String> excluded;
    private List<String> tags;
    private String category;
    private List<Departure> departures;
    private List<ItineraryItem> itinerary;

    // ✅ PHIÊN BẢN GỌN HƠN: Đã loại bỏ tham số `budget`
    public boolean matches(String destination, String duration, List<String> tags, String continent, List<Destination> destinationsList) {
        boolean destinationMatch = true;
        if (destination != null && !destination.isEmpty()) {
            final String normalizedDestination = GeminiService.normalizeString(destination);
            Optional<Destination> matchingDest = destinationsList.stream()
                    .filter(d -> d.getId().equals(this.destinationId))
                    .findFirst();

            boolean nameInTitle = GeminiService.normalizeString(this.title).contains(normalizedDestination);
            boolean nameInCountry = matchingDest.isPresent() &&
                    GeminiService.normalizeString(matchingDest.get().getName()).contains(normalizedDestination);
            destinationMatch = nameInTitle || nameInCountry;
        }

        boolean durationMatch = true;
        if (duration != null && !duration.isEmpty()) {
            String tourDurationNumber = this.duration.replaceAll("[^\\d.]", "");
            durationMatch = tourDurationNumber.equals(duration);
        }

        boolean tagsMatch = true;
        if (tags != null && !tags.isEmpty()) {
            Stream<String> searchCorpusStream = Stream.of(
                    this.description != null ? this.description : "",
                    this.title != null ? this.title : "",
                    this.tags != null ? String.join(" ", this.tags) : "",
                    this.included != null ? String.join(" ", this.included) : "",
                    this.itinerary != null ? this.itinerary.stream().map(i -> i.getTitle() + " " + i.getDetails()).collect(Collectors.joining(" ")) : ""
            );

            List<String> normalizedCorpus = searchCorpusStream
                    .map(GeminiService::normalizeString)
                    .collect(Collectors.toList());

            tagsMatch = tags.stream().anyMatch(inputTag -> {
                String normalizedInputTag = GeminiService.normalizeString(inputTag);
                return normalizedCorpus.stream().anyMatch(corpusText -> corpusText.contains(normalizedInputTag));
            });
        }

        boolean continentMatch = true;
        if (continent != null && !continent.isEmpty()) {
            final String normalizedContinent = GeminiService.normalizeString(continent);
            Optional<Destination> matchingDest = destinationsList.stream()
                    .filter(d -> d.getId().equals(this.destinationId))
                    .findFirst();

            continentMatch = matchingDest.isPresent() &&
                    GeminiService.normalizeString(matchingDest.get().getContinent()).contains(normalizedContinent);
        }

        // Logic so sánh budget đã được chuyển cho AI, nên ở đây chỉ cần trả về các điều kiện còn lại
        return destinationMatch && durationMatch && tagsMatch && continentMatch;
    }

    @Data
    public static class ItineraryItem {
        private Integer day;
        private String title;
        private String details;
    }

    @Data
    public static class Departure {
        private Date date;
        private Integer seatsAvailable;
    }
}