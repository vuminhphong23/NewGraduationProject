package GraduationProject.forumikaa.controller;

import GraduationProject.forumikaa.dao.CommentDao;
import GraduationProject.forumikaa.dao.NotificationDao;
import GraduationProject.forumikaa.dto.NotificationRequest;
import GraduationProject.forumikaa.entity.Notification;
import GraduationProject.forumikaa.service.NotificationService;
import GraduationProject.forumikaa.util.SecurityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired private NotificationService notificationService;
    @Autowired private SecurityUtil securityUtil;
    @Autowired private NotificationDao notificationDao;
    @Autowired private CommentDao commentDao;

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getUserNotifications() {
        Long userId = securityUtil.getCurrentUserId();
        return ResponseEntity.ok(notificationService.getNotificationDtos(userId));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount() {
        Long userId = securityUtil.getCurrentUserId();
        Long count = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(Map.of("count", count));
    }

    @PostMapping("/{notificationId}/read")
    public ResponseEntity<Map<String, String>> markAsRead(@PathVariable Long notificationId) {
        notificationService.markAsRead(notificationId);
        return ResponseEntity.ok(Map.of("message", "Đã đánh dấu đã đọc"));
    }

    @PostMapping("/mark-all-read")
    public ResponseEntity<Map<String, String>> markAllAsRead() {
        Long userId = securityUtil.getCurrentUserId();
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(Map.of("message", "Đã đánh dấu tất cả đã đọc"));
    }

    @PostMapping("/send")
    public Notification send(@RequestBody NotificationRequest req) {
        return notificationService.createNotification(
                req.getSenderId(),
                req.getRecipientId(),
                req.getMessage()
        );
    }

    @PostMapping("/test")
    public ResponseEntity<Map<String, String>> testNotification() {
        Long userId = securityUtil.getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "User not authenticated"));
        }
        
        try {
            // Tạo notification test
            notificationService.createNotification(
                userId, // sender
                userId, // recipient (gửi cho chính mình để test)
                "🔔 Đây là thông báo test WebSocket realtime! Thời gian: " + java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"))
            );
            
            return ResponseEntity.ok(Map.of("message", "Test notification đã được gửi!"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Lỗi: " + e.getMessage()));
        }
    }

    // Test endpoints cho các loại thông báo mới
    @PostMapping("/test/post-like")
    public ResponseEntity<Map<String, String>> testPostLikeNotification() {
        Long userId = securityUtil.getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "User not authenticated"));
        }
        
        try {
            notificationService.createPostLikeNotification(1L, userId, userId);
            return ResponseEntity.ok(Map.of("message", "Test post like notification đã được gửi!"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Lỗi: " + e.getMessage()));
        }
    }

    @PostMapping("/test/post-comment")
    public ResponseEntity<Map<String, String>> testPostCommentNotification() {
        Long userId = securityUtil.getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "User not authenticated"));
        }
        
        try {
            notificationService.createPostCommentNotification(1L, userId, userId, 1L);
            return ResponseEntity.ok(Map.of("message", "Test post comment notification đã được gửi!"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Lỗi: " + e.getMessage()));
        }
    }

    @PostMapping("/test/comment-like")
    public ResponseEntity<Map<String, String>> testCommentLikeNotification() {
        Long userId = securityUtil.getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "User not authenticated"));
        }
        
        try {
            notificationService.createCommentLikeNotification(1L, userId, userId);
            return ResponseEntity.ok(Map.of("message", "Test comment like notification đã được gửi!"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Lỗi: " + e.getMessage()));
        }
    }

    @PostMapping("/test/comment-reply")
    public ResponseEntity<Map<String, String>> testCommentReplyNotification() {
        Long userId = securityUtil.getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "User not authenticated"));
        }
        
        try {
            notificationService.createCommentReplyNotification(1L, userId, userId, 2L);
            return ResponseEntity.ok(Map.of("message", "Test comment reply notification đã được gửi!"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Lỗi: " + e.getMessage()));
        }
    }

    @PostMapping("/test/friendship-request")
    public ResponseEntity<Map<String, String>> testFriendshipRequestNotification() {
        Long userId = securityUtil.getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "User not authenticated"));
        }
        
        try {
            notificationService.createFriendshipRequestNotification(userId, userId);
            return ResponseEntity.ok(Map.of("message", "Test friendship request notification đã được gửi!"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Lỗi: " + e.getMessage()));
        }
    }

    @PostMapping("/test/mention")
    public ResponseEntity<Map<String, String>> testMentionNotification() {
        Long userId = securityUtil.getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "User not authenticated"));
        }
        
        try {
            notificationService.createMentionNotification(userId, userId, 1L, "POST");
            return ResponseEntity.ok(Map.of("message", "Test mention notification đã được gửi!"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Lỗi: " + e.getMessage()));
        }
    }

    @PostMapping("/test/system")
    public ResponseEntity<Map<String, String>> testSystemNotification() {
        Long userId = securityUtil.getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "User not authenticated"));
        }
        
        try {
            notificationService.createSystemNotification(userId, "🔧 Thông báo hệ thống: Cập nhật mới đã có sẵn!");
            return ResponseEntity.ok(Map.of("message", "Test system notification đã được gửi!"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Lỗi: " + e.getMessage()));
        }
    }

    @PostMapping("/test/welcome")
    public ResponseEntity<Map<String, String>> testWelcomeNotification() {
        Long userId = securityUtil.getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "User not authenticated"));
        }
        
        try {
            notificationService.createWelcomeNotification(userId);
            return ResponseEntity.ok(Map.of("message", "Test welcome notification đã được gửi!"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Lỗi: " + e.getMessage()));
        }
    }
    
    @PostMapping("/fix-links")
    public ResponseEntity<Map<String, Object>> fixNotificationLinks() {
        try {
            // Fix notification links for comment notifications
            List<Notification> commentNotifications = notificationDao.findByTypeIn(
                Arrays.asList(Notification.NotificationType.COMMENT_LIKE, Notification.NotificationType.COMMENT_REPLY)
            );
            
            int fixedCount = 0;
            for (Notification notification : commentNotifications) {
                if (notification.getLink() != null && notification.getLink().startsWith("/comments/")) {
                    // Extract comment ID from old link
                    String commentIdStr = notification.getLink().replace("/comments/", "");
                    try {
                        Long commentId = Long.parseLong(commentIdStr);
                        // Get post ID from comment
                        Optional<Long> postIdOpt = commentDao.findPostIdByCommentId(commentId);
                        if (postIdOpt.isPresent()) {
                            String newLink = "/posts/" + postIdOpt.get() + "#comment-" + commentId;
                            notification.setLink(newLink);
                            notificationDao.save(notification);
                            fixedCount++;
                        }
                    } catch (NumberFormatException e) {
                        // Skip invalid comment IDs
                    }
                }
            }
            
            return ResponseEntity.ok(Map.of(
                "message", "Fixed " + fixedCount + " notification links",
                "fixedCount", fixedCount
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Lỗi: " + e.getMessage()));
        }
    }
}

