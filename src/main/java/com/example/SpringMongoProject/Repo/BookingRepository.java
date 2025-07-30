package com.example.SpringMongoProject.Repo;

import com.example.SpringMongoProject.Entity.Booking;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface BookingRepository extends MongoRepository<Booking, String> {
    // Tìm các booking của một người dùng cụ thể
    List<Booking> findByUserId(String userId);
}