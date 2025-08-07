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
}