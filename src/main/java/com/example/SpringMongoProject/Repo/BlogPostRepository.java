package com.example.SpringMongoProject.Repo;

import com.example.SpringMongoProject.Entity.BlogPost;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BlogPostRepository extends MongoRepository<BlogPost, String> {
    // Các phương thức tùy chỉnh có thể thêm ở đây nếu cần
}