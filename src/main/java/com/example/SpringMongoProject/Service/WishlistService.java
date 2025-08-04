package com.example.SpringMongoProject.Service;

import com.example.SpringMongoProject.Entity.Destination;
import com.example.SpringMongoProject.Entity.Tour;
import com.example.SpringMongoProject.Entity.User;
import com.example.SpringMongoProject.Repo.DestinationRepository;
import com.example.SpringMongoProject.Repo.TourRepository;
import com.example.SpringMongoProject.Repo.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class WishlistService {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TourRepository tourRepository;
    @Autowired
    private DestinationRepository destinationRepository;

    public List<Tour> toggleWishlist(String userId, String tourId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng."));
        Tour tour = tourRepository.findById(tourId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tour."));

        if (user.getWishlist() == null) {
            user.setWishlist(new ArrayList<>());
        }

        // Vì wishlist giờ chỉ lưu DBRef, ta phải so sánh qua ID
        boolean isPresent = user.getWishlist().stream().anyMatch(t -> t.getId().equals(tourId));

        if (isPresent) {
            user.getWishlist().removeIf(t -> t.getId().equals(tourId));
        } else {
            user.getWishlist().add(tour);
        }

        userRepository.save(user);
        return getWishlist(userId);
    }

    public List<Tour> getWishlist(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng."));

        if (user.getWishlist() == null || user.getWishlist().isEmpty()) {
            return Collections.emptyList();
        }

        List<String> tourIds = user.getWishlist().stream()
                .map(Tour::getId)
                .collect(Collectors.toList());

        List<Tour> wishlistTours = tourRepository.findAllById(tourIds);

        // Tái sử dụng logic làm đầy dữ liệu
        populateDestinationsForTours(wishlistTours);

        return wishlistTours;
    }

    // Hàm tiện ích để làm đầy thông tin destination
    private void populateDestinationsForTours(List<Tour> tours) {
        if (tours == null || tours.isEmpty()) return;

        List<String> destIds = tours.stream()
                .map(Tour::getDestinationId)
                .filter(id -> id != null && !id.isEmpty())
                .distinct()
                .collect(Collectors.toList());

        if (destIds.isEmpty()) return;

        List<Destination> destinations = destinationRepository.findAllById(destIds);
        Map<String, Destination> destMap = destinations.stream()
                .collect(Collectors.toMap(Destination::getId, dest -> dest));

        tours.forEach(tour -> {
            if (tour.getDestinationId() != null) {
                tour.setDestination(destMap.get(tour.getDestinationId()));
            }
        });
    }
}