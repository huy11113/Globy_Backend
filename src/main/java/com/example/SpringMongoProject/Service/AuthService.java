package com.example.SpringMongoProject.Service;

import com.example.SpringMongoProject.Entity.User;
import com.example.SpringMongoProject.Repo.UserRepository;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import java.util.Collections;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;
    @Value("${google.client.id}") // Lấy giá trị từ application.properties
    private String googleClientId;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * Xử lý logic đăng ký người dùng mới.
     */
    public User register(User registrationData) {
        userRepository.findByPhoneNumber(registrationData.getPhoneNumber()).ifPresent(u -> {
            throw new IllegalStateException("Số điện thoại đã tồn tại.");
        });

        User newUser = new User();
        newUser.setName(registrationData.getName());
        newUser.setPhoneNumber(registrationData.getPhoneNumber());
        newUser.setEmail(registrationData.getEmail());
        newUser.setPassword(passwordEncoder.encode(registrationData.getPassword()));

        return userRepository.save(newUser);
    }

    /**
     * Xử lý logic đăng nhập cho người dùng thường.
     */
    public Optional<User> login(String phoneNumber, String password) {
        Optional<User> userOpt = userRepository.findByPhoneNumber(phoneNumber);

        if (userOpt.isPresent() && passwordEncoder.matches(password, userOpt.get().getPassword())) {
            return userOpt;
        }
        return Optional.empty();
    }

    /**
     * Xử lý logic đăng nhập cho Admin, kiểm tra cả mật khẩu và vai trò.
     */
    public Optional<User> loginAdmin(String phoneNumber, String password) {
        Optional<User> userOpt = userRepository.findByPhoneNumber(phoneNumber);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            // Điều kiện quan trọng: mật khẩu phải khớp VÀ vai trò phải là "admin"
            if (passwordEncoder.matches(password, user.getPassword()) && "admin".equalsIgnoreCase(user.getRole())) {
                return Optional.of(user); // Đăng nhập admin thành công
            }
        }
        return Optional.empty(); // Sai thông tin hoặc không có quyền admin
    }

    /**
     * Gửi yêu cầu đặt lại mật khẩu.
     */
    public void requestPasswordReset(String phoneNumber) {
        User user = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new RuntimeException("Số điện thoại không tồn tại."));

        String resetCode = String.format("%06d", new Random().nextInt(999999));
        user.setResetCode(resetCode);
        userRepository.save(user);

        System.out.println("Mã xác nhận cho " + phoneNumber + " là: " + resetCode);
    }

    /**
     * Hoàn tất việc đặt lại mật khẩu mới.
     */
    public boolean resetPassword(String phoneNumber, String resetCode, String newPassword) {
        User user = userRepository.findByPhoneNumberAndResetCode(phoneNumber, resetCode)
                .orElseThrow(() -> new RuntimeException("Số điện thoại hoặc mã xác nhận không đúng."));

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetCode(null);
        userRepository.save(user);
        return true;
    }
    // --- PHƯƠI THỨC MỚI CHO GOOGLE LOGIN ---

    /**
     * Xác thực Google ID Token, sau đó đăng nhập hoặc đăng ký user mới.
     * @param googleTokenString Google ID Token nhận được từ frontend.
     * @return User tương ứng trong database.
     */
    public User loginOrRegisterWithGoogle(String googleTokenString) {
        try {
            // Khởi tạo bộ xác thực
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            // Xác thực token
            GoogleIdToken idToken = verifier.verify(googleTokenString);
            if (idToken == null) {
                throw new IllegalArgumentException("Token Google không hợp lệ.");
            }

            GoogleIdToken.Payload payload = idToken.getPayload();
            String email = payload.getEmail();
            String name = (String) payload.get("name");
            String avatar = (String) payload.get("picture");

            // Tìm user bằng email, nếu không có thì tạo mới
            return userRepository.findByEmail(email).orElseGet(() -> {
                User newUser = new User();
                newUser.setEmail(email);
                newUser.setName(name);
                newUser.setAvatar(avatar);
                newUser.setRole("user");
                // Tạo một mật khẩu ngẫu nhiên mạnh vì user này sẽ không dùng đến nó
                newUser.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
                return userRepository.save(newUser);
            });
        } catch (Exception e) {
            throw new RuntimeException("Xác thực Google thất bại: " + e.getMessage(), e);
        }
    }
}