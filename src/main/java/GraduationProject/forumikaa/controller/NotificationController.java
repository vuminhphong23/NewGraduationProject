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
import java.util.HashMap;
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
        try {
            System.out.println("🔄 NotificationController: markAsRead(" + notificationId + ") được gọi");
            
            // Kiểm tra xem notification có tồn tại không
            if (notificationId == null || notificationId <= 0) {
                return ResponseEntity.badRequest().body(Map.of("error", "Notification ID không hợp lệ"));
            }
            
            notificationService.markAsRead(notificationId);
            
            System.out.println("✅ NotificationController: markAsRead(" + notificationId + ") thành công");
            return ResponseEntity.ok(Map.of("message", "Đã đánh dấu đã đọc"));
            
        } catch (Exception e) {
            System.err.println("❌ NotificationController: Lỗi trong markAsRead(" + notificationId + "): " + e.getMessage());
            e.printStackTrace();
            
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Lỗi khi đánh dấu đã đọc: " + e.getMessage(),
                "notificationId", notificationId.toString()
            ));
        }
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
    
    // Test endpoint để kiểm tra trạng thái của NotificationService
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getNotificationServiceStatus() {
        try {
            Map<String, Object> status = new HashMap<>();
            
            // Kiểm tra xem notificationService có được inject không
            status.put("notificationServiceInjected", notificationService != null);
            status.put("notificationServiceClass", notificationService != null ? notificationService.getClass().getName() : "null");
            
            // Nếu là NotificationServiceWrapper, kiểm tra trạng thái singleton
            if (notificationService instanceof GraduationProject.forumikaa.patterns.adapter.NotificationServiceWrapper) {
                GraduationProject.forumikaa.patterns.adapter.NotificationServiceWrapper wrapper = 
                    (GraduationProject.forumikaa.patterns.adapter.NotificationServiceWrapper) notificationService;
                
                status.put("singletonInitialized", wrapper.isSingletonInitialized());
                status.put("singletonClass", wrapper.getSingletonInstance().getClass().getName());
            }
            
            // Test một số method cơ bản
            try {
                Long unreadCount = notificationService.getUnreadCount(1L);
                status.put("getUnreadCountWorking", true);
                status.put("unreadCountForUser1", unreadCount);
            } catch (Exception e) {
                status.put("getUnreadCountWorking", false);
                status.put("getUnreadCountError", e.getMessage());
            }
            
            return ResponseEntity.ok(status);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Lỗi khi kiểm tra trạng thái: " + e.getMessage()
            ));
        }
    }
}

