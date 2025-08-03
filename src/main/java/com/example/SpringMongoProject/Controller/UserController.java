package com.example.SpringMongoProject.Controller;

import com.example.SpringMongoProject.Entity.User;
import com.example.SpringMongoProject.Service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "http://localhost:5173")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * API để lấy danh sách tất cả người dùng.
     * Tương ứng với `GET /api/users` trong server.js.
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllUsers() {
        List<User> users = userService.findAllUsers();
        // Ẩn mật khẩu của tất cả user trước khi trả về
        users.forEach(user -> user.setPassword(null));

        return ResponseEntity.ok(Map.of(
                "success", true,
                "count", users.size(),
                "data", users
        ));
    }

    /**
     * API để lấy thông tin chi tiết của một người dùng.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getUserById(@PathVariable String id) {
        Optional<User> userOpt = userService.findUserById(id);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setPassword(null); // Ẩn mật khẩu
            return ResponseEntity.ok(Map.of("success", true, "data", user));
        } else {
            return ResponseEntity.status(404).body(Map.of("success", false, "message", "User not found"));
        }
    }
}