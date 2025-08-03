package com.example.SpringMongoProject.Repo;

import com.example.SpringMongoProject.Entity.Hotel;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface HotelRepository extends MongoRepository<Hotel, String> {

    /**
     * Tự động tìm tất cả các khách sạn có trường 'city' khớp với chuỗi được cung cấp.
     * @param city Tên thành phố.
     * @return Danh sách các khách sạn.
     */
    List<Hotel> findByCity(String city);
}