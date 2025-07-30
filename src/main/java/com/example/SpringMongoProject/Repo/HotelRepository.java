package com.example.SpringMongoProject.Repo;

import com.example.SpringMongoProject.Entity.Hotel;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HotelRepository extends MongoRepository<Hotel, String> {
    // Có thể thêm tìm kiếm khách sạn theo địa điểm
    // List<Hotel> findByDestinationId(String destinationId);
}