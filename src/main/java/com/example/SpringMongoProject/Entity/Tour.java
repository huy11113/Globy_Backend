package com.example.SpringMongoProject.Entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Date;
import java.util.List;

@Data
@Document(collection = "tours")
public class Tour {

    // Class con cho các mục trong lịch trình
    @Data
    public static class ItineraryItem {
        private Integer day;
        private String title;
        private String details;
    }

    // Class con cho các ngày khởi hành
    @Data
    public static class Departure {
        private Date date;
        private Integer seatsAvailable;
    }

    // --- BẮT ĐẦU CÁC TRƯỜNG CỦA TOUR ---

    @Id
    @JsonProperty("_id") // Đổi tên 'id' thành '_id' khi chuyển sang JSON
    private String id;

    private String title;
    private String description;
    @DBRef
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
}