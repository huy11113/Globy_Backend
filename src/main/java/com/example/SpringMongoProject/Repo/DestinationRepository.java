package com.example.SpringMongoProject.Repo;

import com.example.SpringMongoProject.Entity.Destination;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DestinationRepository extends MongoRepository<Destination, String> {
}