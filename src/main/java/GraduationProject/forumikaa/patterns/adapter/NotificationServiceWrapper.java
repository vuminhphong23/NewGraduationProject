package GraduationProject.forumikaa.patterns.adapter;

import GraduationProject.forumikaa.entity.Notification;
import GraduationProject.forumikaa.patterns.singleton.NotificationServiceImpl;
import GraduationProject.forumikaa.patterns.proxy.AccessControlProxy;
import GraduationProject.forumikaa.patterns.decorator.NotificationServiceComponent;
import GraduationProject.forumikaa.patterns.decorator.LoggingNotificationServiceDecorator;
import GraduationProject.forumikaa.patterns.decorator.CachingNotificationServiceDecorator;
import GraduationProject.forumikaa.service.NotificationService;
import GraduationProject.forumikaa.handler.notification.NotificationBroadcaster;
import GraduationProject.forumikaa.dao.NotificationDao;
import GraduationProject.forumikaa.dao.UserDao;
import GraduationProject.forumikaa.dao.CommentDao;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * NotificationServiceWrapper - Adapter Pattern Implementation
 * Làm cầu nối giữa Spring Dependency Injection và Singleton Pattern
 * TÍCH HỢP PROXY PATTERN để thêm access control
 * 
 * Vấn đề: Spring không thể quản lý Singleton instance
 * Giải pháp: Tạo wrapper class implement NotificationService interface
 * Bên trong wrapper sử dụng Singleton instance + Proxy Pattern
 */
@Service
public class NotificationServiceWrapper implements NotificationService {
    
    private final NotificationServiceImpl singletonService;
    private final AccessControlProxy accessControlProxy;
    private final NotificationServiceComponent decoratedService;
    
