package GraduationProject.forumikaa.patterns.decorator;

import GraduationProject.forumikaa.entity.Notification;
import java.util.List;
import java.util.Map;

/**
 * Base Decorator - Abstract class cung cấp implementation mặc định
 * Các concrete decorator sẽ kế thừa từ class này
 */
public abstract class BaseNotificationServiceDecorator implements NotificationServiceComponent {
    
    protected final NotificationServiceComponent wrappedService;
    
    public BaseNotificationServiceDecorator(NotificationServiceComponent wrappedService) {
        this.wrappedService = wrappedService;
    }
    
    // Default implementation - delegate to wrapped service
    @Override
    public Notification createNotification(Long recipientId, Long senderId, String message, String link) {
        return wrappedService.createNotification(recipientId, senderId, message, link);
    }
    
    @Override
    public Notification createNotification(Long recipientId, Long senderId, String message) {
        return wrappedService.createNotification(recipientId, senderId, message);
    }
    
    @Override
    public List<Notification> getUserNotifications(Long userId) {
        return wrappedService.getUserNotifications(userId);
    }
    
    @Override
    public Long getUnreadCount(Long userId) {
        return wrappedService.getUnreadCount(userId);
    }
    
    @Override
    public void markAsRead(Long notificationId) {
        wrappedService.markAsRead(notificationId);
    }
    
    @Override
    public void markAllAsRead(Long userId) {
        wrappedService.markAllAsRead(userId);
    }
    
    @Override
    public List<Map<String, Object>> getNotificationDtos(Long userId) {
        return wrappedService.getNotificationDtos(userId);
    }
    
    @Override
    public Notification createPostLikeNotification(Long postId, Long postAuthorId, Long likerId) {
        return wrappedService.createPostLikeNotification(postId, postAuthorId, likerId);
    }
    
    @Override
    public Notification createPostCommentNotification(Long postId, Long postAuthorId, Long commenterId, Long commentId) {
        return wrappedService.createPostCommentNotification(postId, postAuthorId, commenterId, commentId);
    }
    
    @Override
    public Notification createCommentLikeNotification(Long commentId, Long commentAuthorId, Long likerId) {
        return wrappedService.createCommentLikeNotification(commentId, commentAuthorId, likerId);
    }
    
    @Override
    public Notification createCommentReplyNotification(Long parentCommentId, Long parentCommentAuthorId, Long replierId, Long replyId) {
        return wrappedService.createCommentReplyNotification(parentCommentId, parentCommentAuthorId, replierId, replyId);
    }
    
    @Override
    public Notification createFriendshipRequestNotification(Long recipientId, Long senderId) {
        return wrappedService.createFriendshipRequestNotification(recipientId, senderId);
    }
    
    @Override
    public Notification createFriendshipAcceptedNotification(Long recipientId, Long senderId) {
        return wrappedService.createFriendshipAcceptedNotification(recipientId, senderId);
    }
    
    @Override
    public Notification createFriendshipRejectedNotification(Long recipientId, Long senderId) {
        return wrappedService.createFriendshipRejectedNotification(recipientId, senderId);
    }
    
    @Override
    public Notification createFriendshipCancelledNotification(Long recipientId, Long senderId) {
        return wrappedService.createFriendshipCancelledNotification(recipientId, senderId);
    }
    
    @Override
    public Notification createMentionNotification(Long mentionedUserId, Long mentionerId, Long entityId, String entityType) {
        return wrappedService.createMentionNotification(mentionedUserId, mentionerId, entityId, entityType);
    }
    
    @Override
    public Notification createSystemNotification(Long userId, String message) {
        return wrappedService.createSystemNotification(userId, message);
    }
    
    @Override
    public Notification createWelcomeNotification(Long userId) {
        return wrappedService.createWelcomeNotification(userId);
    }
}



