package GraduationProject.forumikaa.controller.user;

import GraduationProject.forumikaa.entity.Comment;
import GraduationProject.forumikaa.service.CommentService;
import GraduationProject.forumikaa.util.SecurityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/comments")
public class CommentController {

    @Autowired
    private CommentService commentService;

    @Autowired
    private SecurityUtil securityUtil;

    @PostMapping("/{commentId}/like")
    public ResponseEntity<Map<String, Object>> toggleCommentLike(@PathVariable Long commentId) {
        try {
            Long userId = securityUtil.getCurrentUserId();
            boolean isLiked = commentService.toggleLike(commentId, userId);
            int likeCount = commentService.getCommentLikeCount(commentId);

            Map<String, Object> response = new HashMap<>();
            response.put("isLiked", isLiked);
            response.put("likeCount", likeCount);
            response.put("message", isLiked ? "Đã thích bình luận" : "Đã bỏ thích bình luận");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/{commentId}/like-status")
    public ResponseEntity<Map<String, Object>> getCommentLikeStatus(@PathVariable Long commentId) {
        try {
            Long userId = securityUtil.getCurrentUserId();
            boolean isLiked = commentService.isCommentLikedByUser(commentId, userId);
            int likeCount = commentService.getCommentLikeCount(commentId);

            Map<String, Object> response = new HashMap<>();
            response.put("isLiked", isLiked);
            response.put("likeCount", likeCount);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PutMapping("/{commentId}")
    public ResponseEntity<Map<String, Object>> updateComment(
            @PathVariable Long commentId,
            @RequestBody Map<String, String> request) {
        try {
            Long userId = securityUtil.getCurrentUserId();
            String content = request.get("content");
            
            if (content == null || content.trim().isEmpty()) {
                throw new IllegalArgumentException("Nội dung không được để trống");
            }

            Comment updatedComment = commentService.updateComment(commentId, userId, content.trim());
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Đã cập nhật bình luận thành công");
            response.put("comment", updatedComment);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}