    public NotificationServiceWrapper() {
        // Lấy singleton instance
        this.singletonService = NotificationServiceImpl.getInstance();
        
        // Tạo AccessControlProxy để kiểm soát quyền truy cập
        this.accessControlProxy = new AccessControlProxy(singletonService);
        
        // Tạo decorated service với Caching + Logging
        NotificationServiceComponent baseService = new NotificationServiceComponent() {
            // Implement tất cả methods để delegate đến accessControlProxy
            @Override
            public GraduationProject.forumikaa.entity.Notification createNotification(Long recipientId, Long senderId, String message, String link) {
                return accessControlProxy.createNotification(recipientId, senderId, message, link);
            }
            
            @Override
            public GraduationProject.forumikaa.entity.Notification createNotification(Long recipientId, Long senderId, String message) {
                return accessControlProxy.createNotification(recipientId, senderId, message);
            }
            
            @Override
            public java.util.List<GraduationProject.forumikaa.entity.Notification> getUserNotifications(Long userId) {
                return accessControlProxy.getUserNotifications(userId);
            }
            
            @Override
            public Long getUnreadCount(Long userId) {
                return accessControlProxy.getUnreadCount(userId);
            }
            
            @Override
            public void markAsRead(Long notificationId) {
                try {
                    System.out.println("🔄 BaseService: Calling accessControlProxy.markAsRead(" + notificationId + ")");
                    accessControlProxy.markAsRead(notificationId);
                    System.out.println("✅ BaseService: accessControlProxy.markAsRead(" + notificationId + ") completed successfully");
                } catch (Exception e) {
                    System.err.println("❌ BaseService: Error in markAsRead(" + notificationId + "): " + e.getMessage());
                    e.printStackTrace();
                    throw e;
                }
            }
            
            @Override
            public void markAllAsRead(Long userId) {
                try {
                    System.out.println("🔄 BaseService: Calling accessControlProxy.markAllAsRead(" + userId + ")");
                    accessControlProxy.markAllAsRead(userId);
                    System.out.println("✅ BaseService: accessControlProxy.markAllAsRead(" + userId + ") completed successfully");
                } catch (Exception e) {
                    System.err.println("❌ BaseService: Error in markAllAsRead(" + userId + "): " + e.getMessage());
                    e.printStackTrace();
                    throw e;
                }
            }
            
            @Override
            public java.util.List<java.util.Map<String, Object>> getNotificationDtos(Long userId) {
                return accessControlProxy.getNotificationDtos(userId);
            }
            
            @Override
            public GraduationProject.forumikaa.entity.Notification createPostLikeNotification(Long postId, Long postAuthorId, Long likerId) {
                return accessControlProxy.createPostLikeNotification(postId, postAuthorId, likerId);
            }
            
            @Override
            public GraduationProject.forumikaa.entity.Notification createPostCommentNotification(Long postId, Long postAuthorId, Long commenterId, Long commentId) {
                return accessControlProxy.createPostCommentNotification(postId, postAuthorId, commenterId, commentId);
            }
            
            @Override
            public GraduationProject.forumikaa.entity.Notification createCommentLikeNotification(Long commentId, Long commentAuthorId, Long likerId) {
                return accessControlProxy.createCommentLikeNotification(commentId, commentAuthorId, likerId);
            }
            
            @Override
            public GraduationProject.forumikaa.entity.Notification createCommentReplyNotification(Long parentCommentId, Long parentCommentAuthorId, Long replierId, Long replyId) {
                return accessControlProxy.createCommentReplyNotification(parentCommentId, parentCommentAuthorId, replierId, replyId);
            }
            
            @Override
            public GraduationProject.forumikaa.entity.Notification createFriendshipRequestNotification(Long recipientId, Long senderId) {
                return accessControlProxy.createFriendshipRequestNotification(recipientId, senderId);
            }
            
            @Override
            public GraduationProject.forumikaa.entity.Notification createFriendshipAcceptedNotification(Long recipientId, Long senderId) {
                return accessControlProxy.createFriendshipAcceptedNotification(recipientId, senderId);
            }
            
            @Override
            public GraduationProject.forumikaa.entity.Notification createFriendshipRejectedNotification(Long recipientId, Long senderId) {
                return accessControlProxy.createFriendshipRejectedNotification(recipientId, senderId);
            }
            
            @Override
            public GraduationProject.forumikaa.entity.Notification createFriendshipCancelledNotification(Long recipientId, Long senderId) {
                return accessControlProxy.createFriendshipCancelledNotification(recipientId, senderId);
            }
            
            @Override
            public GraduationProject.forumikaa.entity.Notification createMentionNotification(Long mentionedUserId, Long mentionerId, Long entityId, String entityType) {
                return accessControlProxy.createMentionNotification(mentionedUserId, mentionerId, entityId, entityType);
            }
            
            @Override
            public GraduationProject.forumikaa.entity.Notification createSystemNotification(Long userId, String message) {
                return accessControlProxy.createSystemNotification(userId, message);
            }
            
            @Override
            public GraduationProject.forumikaa.entity.Notification createWelcomeNotification(Long userId) {
                return accessControlProxy.createWelcomeNotification(userId);
            }
        };
        
        // Wrap với Logging Decorator
        NotificationServiceComponent loggingService = new LoggingNotificationServiceDecorator(baseService);
        
        // Wrap với Caching Decorator
        this.decoratedService = new CachingNotificationServiceDecorator(loggingService, 5);
        
        // Khởi tạo với dependencies (sẽ được inject sau)
        // Trong thực tế, bạn có thể inject các dependencies cần thiết
        // Tạm thời không khởi tạo để tránh lỗi
        System.out.println("⚠️ NotificationServiceWrapper: Singleton chưa được khởi tạo với dependencies thật");
        System.out.println("   Sẽ được khởi tạo từ NotificationConfig sau khi Spring context được tạo");
    }
    
    // Core notification methods - delegate to decorated service
    @Override
    public Notification createNotification(Long recipientId, Long senderId, String message, String link) {
        return decoratedService.createNotification(recipientId, senderId, message, link);
    }
    
    @Override
    public Notification createNotification(Long recipientId, Long senderId, String message) {
        return decoratedService.createNotification(recipientId, senderId, message);
    }
    
    @Override
    public List<Notification> getUserNotifications(Long userId) {
        return decoratedService.getUserNotifications(userId);
    }
    
    @Override
    public Long getUnreadCount(Long userId) {
        return decoratedService.getUnreadCount(userId);
    }
    
    @Override
    public void markAsRead(Long notificationId) {
        decoratedService.markAsRead(notificationId);
    }
    
