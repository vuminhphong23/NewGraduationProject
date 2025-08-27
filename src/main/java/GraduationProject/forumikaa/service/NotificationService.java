package GraduationProject.forumikaa.service;

import GraduationProject.forumikaa.entity.Notification;
import GraduationProject.forumikaa.entity.User;

import java.util.List;
import java.util.Map;

public interface NotificationService {
    // Các method cơ bản
    Notification createNotification(Long recipientId, Long senderId, String message, String link);
    Notification createNotification(Long recipientId, Long senderId, String message);
    List<Notification> getUserNotifications(Long userId);
    Long getUnreadCount(Long userId);
    void markAsRead(Long notificationId);
    void markAllAsRead(Long userId);
    List<Map<String, Object>> getNotificationDtos(Long userId);
    
    // Các method tạo thông báo cụ thể
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
}

