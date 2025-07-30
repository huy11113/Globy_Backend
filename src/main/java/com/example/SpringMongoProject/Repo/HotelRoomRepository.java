package com.example.SpringMongoProject.Repo;

import com.example.SpringMongoProject.Entity.HotelRoom;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HotelRoomRepository extends MongoRepository<HotelRoom, String> {
}