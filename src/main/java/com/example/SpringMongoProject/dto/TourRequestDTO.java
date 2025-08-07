package com.example.SpringMongoProject.dto;

import com.example.SpringMongoProject.Entity.Tour;
import lombok.Data;
import java.util.List;

@Data
public class TourRequestDTO {
    private String title;
    private String city;
    private String description;
    private String destinationId;

    // ✅ THAY ĐỔI: Chuyển từ Double sang Long
    private Long price;

    private String duration;
    private String image;
    private Boolean featured;
    private List<String> images;
    private String startLocation;
    private String endLocation;
    private List<String> included;
    private List<String> excluded;
    private List<String> tags;
    private String category;
    private List<Tour.Departure> departures;
    private List<Tour.ItineraryItem> itinerary;
}