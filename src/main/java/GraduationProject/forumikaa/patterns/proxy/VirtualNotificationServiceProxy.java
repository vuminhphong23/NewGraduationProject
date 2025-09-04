package GraduationProject.forumikaa.patterns.proxy;

import GraduationProject.forumikaa.entity.Notification;
import GraduationProject.forumikaa.service.NotificationService;
import GraduationProject.forumikaa.patterns.adapter.NotificationServiceWrapper;

import java.util.List;
import java.util.Map;

/**
 * VirtualNotificationServiceProxy - Virtual Proxy Pattern Implementation
 * Lazy loading: chỉ khởi tạo real service khi cần thiết
 * 
 * Lợi ích:
 * - Tiết kiệm memory khi không cần dùng
 * - Khởi tạo chậm (lazy initialization)
 * - Tương tác với Adapter Pattern để sử dụng Singleton
 */
public class VirtualNotificationServiceProxy implements NotificationService {
    
    private NotificationService realService;
    private final String proxyName;
    
    public VirtualNotificationServiceProxy(String proxyName) {
        this.proxyName = proxyName;
        System.out.println("🔄 Virtual Proxy '" + proxyName + "' created - Real service not yet initialized");
    }
    
    /**
     * Lazy initialization - chỉ tạo real service khi cần thiết
     */
    private void initializeRealService() {
        if (realService == null) {
            // Sử dụng Adapter thay vì truy cập trực tiếp Singleton
            realService = new NotificationServiceWrapper();
            System.out.println("🔄 Virtual Proxy: Real service initialized through Adapter");
        }
    }
    
    // Core notification methods - delegate to real service
    @Override
    public Notification createNotification(Long recipientId, Long senderId, String message, String link) {
        initializeRealService();
        System.out.println("📝 Virtual Proxy: Creating notification through real service");
        return realService.createNotification(recipientId, senderId, message, link);
    }
    
    @Override
    public Notification createNotification(Long recipientId, Long senderId, String message) {
        initializeRealService();
        System.out.println("📝 Virtual Proxy: Creating notification through real service");
        return realService.createNotification(recipientId, senderId, message);
    }
    
    @Override
    public List<Notification> getUserNotifications(Long userId) {
        initializeRealService();
        System.out.println("📋 Virtual Proxy: Getting notifications through real service");
        return realService.getUserNotifications(userId);
    }
    
    @Override
    public Long getUnreadCount(Long userId) {
        initializeRealService();
        System.out.println("🔢 Virtual Proxy: Getting unread count through real service");
        return realService.getUnreadCount(userId);
    }
    
    @Override
    public void markAsRead(Long notificationId) {
        initializeRealService();
        System.out.println("✅ Virtual Proxy: Marking as read through real service");
        realService.markAsRead(notificationId);
    }
    
    @Override
    public void markAllAsRead(Long userId) {
        initializeRealService();
        System.out.println("✅ Virtual Proxy: Marking all as read through real service");
        realService.markAllAsRead(userId);
    }
    
    @Override
    public List<Map<String, Object>> getNotificationDtos(Long userId) {
        initializeRealService();
        System.out.println("📊 Virtual Proxy: Getting notification DTOs through real service");
        return realService.getNotificationDtos(userId);
    }
    
    // Specific notification creation methods - delegate to real service
    @Override
    public Notification createPostLikeNotification(Long postId, Long postAuthorId, Long likerId) {
        initializeRealService();
        System.out.println("👍 Virtual Proxy: Creating post like notification through real service");
        return realService.createPostLikeNotification(postId, postAuthorId, likerId);
    }
    
    @Override
    public Notification createPostCommentNotification(Long postId, Long postAuthorId, Long commenterId, Long commentId) {
        initializeRealService();
        System.out.println("💬 Virtual Proxy: Creating post comment notification through real service");
        return realService.createPostCommentNotification(postId, postAuthorId, commenterId, commentId);
    }
    
    @Override
    public Notification createCommentLikeNotification(Long commentId, Long commentAuthorId, Long likerId) {
        initializeRealService();
        System.out.println("👍 Virtual Proxy: Creating comment like notification through real service");
        return realService.createCommentLikeNotification(commentId, commentAuthorId, likerId);
    }
    
    @Override
    public Notification createCommentReplyNotification(Long parentCommentId, Long parentCommentAuthorId, Long replierId, Long replyId) {
        initializeRealService();
        System.out.println("💬 Virtual Proxy: Creating comment reply notification through real service");
        return realService.createCommentReplyNotification(parentCommentId, parentCommentAuthorId, replierId, replyId);
    }
    
    @Override
    public Notification createFriendshipRequestNotification(Long recipientId, Long senderId) {
        initializeRealService();
        System.out.println("🤝 Virtual Proxy: Creating friendship request notification through real service");
        return realService.createFriendshipRequestNotification(recipientId, senderId);
    }
    
    @Override
    public Notification createFriendshipAcceptedNotification(Long recipientId, Long senderId) {
        initializeRealService();
        System.out.println("🤝 Virtual Proxy: Creating friendship accepted notification through real service");
        return realService.createFriendshipAcceptedNotification(recipientId, senderId);
    }
    
    @Override
    public Notification createFriendshipRejectedNotification(Long recipientId, Long senderId) {
        initializeRealService();
        System.out.println("🤝 Virtual Proxy: Creating friendship rejected notification through real service");
        return realService.createFriendshipRejectedNotification(recipientId, senderId);
    }
    
    @Override
    public Notification createFriendshipCancelledNotification(Long recipientId, Long senderId) {
        initializeRealService();
        System.out.println("🤝 Virtual Proxy: Creating friendship cancelled notification through real service");
        return realService.createFriendshipCancelledNotification(recipientId, senderId);
    }
    
    @Override
    public Notification createMentionNotification(Long mentionedUserId, Long mentionerId, Long entityId, String entityType) {
        initializeRealService();
        System.out.println("📢 Virtual Proxy: Creating mention notification through real service");
        return realService.createMentionNotification(mentionedUserId, mentionerId, entityId, entityType);
    }
    
    @Override
    public Notification createSystemNotification(Long userId, String message) {
        initializeRealService();
        System.out.println("🔔 Virtual Proxy: Creating system notification through real service");
        return realService.createSystemNotification(userId, message);
    }
    
    @Override
    public Notification createWelcomeNotification(Long userId) {
        initializeRealService();
        System.out.println("🎉 Virtual Proxy: Creating welcome notification through real service");
        return realService.createWelcomeNotification(userId);
    }
    
    /**
     * Method để kiểm tra trạng thái proxy
     */
    public boolean isRealServiceInitialized() {
        return realService != null;
    }
    
    /**
     * Method để lấy tên proxy
     */
    public String getProxyName() {
        return proxyName;
    }
}
