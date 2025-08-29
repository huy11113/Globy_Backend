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

    public List<Hotel> findHotelsByCity(String city) {
        if (city == null || city.isEmpty()) {
            return List.of();
        }
        return hotelRepository.findByCity(city);
    }
}