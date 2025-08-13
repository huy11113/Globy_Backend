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
        userRepository.deleteById(userId);
    }

    /**
     * ✅ HÀM MỚI: Cập nhật vai trò (role) của người dùng.
     * @param userId ID của người dùng.
     * @param newRole Vai trò mới ('admin' hoặc 'user').
     * @return User sau khi đã được cập nhật.
     */
    public User updateUserRole(String userId, String newRole) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với ID: " + userId));

        // Chỉ cho phép cập nhật thành 'admin' hoặc 'user' để bảo mật
        if (!"admin".equals(newRole) && !"user".equals(newRole)) {
            throw new IllegalArgumentException("Vai trò không hợp lệ: " + newRole);
        }

        user.setRole(newRole);
        return userRepository.save(user);
    }
    /**
     * ✅ HÀM MỚI: Cập nhật trạng thái khóa (suspend) của người dùng.
     * @param userId ID của người dùng.
     * @param isSuspended Trạng thái mới (true: khóa, false: mở khóa).
     * @return User sau khi đã được cập nhật.
     */
    public User updateUserSuspension(String userId, boolean isSuspended) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với ID: " + userId));

        user.setSuspended(isSuspended);
        return userRepository.save(user);
    }
}