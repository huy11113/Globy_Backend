package com.example.SpringMongoProject.Entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "hotels")
public class Hotel {
    @Id
    private String id;
    private String name;
    @DBRef
    private Destination destination;
    private String address;
    private String description;
    private String image;
    private Double rating;
    private Integer reviewsCount;
    private Boolean featured;
}