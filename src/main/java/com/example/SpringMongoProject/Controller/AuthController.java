package com.example.SpringMongoProject.Controller;

import com.example.SpringMongoProject.Entity.User;
import com.example.SpringMongoProject.Service.AuthService;
import com.example.SpringMongoProject.Service.JwtService; // Thêm import
import com.example.SpringMongoProject.dto.ForgotPasswordRequest;
import com.example.SpringMongoProject.dto.LoginRequest;
import com.example.SpringMongoProject.dto.RegisterRequest;
import com.example.SpringMongoProject.dto.ResetPasswordRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:5173")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtService jwtService; // Thêm JwtService

    // API Đăng ký: POST /api/auth/register
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody RegisterRequest request) {
        try {
            User newUser = new User();
            newUser.setName(request.getName());
            newUser.setPhoneNumber(request.getPhoneNumber());
            newUser.setEmail(request.getEmail());
            newUser.setPassword(request.getPassword());

            User savedUser = authService.register(newUser);

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "success", true,
                    "message", "Đăng ký thành công!",
                    "data", Map.of("id", savedUser.getId())
            ));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    // API Đăng nhập cho người dùng thường: POST /api/auth/login
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest request) {
        Optional<User> userOpt = authService.login(request.getPhoneNumber(), request.getPassword());
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setPassword(null);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Đăng nhập thành công!",
                    "data", user
            ));
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "success", false,
                    "message", "Số điện thoại hoặc mật khẩu không hợp lệ."
            ));
        }
    }

    // API Đăng nhập cho Admin: POST /api/auth/admin/login
    @PostMapping("/admin/login")
    public ResponseEntity<Map<String, Object>> loginAdmin(@RequestBody LoginRequest request) {
        Optional<User> userOpt = authService.loginAdmin(request.getPhoneNumber(), request.getPassword());

        if (userOpt.isPresent()) {
            User adminUser = userOpt.get();

            // Tạo JWT token cho admin
            String token = jwtService.generateToken(adminUser);

            adminUser.setPassword(null); // Không trả về mật khẩu

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Đăng nhập admin thành công!",
                    "token", token, // Trả token về cho frontend
                    "data", adminUser
            ));
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "success", false,
                    "message", "Thông tin đăng nhập không hợp lệ hoặc bạn không có quyền admin."
            ));
        }
    }

    // API Quên mật khẩu: POST /api/auth/forgot-password
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, Object>> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        try {
            authService.requestPasswordReset(request.getPhoneNumber());
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Mã xác nhận đã được gửi (kiểm tra console backend)."
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    // API Đặt lại mật khẩu: POST /api/auth/reset-password
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, Object>> resetPassword(@RequestBody ResetPasswordRequest request) {
        try {
            authService.resetPassword(request.getPhoneNumber(), request.getResetCode(), request.getNewPassword());
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Mật khẩu đã được đặt lại thành công."
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }
    // --- API MỚI CHO GOOGLE LOGIN ---

    @PostMapping("/google/login")
    public ResponseEntity<Map<String, Object>> loginWithGoogle(@RequestBody Map<String, String> payload) {
        try {
            String googleToken = payload.get("token");
            if (googleToken == null || googleToken.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Google token is required."));
            }

            User user = authService.loginOrRegisterWithGoogle(googleToken);

            // Tạo token của ứng dụng Globy (JWT) cho user
            String appToken = jwtService.generateToken(user);
            user.setPassword(null); // Không bao giờ trả về mật khẩu

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Đăng nhập bằng Google thành công!",
                    "token", appToken,
                    "data", user
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }
}