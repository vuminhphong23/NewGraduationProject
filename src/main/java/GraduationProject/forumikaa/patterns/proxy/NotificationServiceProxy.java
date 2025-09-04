package GraduationProject.forumikaa.patterns.proxy;

import GraduationProject.forumikaa.entity.Notification;

import java.util.List;
import java.util.Map;

/**
 * NotificationServiceProxy interface - Proxy Pattern
 * Định nghĩa contract cho proxy của NotificationService
 * Proxy sẽ kiểm soát quyền truy cập và thêm logic bảo mật
 */
public interface NotificationServiceProxy {
    
    // Core notification methods - EXACTLY matching NotificationService interface
    Notification createNotification(Long recipientId, Long senderId, String message, String link);
    Notification createNotification(Long recipientId, Long senderId, String message);
    List<Notification> getUserNotifications(Long userId);
    Long getUnreadCount(Long userId);
    void markAsRead(Long notificationId);
    void markAllAsRead(Long userId);
    List<Map<String, Object>> getNotificationDtos(Long userId);
    
    // Specific notification creation methods - EXACTLY matching NotificationService interface
    Notification createPostLikeNotification(Long postId, Long postAuthorId, Long likerId);
    Notification createPostCommentNotification(Long postId, Long postAuthorId, Long commenterId, Long commentId);
    Notification createCommentLikeNotification(Long commentId, Long commentAuthorId, Long likerId);
    Notification createCommentReplyNotification(Long parentCommentId, Long parentCommentAuthorId, Long replierId, Long replyId);
    Notification createFriendshipRequestNotification(Long recipientId, Long senderId);
    Notification createFriendshipAcceptedNotification(Long recipientId, Long senderId);
    Notification createFriendshipRejectedNotification(Long recipientId, Long senderId);
    Notification createFriendshipCancelledNotification(Long recipientId, Long senderId);
    Notification createMentionNotification(Long mentionedUserId, Long mentionerId, Long entityId, String entityType);
    Notification createSystemNotification(Long userId, String message);
    Notification createWelcomeNotification(Long userId);
    
    // Proxy-specific methods
    boolean isUserAuthorized(Long userId, String operation);
    void logAccessAttempt(Long userId, String operation, boolean authorized);
    Map<String, Object> getAccessLogs();
    void clearAccessLogs();
}
