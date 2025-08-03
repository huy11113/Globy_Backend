package com.example.SpringMongoProject.Controller;

import com.example.SpringMongoProject.Entity.Tour;
import com.example.SpringMongoProject.Service.TourService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/tours")
@CrossOrigin(origins = "http://localhost:5173")
public class TourController {

    @Autowired
    private TourService tourService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllTours(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "-createdAt", required = false) String sort,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "9") int limit,
            @RequestParam(name = "price[lte]", required = false) Double maxPrice,
            @RequestParam(required = false) Boolean featured,
            @RequestParam(required = false) String destination
    ) {
        Page<Tour> tourPage = tourService.findTours(search, sort, page, limit, maxPrice, featured, destination);

        Map<String, Object> response = Map.of(
                "success", true,
                "count", tourPage.getNumberOfElements(),
                "total", tourPage.getTotalElements(),
                "data", tourPage.getContent()
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getTourById(@PathVariable String id) {
        try {
            Tour tour = tourService.findTourById(id);
            return ResponseEntity.ok(Map.of("success", true, "data", tour));
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(Map.of("success", false, "message", "Tour not found"));
        }
    }
}