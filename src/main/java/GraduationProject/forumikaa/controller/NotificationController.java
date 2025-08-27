package GraduationProject.forumikaa.controller;

import GraduationProject.forumikaa.dto.NotificationRequest;
import GraduationProject.forumikaa.entity.Notification;
import GraduationProject.forumikaa.service.NotificationService;
import GraduationProject.forumikaa.util.SecurityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired private NotificationService notificationService;
    @Autowired private SecurityUtil securityUtil;

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
}

