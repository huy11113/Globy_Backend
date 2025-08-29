package com.example.SpringMongoProject.Entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Date;
import java.util.List;

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

    @Field("tour_embedding")
    private List<Double> tourEmbedding;

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