package GraduationProject.forumikaa.controller;

import GraduationProject.forumikaa.dto.PostRequest;
import GraduationProject.forumikaa.dto.PostResponse;
import GraduationProject.forumikaa.service.PostService;
import GraduationProject.forumikaa.util.SecurityUtil;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import GraduationProject.forumikaa.entity.Topic;
import GraduationProject.forumikaa.service.TopicService;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    @Autowired private PostService postService;
    @Autowired private SecurityUtil securityUtil;
    @Autowired private TopicService topicService;

    // 1. Tạo post mới
    @PostMapping
    public ResponseEntity<?> createPost(@Valid @RequestBody PostRequest request) {
        try {
            Long userId = getCurrentUserId();
            PostResponse createdPost = postService.createPost(request, userId);
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

    // 2. Lấy toàn bộ
    @GetMapping("/feed")
    public ResponseEntity<List<PostResponse>> getUserFeed() {
        Long userId = getCurrentUserId();
        List<PostResponse> posts = postService.getUserFeed(userId);
        return ResponseEntity.ok(posts);
    }

    // 3. Lấy bài viết cá nhân
    @GetMapping("/my-posts")
    public ResponseEntity<List<PostResponse>> getMyPosts() {
        Long userId = getCurrentUserId();
        List<PostResponse> posts = postService.getUserPosts(userId);
        return ResponseEntity.ok(posts);
    }

    // 4. Lấy bài viết theo topic
    @GetMapping("/topic/{topicId}")
    public ResponseEntity<List<PostResponse>> getPostsByTopic(@PathVariable Long topicId) {
        List<PostResponse> posts = postService.getPostsByTopic(topicId, getCurrentUserId());
        return ResponseEntity.ok(posts);
    }

    // 5. Lấy bài viết theo group
    @GetMapping("/group/{groupId}")
    public ResponseEntity<List<PostResponse>> getPostsByGroup(@PathVariable Long groupId) {
        List<PostResponse> posts = postService.getPostsByGroup(groupId, getCurrentUserId());
        return ResponseEntity.ok(posts);
    }

    // 5. Lấy post theo ID (có kiểm tra quyền)
    @GetMapping("/{postId}")
    public ResponseEntity<PostResponse> getPostById(@PathVariable Long postId) {
        try {
            Long userId = getCurrentUserId();
            System.out.println("📄 Getting post " + postId + " for user " + userId);
            
            PostResponse post = postService.getPostById(postId, userId);
            System.out.println("✅ Post retrieved successfully: " + post.getTitle());
            
            return ResponseEntity.ok(post);
        } catch (Exception e) {
            System.err.println("❌ Error getting post: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    // 6. Cập nhật bài viết
    @PutMapping("/{postId}")
    public ResponseEntity<PostResponse> updatePost(@PathVariable Long postId,
                                                   @Valid @RequestBody PostRequest request) {
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
    public ResponseEntity<PostResponse> approvePost(@PathVariable Long postId) {
        return ResponseEntity.ok(postService.approvePost(postId));
    }

    @PutMapping("/admin/{postId}/reject")
    public ResponseEntity<PostResponse> rejectPost(@PathVariable Long postId,
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

    // 11. Lấy trending topics để gợi ý hashtag
    @GetMapping("/trending-topics")
    public ResponseEntity<List<Map<String, Object>>> getTrendingTopics() {
        try {
            List<Topic> trendingTopics = topicService.getTopTopics(10);
            List<Map<String, Object>> topicData = trendingTopics.stream()
                    .map(topic -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("id", topic.getId());
                        map.put("name", topic.getName());
                        map.put("usageCount", topic.getUsageCount());
                        return map;
                    })
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(topicData);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(List.of());
        }
    }

    // 12. Like/Unlike bài viết
    @PostMapping("/{postId}/like")
    public ResponseEntity<Map<String, Object>> toggleLike(@PathVariable Long postId) {
        try {
            Long userId = getCurrentUserId();
            boolean isLiked = postService.toggleLike(postId, userId);
            Long likeCount = postService.getPostLikeCount(postId);

            return ResponseEntity.ok(Map.of(
                "isLiked", isLiked,
                "likeCount", likeCount,
                "message", isLiked ? "Đã thích bài viết" : "Đã bỏ thích bài viết"
            ));
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            Long likeCount = postService.getPostLikeCount(postId);
            return ResponseEntity.ok(Map.of(
                "isLiked", true,
                "likeCount", likeCount,
                "message", "Đã thích bài viết"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Không thể thích bài viết", "message", e.getMessage()));
        }
    }

    // 14b. Cập nhật comment
    @PutMapping("/{postId}/comments/{commentId}")
    public ResponseEntity<Map<String, Object>> updateComment(@PathVariable Long postId,
                                                             @PathVariable Long commentId,
                                                             @RequestBody Map<String, String> request) {
        Long userId = getCurrentUserId();
        String content = request.get("content");
        Map<String, Object> updated = postService.updateComment(postId, commentId, userId, content);
        return ResponseEntity.ok(Map.of("comment", updated));
    }

    // 13. Lấy danh sách comment của bài viết
    @GetMapping("/{postId}/comments")
    public ResponseEntity<List<Map<String, Object>>> getPostComments(@PathVariable Long postId,
                                                                   @RequestParam(defaultValue = "0") int page,
                                                                   @RequestParam(defaultValue = "10") int size) {
        try {
            Long userId = getCurrentUserId();
            List<Map<String, Object>> comments = postService.getPostComments(postId, userId, page, size);
            return ResponseEntity.ok(comments);
        } catch (Exception e) {
            System.err.println("PostController: Error getting comments for post " + postId + ": " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(List.of());
        }
    }

    // 14. Thêm comment mới
    @PostMapping("/{postId}/comments")
    public ResponseEntity<Map<String, Object>> addComment(@PathVariable Long postId,
                                                        @RequestBody Map<String, String> request) {
        try {
            Long userId = getCurrentUserId();
            String content = request.get("content");
            
            if (content == null || content.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Nội dung comment không được để trống"));
            }
            
            Map<String, Object> comment = postService.addComment(postId, userId, content.trim());
            Long commentCount = postService.getPostCommentCount(postId);
            
            return ResponseEntity.ok(Map.of(
                "comment", comment,
                "commentCount", commentCount,
                "message", "Đã thêm bình luận thành công"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Không thể thêm bình luận", "message", e.getMessage()));
        }
    }

    // 15. Xóa comment
    @DeleteMapping("/{postId}/comments/{commentId}")
    public ResponseEntity<Map<String, Object>> deleteComment(@PathVariable Long postId,
                                                           @PathVariable Long commentId) {
        try {
            Long userId = getCurrentUserId();
            postService.deleteComment(postId, commentId, userId);
            Long commentCount = postService.getPostCommentCount(postId);
            
            return ResponseEntity.ok(Map.of(
                "commentCount", commentCount,
                "message", "Đã xóa bình luận thành công"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Không thể xóa bình luận", "message", e.getMessage()));
        }
    }

    // 16. Share bài viết
    @PostMapping("/{postId}/share")
    public ResponseEntity<Map<String, Object>> sharePost(@PathVariable Long postId,
                                                       @RequestBody(required = false) Map<String, String> request) {
        try {
            Long userId = getCurrentUserId();
            String message = request != null ? request.get("message") : null;
            String privacy = request != null ? request.get("privacy") : "PUBLIC";
            
            System.out.println("🔗 Sharing post " + postId + " by user " + userId + " with message: " + message + ", privacy: " + privacy);
            
            Map<String, Object> sharedPost = postService.sharePost(postId, userId, message, privacy);
            Long shareCount = postService.getPostShareCount(postId);
            
            System.out.println("✅ Post shared successfully, share count: " + shareCount);
            
            return ResponseEntity.ok(Map.of(
                "sharedPost", sharedPost,
                "shareCount", shareCount,
                "message", "Đã chia sẻ bài viết thành công"
            ));
        } catch (Exception e) {
            System.err.println("❌ Error sharing post: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Không thể chia sẻ bài viết", "message", e.getMessage()));
        }
    }

    // 17. Kiểm tra trạng thái like của user
    @GetMapping("/{postId}/like-status")
    public ResponseEntity<Map<String, Object>> getLikeStatus(@PathVariable Long postId) {
        try {
            Long userId = getCurrentUserId();
            boolean isLiked = postService.isPostLikedByUser(postId, userId);
            Long likeCount = postService.getPostLikeCount(postId);
            
            return ResponseEntity.ok(Map.of(
                "isLiked", isLiked,
                "likeCount", likeCount
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Không thể kiểm tra trạng thái like", "message", e.getMessage()));
        }
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
