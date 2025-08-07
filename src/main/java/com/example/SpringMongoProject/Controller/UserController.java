package com.example.SpringMongoProject.Controller;

import com.example.SpringMongoProject.Entity.User;
import com.example.SpringMongoProject.Service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
// ✅ Sửa CrossOrigin để cho phép cả trang admin truy cập
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5174"})
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * API để lấy danh sách tất cả người dùng.
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllUsers() {
        List<User> users = userService.findAllUsers();
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
            user.setPassword(null);
            return ResponseEntity.ok(Map.of("success", true, "data", user));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("success", false, "message", "User not found"));
        }
    }

    /**
     * ✅ API MỚI: Cập nhật vai trò của người dùng (chỉ dành cho Admin).
     */
    @PutMapping("/{id}/role")
    public ResponseEntity<Map<String, Object>> updateUserRole(@PathVariable String id, @RequestBody Map<String, String> payload) {
        try {
            String newRole = payload.get("role");
            User updatedUser = userService.updateUserRole(id, newRole);
            updatedUser.setPassword(null); // Ẩn mật khẩu
            return ResponseEntity.ok(Map.of("success", true, "message", "Cập nhật vai trò thành công!", "data", updatedUser));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * ✅ API MỚI: Xóa người dùng (chỉ dành cho Admin).
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteUser(@PathVariable String id) {
        try {
            userService.deleteUser(id);
            return ResponseEntity.ok(Map.of("success", true, "message", "Xóa người dùng thành công."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}