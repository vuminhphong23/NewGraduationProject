package GraduationProject.forumikaa.patterns.decorator;

import GraduationProject.forumikaa.entity.Notification;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Logging Decorator - Thêm tính năng logging để theo dõi hoạt động
 * Log tất cả các method calls với thời gian và thông tin chi tiết
 */
public class LoggingNotificationServiceDecorator extends BaseNotificationServiceDecorator {
    
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    private final String serviceName;
    
    public LoggingNotificationServiceDecorator(NotificationServiceComponent wrappedService) {
        this(wrappedService, "NotificationService");
    }
    
    public LoggingNotificationServiceDecorator(NotificationServiceComponent wrappedService, String serviceName) {
        super(wrappedService);
        this.serviceName = serviceName;
        System.out.println("📝 Logging Decorator: Started logging for " + serviceName);
    }
    
    @Override
    public Notification createNotification(Long recipientId, Long senderId, String message, String link) {
        logMethodCall("createNotification", "recipientId=" + recipientId + ", senderId=" + senderId + ", message=\"" + message + "\", link=" + link);
        
        long startTime = System.currentTimeMillis();
        try {
            Notification result = wrappedService.createNotification(recipientId, senderId, message, link);
            long duration = System.currentTimeMillis() - startTime;
            logMethodSuccess("createNotification", duration, "Created notification ID: " + result.getId());
            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            logMethodError("createNotification", duration, e.getMessage());
            throw e;
        }
    }
    
    @Override
    public Notification createNotification(Long recipientId, Long senderId, String message) {
        logMethodCall("createNotification", "recipientId=" + recipientId + ", senderId=" + senderId + ", message=\"" + message + "\"");
        
        long startTime = System.currentTimeMillis();
        try {
            Notification result = wrappedService.createNotification(recipientId, senderId, message);
            long duration = System.currentTimeMillis() - startTime;
            logMethodSuccess("createNotification", duration, "Created notification ID: " + result.getId());
            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            logMethodError("createNotification", duration, e.getMessage());
            throw e;
        }
    }
    
    @Override
    public List<Notification> getUserNotifications(Long userId) {
        logMethodCall("getUserNotifications", "userId=" + userId);
        
        long startTime = System.currentTimeMillis();
        try {
            List<Notification> result = wrappedService.getUserNotifications(userId);
            long duration = System.currentTimeMillis() - startTime;
            logMethodSuccess("getUserNotifications", duration, "Retrieved " + result.size() + " notifications");
            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            logMethodError("getUserNotifications", duration, e.getMessage());
            throw e;
        }
    }
    
    @Override
    public Long getUnreadCount(Long userId) {
        logMethodCall("getUnreadCount", "userId=" + userId);
        
        long startTime = System.currentTimeMillis();
        try {
            Long result = wrappedService.getUnreadCount(userId);
            long duration = System.currentTimeMillis() - startTime;
            logMethodSuccess("getUnreadCount", duration, "Unread count: " + result);
            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            logMethodError("getUnreadCount", duration, e.getMessage());
            throw e;
        }
    }
    
    @Override
    public void markAsRead(Long notificationId) {
        logMethodCall("markAsRead", "notificationId=" + notificationId);
        
        long startTime = System.currentTimeMillis();
        try {
            wrappedService.markAsRead(notificationId);
            long duration = System.currentTimeMillis() - startTime;
            logMethodSuccess("markAsRead", duration, "Marked notification " + notificationId + " as read");
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            logMethodError("markAsRead", duration, e.getMessage());
            throw e;
        }
    }
    
    @Override
    public void markAllAsRead(Long userId) {
        logMethodCall("markAllAsRead", "userId=" + userId);
        
        long startTime = System.currentTimeMillis();
        try {
            wrappedService.markAllAsRead(userId);
            long duration = System.currentTimeMillis() - startTime;
            logMethodSuccess("markAllAsRead", duration, "Marked all notifications as read for user " + userId);
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            logMethodError("markAllAsRead", duration, e.getMessage());
            throw e;
        }
    }
    
    @Override
    public Notification createPostLikeNotification(Long postId, Long postAuthorId, Long likerId) {
        logMethodCall("createPostLikeNotification", "postId=" + postId + ", postAuthorId=" + postAuthorId + ", likerId=" + likerId);
        
        long startTime = System.currentTimeMillis();
        try {
            Notification result = wrappedService.createPostLikeNotification(postId, postAuthorId, likerId);
            long duration = System.currentTimeMillis() - startTime;
            logMethodSuccess("createPostLikeNotification", duration, "Created post like notification ID: " + result.getId());
            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            logMethodError("createPostLikeNotification", duration, e.getMessage());
            throw e;
        }
    }
    
    @Override
    public Notification createCommentLikeNotification(Long commentId, Long commentAuthorId, Long likerId) {
        logMethodCall("createCommentLikeNotification", "commentId=" + commentId + ", commentAuthorId=" + commentAuthorId + ", likerId=" + likerId);
        
        long startTime = System.currentTimeMillis();
        try {
            Notification result = wrappedService.createCommentLikeNotification(commentId, commentAuthorId, likerId);
            long duration = System.currentTimeMillis() - startTime;
            logMethodSuccess("createCommentLikeNotification", duration, "Created comment like notification ID: " + result.getId());
            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            logMethodError("createCommentLikeNotification", duration, e.getMessage());
            throw e;
        }
    }
    
    // Helper methods for logging
    private void logMethodCall(String methodName, String parameters) {
        String timestamp = LocalDateTime.now().format(formatter);
        System.out.println("📝 [" + timestamp + "] " + serviceName + "." + methodName + "(" + parameters + ") - CALLED");
    }
    
    private void logMethodSuccess(String methodName, long duration, String result) {
        String timestamp = LocalDateTime.now().format(formatter);
        System.out.println("✅ [" + timestamp + "] " + serviceName + "." + methodName + " - SUCCESS (" + duration + "ms) - " + result);
    }
    
    private void logMethodError(String methodName, long duration, String error) {
        String timestamp = LocalDateTime.now().format(formatter);
        System.err.println("❌ [" + timestamp + "] " + serviceName + "." + methodName + " - ERROR (" + duration + "ms) - " + error);
    }
}



