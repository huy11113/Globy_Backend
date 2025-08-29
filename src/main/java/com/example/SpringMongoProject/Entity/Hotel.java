package com.example.SpringMongoProject.Entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "hotels")
public class Hotel {

    @Id
    @JsonProperty("_id")
    private String id;

    private String name;
    private String city; // <-- THÊM TRƯỜNG NÀY
    @DBRef
    private Destination destination;
    private String address;
    private String description;
    private String image;
    private Double rating;
    private Integer reviewsCount;
    private Boolean featured;
}