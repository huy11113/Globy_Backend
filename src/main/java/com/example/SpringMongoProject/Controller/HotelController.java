package com.example.SpringMongoProject.Controller;

import com.example.SpringMongoProject.Entity.Hotel;
import com.example.SpringMongoProject.Service.HotelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/hotels")
@CrossOrigin(origins = "http://localhost:5173")
public class HotelController {

    @Autowired
    private HotelService hotelService;

    /**
     * API để lấy danh sách khách sạn.
     * Có thể lọc theo 'city' bằng cách truyền vào như một query parameter.
     * Ví dụ: GET /api/hotels?city=Hạ Long
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getHotels(
            @RequestParam(required = false) String city) { // <-- SỬA Ở ĐÂY

        List<Hotel> hotels = hotelService.findHotelsByCity(city);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "count", hotels.size(),
                "data", hotels
        ));
    }
}