package com.example.SpringMongoProject.Controller;

import com.example.SpringMongoProject.Entity.Destination;
import com.example.SpringMongoProject.Service.DestinationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/destinations")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5174"})
public class DestinationController {

    @Autowired
    private DestinationService destinationService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllDestinations(@RequestParam(required = false) Integer limit) {
        List<Destination> destinations = destinationService.findDestinations(limit);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "count", destinations.size(),
                "data", destinations
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getDestinationById(@PathVariable String id) {
        try {
            Destination destination = destinationService.findDestinationById(id);
            return ResponseEntity.ok(Map.of("success", true, "data", destination));
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createDestination(@RequestBody Destination destination) {
        try {
            Destination newDestination = destinationService.createDestination(destination);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("success", true, "data", newDestination));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateDestination(@PathVariable String id, @RequestBody Destination destination) {
        try {
            Destination updatedDestination = destinationService.updateDestination(id, destination);
            return ResponseEntity.ok(Map.of("success", true, "data", updatedDestination));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteDestination(@PathVariable String id) {
        try {
            destinationService.deleteDestination(id);
            return ResponseEntity.ok(Map.of("success", true, "message", "Xóa địa điểm thành công."));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("success", false, "message", e.getMessage()));
        }
    }
} // <--- Dấu ngoặc đóng của class, không xóa dấu này.
// ❌ XÓA BỎ DẤU NGOẶC THỪA Ở ĐÂY