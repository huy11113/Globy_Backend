package com.example.SpringMongoProject.Repo;

import com.example.SpringMongoProject.Entity.Review;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ReviewRepository extends MongoRepository<Review, String> {
    // Tìm tất cả review của một tour cụ thể
    List<Review> findByTourId(String tourId);
}