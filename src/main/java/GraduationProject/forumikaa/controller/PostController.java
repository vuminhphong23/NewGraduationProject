package GraduationProject.forumikaa.controller;

import GraduationProject.forumikaa.dao.UserDao;
import GraduationProject.forumikaa.dto.CreatePostRequest;
import GraduationProject.forumikaa.dto.PostDto;
import GraduationProject.forumikaa.dto.UpdatePostRequest;
import GraduationProject.forumikaa.entity.PostPrivacy;
import GraduationProject.forumikaa.entity.PostStatus;
import GraduationProject.forumikaa.entity.User;
import GraduationProject.forumikaa.service.PostService;
import GraduationProject.forumikaa.util.SecurityUtil;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    @Autowired private PostService postService;
    @Autowired private SecurityUtil securityUtil;

    // 1. Tạo post mới
    @PostMapping
    public ResponseEntity<?> createPost(@Valid @RequestBody CreatePostRequest request) {
        try {
            Long userId = getCurrentUserId();
            PostDto createdPost = postService.createPost(request, userId);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdPost);
        } catch (RuntimeException e) {
            System.err.println("RuntimeException in createPost: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Bad Request", "message", e.getMessage()));
        } catch (Exception e) {
            System.err.println("Exception in createPost: " + e.getMessage());
            e.printStackTrace();
            return error("Failed to create post", e);
        }
    }

    // 2. Lấy feed giống Facebook (toàn bộ)
    @GetMapping("/feed")
    public ResponseEntity<List<PostDto>> getUserFeed() {
        Long userId = getCurrentUserId();
        List<PostDto> posts = postService.getUserFeed(userId);
        return ResponseEntity.ok(posts);
    }

    // 3. Lấy bài viết cá nhân
    @GetMapping("/my-posts")
    public ResponseEntity<List<PostDto>> getMyPosts() {
        Long userId = getCurrentUserId();
        List<PostDto> posts = postService.getUserPosts(userId);
        return ResponseEntity.ok(posts);
    }

    // 4. Lấy bài viết theo topic
    @GetMapping("/topic/{topicId}")
    public ResponseEntity<List<PostDto>> getPostsByTopic(@PathVariable Long topicId) {
        List<PostDto> posts = postService.getPostsByTopic(topicId, getCurrentUserId());
        return ResponseEntity.ok(posts);
    }

    // 5. Lấy post theo ID (có kiểm tra quyền)
    @GetMapping("/{postId}")
    public ResponseEntity<PostDto> getPostById(@PathVariable Long postId) {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(postService.getPostById(postId, userId));
    }

    // 6. Cập nhật bài viết
    @PutMapping("/{postId}")
    public ResponseEntity<PostDto> updatePost(@PathVariable Long postId,
                                              @Valid @RequestBody UpdatePostRequest request) {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(postService.updatePost(postId, request, userId));
    }

    // 7. Xoá bài viết
    @DeleteMapping("/{postId}")
    public ResponseEntity<?> deletePost(@PathVariable Long postId) {
        Long userId = getCurrentUserId();
        postService.deletePost(postId, userId);
        return ResponseEntity.ok(Map.of("message", "Xoá thành công"));
    }

    // 8. Duyệt & từ chối bài viết (admin)
    @PutMapping("/admin/{postId}/approve")
    public ResponseEntity<PostDto> approvePost(@PathVariable Long postId) {
        return ResponseEntity.ok(postService.approvePost(postId));
    }

    @PutMapping("/admin/{postId}/reject")
    public ResponseEntity<PostDto> rejectPost(@PathVariable Long postId,
                                              @RequestParam String reason) {
        return ResponseEntity.ok(postService.rejectPost(postId, reason));
    }

    // 9. Kiểm tra quyền
    @GetMapping("/{postId}/can-access")
    public ResponseEntity<Map<String, Boolean>> canAccess(@PathVariable Long postId) {
        boolean canAccess = postService.canAccessPost(postId, getCurrentUserId());
        return ResponseEntity.ok(Map.of("canAccess", canAccess));
    }

    @GetMapping("/{postId}/can-edit")
    public ResponseEntity<Map<String, Boolean>> canEdit(@PathVariable Long postId) {
        boolean canEdit = postService.canEditPost(postId, getCurrentUserId());
        return ResponseEntity.ok(Map.of("canEdit", canEdit));
    }

    // 10. Privacy options
    @GetMapping("/privacy-options")
    public ResponseEntity<Map<String, String>> getPrivacyOptions() {
        return ResponseEntity.ok(Map.of(
                "PUBLIC", "Công khai",
                "FRIENDS", "Bạn bè",
                "PRIVATE", "Riêng tư"
        ));
    }

    // Utility
    private Long getCurrentUserId() {
        try {
            Long userId = securityUtil.getCurrentUserId();
            if (userId == null) {
                throw new RuntimeException("User ID is null - user may not be authenticated");
            }
            return userId;
        } catch (Exception e) {
            throw new RuntimeException("User not authenticated: " + e.getMessage());
        }
    }

    private ResponseEntity<Map<String, String>> error(String msg, Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", msg, "message", e.getMessage()));
    }
}
