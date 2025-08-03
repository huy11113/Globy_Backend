package com.example.SpringMongoProject.Service;

import com.example.SpringMongoProject.Entity.Hotel;
import com.example.SpringMongoProject.Repo.HotelRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HotelService {

    @Autowired
    private HotelRepository hotelRepository;

    /**
     * Tìm các khách sạn dựa trên tên thành phố.
     * @param city Tên thành phố.
     * @return Danh sách các khách sạn.
     */
    public List<Hotel> findHotelsByCity(String city) {
        if (city == null || city.isEmpty()) {
            return List.of(); // Trả về danh sách rỗng nếu không có tên thành phố
        }
        return hotelRepository.findByCity(city);
    }
}