    @Override
    public void markAllAsRead(Long userId) {
        decoratedService.markAllAsRead(userId);
    }
    
    @Override
    public List<Map<String, Object>> getNotificationDtos(Long userId) {
        return decoratedService.getNotificationDtos(userId);
    }
    
    // Specific notification creation methods - delegate to decorated service
    @Override
    public Notification createPostLikeNotification(Long postId, Long postAuthorId, Long likerId) {
        return decoratedService.createPostLikeNotification(postId, postAuthorId, likerId);
    }
    
    @Override
    public Notification createPostCommentNotification(Long postId, Long postAuthorId, Long commenterId, Long commentId) {
        return decoratedService.createPostCommentNotification(postId, postAuthorId, commenterId, commentId);
    }
    
    @Override
    public Notification createCommentLikeNotification(Long commentId, Long commentAuthorId, Long likerId) {
        return decoratedService.createCommentLikeNotification(commentId, commentAuthorId, likerId);
    }
    
    @Override
    public Notification createCommentReplyNotification(Long parentCommentId, Long parentCommentAuthorId, Long replierId, Long replyId) {
        return decoratedService.createCommentReplyNotification(parentCommentId, parentCommentAuthorId, replierId, replyId);
    }
    
    @Override
    public Notification createFriendshipRequestNotification(Long recipientId, Long senderId) {
        return decoratedService.createFriendshipRequestNotification(recipientId, senderId);
    }
    
    @Override
    public Notification createFriendshipAcceptedNotification(Long recipientId, Long senderId) {
        return decoratedService.createFriendshipAcceptedNotification(recipientId, senderId);
    }
    
    @Override
    public Notification createFriendshipRejectedNotification(Long recipientId, Long senderId) {
        return decoratedService.createFriendshipRejectedNotification(recipientId, senderId);
    }
    
    @Override
    public Notification createFriendshipCancelledNotification(Long recipientId, Long senderId) {
        return decoratedService.createFriendshipCancelledNotification(recipientId, senderId);
    }
    
    @Override
    public Notification createMentionNotification(Long mentionedUserId, Long mentionerId, Long entityId, String entityType) {
        return decoratedService.createMentionNotification(mentionedUserId, mentionerId, entityId, entityType);
    }
    
    @Override
    public Notification createSystemNotification(Long userId, String message) {
        return decoratedService.createSystemNotification(userId, message);
    }
    
    @Override
    public Notification createWelcomeNotification(Long userId) {
        return decoratedService.createWelcomeNotification(userId);
    }
    
    /**
     * Method để truy cập trực tiếp singleton instance nếu cần
     */
    public NotificationServiceImpl getSingletonInstance() {
        return singletonService;
    }
    
    /**
     * Method để truy cập access control proxy
     */
    public AccessControlProxy getAccessControlProxy() {
        return accessControlProxy;
    }
    
    /**
     * Method để truy cập decorated service
     */
    public NotificationServiceComponent getDecoratedService() {
        return decoratedService;
    }
    
    /**
     * Method để truy cập caching decorator để quản lý cache
     */
    public CachingNotificationServiceDecorator getCachingDecorator() {
        if (decoratedService instanceof CachingNotificationServiceDecorator) {
            return (CachingNotificationServiceDecorator) decoratedService;
        }
        return null;
    }
    
    /**
     * Method để kiểm tra trạng thái singleton
     */
    public boolean isSingletonInitialized() {
        return singletonService.isInitialized();
    }
    
    /**
     * Method để khởi tạo singleton với dependencies thật
     * Được gọi từ NotificationConfig sau khi Spring context được tạo
     */
    public void initializeSingleton(NotificationBroadcaster notificationBroadcaster, 
                                   NotificationDao notificationDao, 
                                   UserDao userDao, 
                                   CommentDao commentDao) {
        try {
            singletonService.initialize(notificationBroadcaster, notificationDao, userDao, commentDao);
            System.out.println("✅ Singleton initialized with real dependencies through Adapter + Proxy");
        } catch (Exception e) {
            System.err.println("⚠️ Error initializing singleton: " + e.getMessage());
            System.err.println("   Make sure all dependencies are properly injected");
        }
    }
}
