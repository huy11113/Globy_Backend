package com.example.SpringMongoProject.Controller;

import com.example.SpringMongoProject.Entity.Destination;
import com.example.SpringMongoProject.Service.DestinationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/destinations")
@CrossOrigin(origins = "http://localhost:5173")
public class DestinationController {

    @Autowired
    private DestinationService destinationService;

    /**
     * API để lấy danh sách các địa điểm, có thể có tham số limit.
     * Tương ứng với hook useDestinations.js.
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllDestinations(
            @RequestParam(required = false) Integer limit) {
        List<Destination> destinations = destinationService.findDestinations(limit);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "count", destinations.size(),
                "data", destinations
        ));
    }

    /**
     * API để lấy chi tiết một địa điểm.
     * Tương ứng với hook useDestinationDetail.js.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getDestinationById(@PathVariable String id) {
        try {
            Destination destination = destinationService.findDestinationById(id);
            return ResponseEntity.ok(Map.of("success", true, "data", destination));
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}