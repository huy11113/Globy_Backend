package com.example.SpringMongoProject.Entity;

import com.fasterxml.jackson.annotation.JsonProperty; // 1. THÊM IMPORT NÀY
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "destinations")
public class Destination {
    @Id
    @JsonProperty("_id") // 2. THÊM DÒNG NÀY
    private String id;

    private String name;
    private String description;
    private String image;
    private String continent;
}