package com.example.SpringMongoProject.Repo;

import com.example.SpringMongoProject.Entity.Destination;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DestinationRepository extends MongoRepository<Destination, String> {
    // Hiện tại chưa cần phương thức tùy chỉnh, nhưng có thể thêm sau
}