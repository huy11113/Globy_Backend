package com.example.SpringMongoProject.Service;

import com.example.SpringMongoProject.Entity.BlogPost;
import com.example.SpringMongoProject.Repo.BlogPostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class BlogPostService {

    @Autowired
    private BlogPostRepository blogPostRepository;

    public List<BlogPost> findAllPosts() {
        return blogPostRepository.findAll();
    }

    public BlogPost findPostById(String id) {
        return blogPostRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài viết với ID: " + id));
    }

    public BlogPost createPost(BlogPost post) {
        return blogPostRepository.save(post);
    }

    public BlogPost updatePost(String id, BlogPost postDetails) {
        BlogPost post = findPostById(id);
        post.setTitle(postDetails.getTitle());
        post.setCategory(postDetails.getCategory());
        post.setImageUrl(postDetails.getImageUrl());
        post.setExcerpt(postDetails.getExcerpt());
        post.setContent(postDetails.getContent());
        post.setAuthor(postDetails.getAuthor());
        return blogPostRepository.save(post);
    }

    public void deletePost(String id) {
        if (!blogPostRepository.existsById(id)) {
            throw new RuntimeException("Không tìm thấy bài viết với ID: " + id);
        }
        blogPostRepository.deleteById(id);
    }
}