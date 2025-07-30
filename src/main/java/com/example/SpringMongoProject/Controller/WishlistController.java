package com.example.SpringMongoProject.Controller;

import com.example.SpringMongoProject.Entity.Tour;
import com.example.SpringMongoProject.Service.WishlistService;
import com.example.SpringMongoProject.dto.ToggleWishlistRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/wishlist")
@CrossOrigin(origins = "http://localhost:5173")
public class WishlistController {

    @Autowired
    private WishlistService wishlistService;

    // Lấy danh sách yêu thích: GET /api/wishlist/:userId
    @GetMapping("/{userId}")
    public ResponseEntity<Map<String, Object>> getWishlist(@PathVariable String userId) {
        List<Tour> wishlist = wishlistService.getWishlist(userId);
        return ResponseEntity.ok(Map.of("success", true, "data", wishlist));
    }

    // Thêm/xóa khỏi danh sách yêu thích: POST /api/wishlist/toggle
    @PostMapping("/toggle")
    public ResponseEntity<Map<String, Object>> toggleWishlist(@RequestBody ToggleWishlistRequest request) {
        List<Tour> updatedWishlist = wishlistService.toggleWishlist(request.getUserId(), request.getTourId());
        return ResponseEntity.ok(Map.of("success", true, "data", updatedWishlist));
    }
}