package com.example.SpringMongoProject.Entity;

import com.example.SpringMongoProject.Service.GeminiService;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import java.util.Date;
import java.util.List;
import java.util.Optional; // Thêm import này

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

    private Double price;
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

    // Phương thức matches() sẽ được thêm vào đây
    public boolean matches(String destination, String duration, Double budget, List<String> tags, String continent, List<Destination> destinationsList) {
        boolean destinationMatch = true;
        if (destination != null) {
            Optional<Destination> matchingDest = destinationsList.stream()
                    .filter(d -> d.getId().equals(this.destinationId))
                    .findFirst();

            boolean nameInTitle = GeminiService.normalizeString(this.title).contains(GeminiService.normalizeString(destination));
            boolean nameInCountry = matchingDest.isPresent() &&
                    GeminiService.normalizeString(matchingDest.get().getName()).contains(GeminiService.normalizeString(destination));
            destinationMatch = nameInTitle || nameInCountry;
        }

        boolean durationMatch = true;
        if (duration != null) {
            String tourDurationNumber = this.duration.replaceAll("[^\\d.]", "");
            durationMatch = tourDurationNumber.equals(duration);
        }

        boolean budgetMatch = budget == null || this.price <= budget;

        boolean tagsMatch = tags.isEmpty() || tags.stream()
                .anyMatch(inputTag -> this.tags.stream()
                        .anyMatch(tourTag -> GeminiService.normalizeString(tourTag).contains(GeminiService.normalizeString(inputTag))));

        boolean continentMatch = true;
        if (continent != null) {
            Optional<Destination> matchingDest = destinationsList.stream()
                    .filter(d -> d.getId().equals(this.destinationId))
                    .findFirst();

            continentMatch = matchingDest.isPresent() &&
                    GeminiService.normalizeString(matchingDest.get().getContinent()).contains(GeminiService.normalizeString(continent));
        }

        return destinationMatch && durationMatch && budgetMatch && tagsMatch && continentMatch;
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