package com.example.SpringMongoProject.Repo;

import com.example.SpringMongoProject.Entity.HotelBooking;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HotelBookingRepository extends MongoRepository<HotelBooking, String> {
}