package com.example.SpringMongoProject.Service;

import com.example.SpringMongoProject.Entity.User;
import com.example.SpringMongoProject.Repo.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    /**
     * Lấy danh sách tất cả người dùng trong hệ thống.
     * @return List các đối tượng User.
     */
    public List<User> findAllUsers() {
        return userRepository.findAll();
    }

    /**
     * Tìm một người dùng cụ thể bằng ID.
     * @param id ID của người dùng.
     * @return Optional chứa User nếu tìm thấy.
     */
    public Optional<User> findUserById(String id) {
        return userRepository.findById(id);
    }

    /**
     * ✅ HÀM MỚI: Xóa một người dùng khỏi hệ thống.
     * @param userId ID của người dùng cần xóa.
     */
    public void deleteUser(String userId) {
        if (!userRepository.existsById(userId)) {
            throw new RuntimeException("Không tìm thấy người dùng với ID: " + userId);
        }
        // Thêm logic xóa các dữ liệu liên quan (bookings, reviews) ở đây nếu cần
        userRepository.deleteById(userId);
    }

    public User updateUserRole(String userId, String newRole) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với ID: " + userId));

        if (!"admin".equals(newRole) && !"user".equals(newRole)) {
            throw new IllegalArgumentException("Vai trò không hợp lệ: " + newRole);
        }

        user.setRole(newRole);
        return userRepository.save(user);
    }

    public User updateUserSuspension(String userId, boolean isSuspended) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với ID: " + userId));

        user.setSuspended(isSuspended);
        return userRepository.save(user);
    }

    /**
     * ✅ HÀM MỚI: Cập nhật thông tin cá nhân (tên, sđt) của người dùng.
     * @param userId ID của người dùng.
     * @param updatedData Dữ liệu mới cần cập nhật.
     * @return User sau khi đã được cập nhật.
     */
    public User updateProfile(String userId, User updatedData) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với ID: " + userId));

        // Cập nhật tên nếu có
        if (updatedData.getName() != null && !updatedData.getName().isEmpty()) {
            user.setName(updatedData.getName());
        }

        // Cập nhật SĐT nếu có và SĐT đó chưa tồn tại
        if (updatedData.getPhoneNumber() != null && !updatedData.getPhoneNumber().equals(user.getPhoneNumber())) {
            userRepository.findByPhoneNumber(updatedData.getPhoneNumber()).ifPresent(u -> {
                throw new IllegalStateException("Số điện thoại đã tồn tại.");
            });
            user.setPhoneNumber(updatedData.getPhoneNumber());
        }

        // Cập nhật avatar nếu có
        if (updatedData.getAvatar() != null && !updatedData.getAvatar().isEmpty()) {
            user.setAvatar(updatedData.getAvatar());
        }

        return userRepository.save(user);
    }
}