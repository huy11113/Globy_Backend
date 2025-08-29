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

    @GetMapping
    public ResponseEntity<Map<String, Object>> getHotels(
            @RequestParam(required = false) String city) {
        List<Hotel> hotels = hotelService.findHotelsByCity(city);
        return ResponseEntity.ok(Map.of("success", true, "data", hotels));
    }
}