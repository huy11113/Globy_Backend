// File: src/main/java/com/example/SpringMongoProject/Service/DataInitializer.java
package com.example.SpringMongoProject.Service;

import com.example.SpringMongoProject.Entity.Tour;
import com.example.SpringMongoProject.Repo.TourRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DataInitializer {

    @Autowired
    private TourRepository tourRepository;
    @Autowired
    private EmbeddingService embeddingService;

    @PostConstruct
    public void run() throws Exception {
        System.out.println("Bắt đầu quá trình tạo và lưu embedding...");
        List<Tour> allTours = tourRepository.findAll();

        for (Tour tour : allTours) {
            if (tour.getTourEmbedding() == null || tour.getTourEmbedding().isEmpty()) {
                String contentToEmbed = String.join(". ",
                        tour.getTitle(),
                        tour.getDescription(),
                        "Thành phố: " + tour.getCity()
                );
                try {
                    List<Double> embedding = embeddingService.createEmbedding(contentToEmbed);
                    tour.setTourEmbedding(embedding);
                    tourRepository.save(tour);
                    System.out.println("Đã tạo embedding cho tour: " + tour.getTitle());
                } catch (Exception e) {
                    System.err.println("Lỗi khi tạo embedding cho tour " + tour.getId() + ": " + e.getMessage());
                }
            }
        }
        System.out.println("Hoàn tất quá trình tạo embedding!");
    }
}