package com.example.SpringMongoProject.Service;

import com.example.SpringMongoProject.Entity.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class JwtService {

    // Đây là một khóa bí mật. TRONG DỰ ÁN THỰC TẾ, NÓ PHẢI ĐƯỢC LƯU TRONG FILE CẤU HÌNH
    // Khóa này phải đủ dài để đảm bảo an toàn.
    private static final String SECRET_KEY = "day-la-mot-cai-khoa-bi-mat-rat-la-dai-va-an-toan-cho-ung-dung-globy";

    /**
     * Tạo ra một JWT token cho người dùng.
     * @param user Đối tượng người dùng (admin)
     * @return Chuỗi JWT token
     */
    public String generateToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        // Thêm các thông tin cần thiết vào token
        claims.put("userId", user.getId());
        claims.put("role", user.getRole());

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(user.getPhoneNumber())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 10)) // Token hết hạn sau 10 giờ
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    private Key getSigningKey() {
        byte[] keyBytes = SECRET_KEY.getBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // Các phương thức để xác thực token sẽ được thêm vào sau khi cần
}