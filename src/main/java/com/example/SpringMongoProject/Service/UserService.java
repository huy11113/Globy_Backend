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
}