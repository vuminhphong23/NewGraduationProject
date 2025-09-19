package GraduationProject.forumikaa.controller.user;

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
        try {
            System.out.println("🔄 NotificationController: markAsRead(" + notificationId + ") được gọi");
            
            // Kiểm tra xem notification có tồn tại không
            if (notificationId == null || notificationId <= 0) {
                return ResponseEntity.badRequest().body(Map.of("error", "Notification ID không hợp lệ"));
            }
            
            notificationService.markAsRead(notificationId);
            
            return ResponseEntity.ok(Map.of("message", "Đã đánh dấu đã đọc"));
            
        } catch (Exception e) {
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

