package com.example.SpringMongoProject.Entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.List;

@Data
@Document(collection = "hotel_rooms")
public class HotelRoom {
    @Id
    private String id;
    @DBRef
    private Hotel hotel;
    private String type;
    private String description;
    private Double pricePerNight;
    private Integer capacity;
    private List<String> images;
}