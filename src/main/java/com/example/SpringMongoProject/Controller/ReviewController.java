package com.example.SpringMongoProject.Controller;

import com.example.SpringMongoProject.Entity.Review;
import com.example.SpringMongoProject.Entity.Tour;
import com.example.SpringMongoProject.Entity.User;
import com.example.SpringMongoProject.Service.ReviewService;
import com.example.SpringMongoProject.dto.ReviewRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reviews")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5174"})
public class ReviewController {

    @Autowired
    private ReviewService reviewService;

    /**
     * API để lấy tất cả review của một tour
     */
    @GetMapping("/tour/{tourId}")
    public ResponseEntity<Map<String, Object>> getReviewsForTour(@PathVariable String tourId) {
        List<Review> reviews = reviewService.findVisibleReviewsByTourId(tourId);
        return ResponseEntity.ok(Map.of("success", true, "data", reviews));
    }

    /**
     * API để user tạo một review mới
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createReview(@RequestBody ReviewRequest request) {
        try {
            Review newReview = new Review();
            User user = new User();
            user.setId(request.getUserId());
            Tour tour = new Tour();
            tour.setId(request.getTourId());

            newReview.setUser(user);
            newReview.setTour(tour);
            newReview.setRating(request.getRating());
            newReview.setComment(request.getComment());

            Review createdReview = reviewService.createReview(newReview);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("success", true, "message", "Cảm ơn bạn đã đánh giá!", "data", createdReview));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }
    /**
     * ✅ API MỚI: Lấy tất cả review (cho admin)
     */
    @GetMapping("/admin/all")
    public ResponseEntity<Map<String, Object>> getAllReviewsForAdmin() {
        List<Review> reviews = reviewService.findAllReviews();
        return ResponseEntity.ok(Map.of("success", true, "data", reviews));
    }

    /**
     * ✅ API MỚI: Cập nhật trạng thái hiển thị (cho admin)
     */
    @PutMapping("/admin/{reviewId}/visibility")
    public ResponseEntity<Map<String, Object>> setReviewVisibility(@PathVariable String reviewId, @RequestBody Map<String, Boolean> payload) {
        try {
            Review updatedReview = reviewService.updateReviewVisibility(reviewId, payload.get("isVisible"));
            return ResponseEntity.ok(Map.of("success", true, "data", updatedReview));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * ✅ API MỚI: Xóa review (cho admin)
     */
    @DeleteMapping("/admin/{reviewId}")
    public ResponseEntity<Map<String, Object>> deleteReview(@PathVariable String reviewId) {
        try {
            reviewService.deleteReview(reviewId);
            return ResponseEntity.ok(Map.of("success", true, "message", "Xóa review thành công."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}