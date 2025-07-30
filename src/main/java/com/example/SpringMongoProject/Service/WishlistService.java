package com.example.SpringMongoProject.Service;

import com.example.SpringMongoProject.Entity.Tour;
import com.example.SpringMongoProject.Entity.User;
import com.example.SpringMongoProject.Repo.TourRepository;
import com.example.SpringMongoProject.Repo.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
public class WishlistService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TourRepository tourRepository;

    public List<Tour> toggleWishlist(String userId, String tourId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng."));
        // Không cần tìm tour vì chúng ta chỉ cần ID để thêm/xóa tham chiếu

        if (user.getWishlist() == null) {
            user.setWishlist(new ArrayList<>());
        }

        // Tìm tour trong danh sách wishlist hiện tại của user
        Tour tourInWishlist = user.getWishlist().stream()
                .filter(t -> t.getId().equals(tourId))
                .findFirst()
                .orElse(null);

        if (tourInWishlist != null) {
            // Nếu có rồi thì xóa đi
            user.getWishlist().remove(tourInWishlist);
        } else {
            // Nếu chưa có, tìm tour trong DB và thêm vào
            Tour tourToAdd = tourRepository.findById(tourId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy tour."));
            user.getWishlist().add(tourToAdd);
        }

        userRepository.save(user);
        return user.getWishlist();
    }

    public List<Tour> getWishlist(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng."));

        if (user.getWishlist() == null) {
            return List.of(); // Trả về danh sách rỗng an toàn
        }

        return user.getWishlist();
    }
}