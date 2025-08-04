package com.example.SpringMongoProject.Controller;

import com.example.SpringMongoProject.Entity.Tour;
import com.example.SpringMongoProject.Service.TourService;
import com.example.SpringMongoProject.dto.TourRequestDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/tours")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5174"})
public class TourController {

    @Autowired
    private TourService tourService;

    // API Lấy danh sách tour (không đổi)
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllTours(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "-createdAt", required = false) String sort,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "1000") int limit,
            @RequestParam(name = "price[lte]", required = false) Double maxPrice,
            @RequestParam(required = false) Boolean featured,
            @RequestParam(required = false) String destination
    ) {
        Page<Tour> tourPage = tourService.findTours(search, sort, page, limit, maxPrice, featured, destination);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "count", tourPage.getNumberOfElements(),
                "total", tourPage.getTotalElements(),
                "data", tourPage.getContent()
        ));
    }

    // API Lấy chi tiết tour (không đổi)
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getTourById(@PathVariable String id) {
        try {
            Tour tour = tourService.findTourById(id);
            return ResponseEntity.ok(Map.of("success", true, "data", tour));
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(Map.of("success", false, "message", "Tour not found"));
        }
    }

    // API để tạo tour mới (Admin)
    @PostMapping
    public ResponseEntity<Map<String, Object>> createTour(@RequestBody TourRequestDTO request) {
        try {
            Tour newTour = mapDtoToEntity(request);
            Tour createdTour = tourService.createTour(newTour);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("success", true, "message", "Tạo tour thành công!", "data", createdTour));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // API để cập nhật tour (Admin)
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateTour(@PathVariable String id, @RequestBody TourRequestDTO request) {
        try {
            Tour tourDetails = mapDtoToEntity(request);
            Tour updatedTour = tourService.updateTour(id, tourDetails);
            return ResponseEntity.ok(Map.of("success", true, "message", "Cập nhật tour thành công!", "data", updatedTour));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // API để xóa tour (Admin)
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteTour(@PathVariable String id) {
        try {
            tourService.deleteTour(id);
            return ResponseEntity.ok(Map.of("success", true, "message", "Xóa tour thành công."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // Hàm tiện ích để chuyển đổi từ DTO sang Entity
    private Tour mapDtoToEntity(TourRequestDTO request) {
        Tour tour = new Tour();
        tour.setTitle(request.getTitle());
        tour.setCity(request.getCity());
        tour.setDescription(request.getDescription());
        tour.setDestinationId(request.getDestinationId());
        tour.setPrice(request.getPrice());
        tour.setDuration(request.getDuration());
        tour.setImage(request.getImage());
        tour.setFeatured(request.getFeatured());
        tour.setImages(request.getImages());
        tour.setStartLocation(request.getStartLocation());
        tour.setEndLocation(request.getEndLocation());
        tour.setIncluded(request.getIncluded());
        tour.setExcluded(request.getExcluded());
        tour.setTags(request.getTags());
        tour.setCategory(request.getCategory());
        tour.setDepartures(request.getDepartures());
        tour.setItinerary(request.getItinerary());
        return tour;
    }
}