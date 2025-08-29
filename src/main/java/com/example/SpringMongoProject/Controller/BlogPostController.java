package com.example.SpringMongoProject.Controller;

import com.example.SpringMongoProject.Entity.BlogPost;
import com.example.SpringMongoProject.Service.BlogPostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/blog")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5174"})
public class BlogPostController {

    @Autowired
    private BlogPostService blogPostService;

    // Lấy tất cả bài viết
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllPosts() {
        List<BlogPost> posts = blogPostService.findAllPosts();
        return ResponseEntity.ok(Map.of("success", true, "data", posts));
    }

    // Lấy chi tiết một bài viết
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getPostById(@PathVariable String id) {
        try {
            BlogPost post = blogPostService.findPostById(id);
            return ResponseEntity.ok(Map.of("success", true, "data", post));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // Tạo bài viết mới (Admin)
    @PostMapping
    public ResponseEntity<Map<String, Object>> createPost(@RequestBody BlogPost post) {
        BlogPost newPost = blogPostService.createPost(post);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("success", true, "data", newPost));
    }

    // Cập nhật bài viết (Admin)
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updatePost(@PathVariable String id, @RequestBody BlogPost post) {
        try {
            BlogPost updatedPost = blogPostService.updatePost(id, post);
            return ResponseEntity.ok(Map.of("success", true, "data", updatedPost));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // Xóa bài viết (Admin)
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deletePost(@PathVariable String id) {
        try {
            blogPostService.deletePost(id);
            return ResponseEntity.ok(Map.of("success", true, "message", "Xóa bài viết thành công."));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}