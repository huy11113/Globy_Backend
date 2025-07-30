package com.example.SpringMongoProject.Repo;

import com.example.SpringMongoProject.Entity.Tour;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TourRepository extends MongoRepository<Tour, String> {
    List<Tour> findByFeatured(boolean featured);
    List<Tour> findByCategory(String category);
}