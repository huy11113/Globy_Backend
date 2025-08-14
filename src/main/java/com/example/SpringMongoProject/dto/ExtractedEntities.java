// File: src/main/java/com/example/SpringMongoProject/dto/ExtractedEntities.java

package com.example.SpringMongoProject.dto;

import lombok.Data;

@Data
public class ExtractedEntities {
    private String keywords;
    private Long maxPrice;
    private Long minPrice;
    private String category;
    private String duration;
    private String location;
}