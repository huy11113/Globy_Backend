package com.example.SpringMongoProject.Service;

import com.example.SpringMongoProject.Entity.Review;
import com.example.SpringMongoProject.Entity.Tour;
import com.example.SpringMongoProject.Entity.User;
import com.example.SpringMongoProject.Repo.ReviewRepository;
import com.example.SpringMongoProject.Repo.TourRepository;
import com.example.SpringMongoProject.Repo.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReviewService {

    @Autowired
    private ReviewRepository reviewRepository;
    @Autowired
    private TourRepository tourRepository;
    @Autowired
    private UserRepository userRepository;

    /**
     * Lấy tất cả review của một tour
     */
    public List<Review> findReviewsByTourId(String tourId) {
        return reviewRepository.findByTourId(tourId);
    }

    /**
     * Tạo một review mới và cập nhật lại rating cho tour
     */
    @Transactional
    public Review createReview(Review reviewData) {
        User user = userRepository.findById(reviewData.getUser().getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        Tour tour = tourRepository.findById(reviewData.getTour().getId())
                .orElseThrow(() -> new RuntimeException("Tour not found"));

        reviewData.setUser(user);
        reviewData.setTour(tour);
        reviewData.setCreatedAt(LocalDateTime.now()); // ✅ Gán thời gian tạo
        Review savedReview = reviewRepository.save(reviewData);
        // --- Logic cập nhật rating trung bình ---
        List<Review> reviews = reviewRepository.findByTourId(tour.getId());
        double totalRating = reviews.stream().mapToInt(Review::getRating).sum();
        tour.setReviewsCount(reviews.size());
        tour.setRating(totalRating / reviews.size());
        tourRepository.save(tour);
        // -----------------------------------------

        return savedReview;
    }
    /**
     * ✅ HÀM MỚI: Lấy tất cả review cho admin
     */
    public List<Review> findAllReviews() {
        return reviewRepository.findAll();
    }

    /**
     * Lấy tất cả review CÓ THỂ HIỂN THỊ của một tour (cho người dùng xem)
     */
    public List<Review> findVisibleReviewsByTourId(String tourId) {
        return reviewRepository.findByTourIdAndIsVisibleTrue(tourId); // Sửa lại hàm
    }
    /**
     * ✅ HÀM MỚI: Cập nhật trạng thái hiển thị của review
     */
    @Transactional
    public Review updateReviewVisibility(String reviewId, boolean isVisible) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy review với ID: " + reviewId));
        review.setVisible(isVisible);
        return reviewRepository.save(review);
    }

    /**
     * ✅ HÀM MỚI: Xóa một review và cập nhật lại rating
     */
    @Transactional
    public void deleteReview(String reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy review với ID: " + reviewId));

        String tourId = review.getTour().getId();
        reviewRepository.delete(review);

        // Cập nhật lại rating của tour sau khi xóa
        List<Review> remainingReviews = reviewRepository.findByTourId(tourId);
        Tour tour = tourRepository.findById(tourId).orElse(null);
        if (tour != null) {
            if (remainingReviews.isEmpty()) {
                tour.setRating(0.0);
                tour.setReviewsCount(0);
            } else {
                double totalRating = remainingReviews.stream().mapToInt(Review::getRating).sum();
                tour.setRating(totalRating / remainingReviews.size());
                tour.setReviewsCount(remainingReviews.size());
            }
            tourRepository.save(tour);
        }
    }
}