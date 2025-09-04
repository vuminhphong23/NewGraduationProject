package GraduationProject.forumikaa.patterns.singleton;

import GraduationProject.forumikaa.dao.NotificationDao;
import GraduationProject.forumikaa.dao.UserDao;
import GraduationProject.forumikaa.dao.CommentDao;
import GraduationProject.forumikaa.entity.Notification;
import GraduationProject.forumikaa.patterns.builder.NotificationBuilder;
import GraduationProject.forumikaa.patterns.factory.NotificationFactoryManager;
import GraduationProject.forumikaa.entity.User;
import GraduationProject.forumikaa.handler.notification.NotificationBroadcaster;
import GraduationProject.forumikaa.service.NotificationService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.HashMap;
import java.util.Optional;

/**
 * NotificationService sử dụng Singleton Pattern
 * Đảm bảo chỉ có một instance duy nhất trong toàn bộ ứng dụng
 */
public class NotificationServiceImpl implements NotificationService {

    // Private static instance - đây là instance duy nhất
    private static NotificationServiceImpl instance;
    
    // Private constructor để ngăn tạo instance từ bên ngoài
    private NotificationServiceImpl() {
        // Constructor private để ngăn instantiation
    }
    
    // Public static method để lấy instance duy nhất
    public static NotificationServiceImpl getInstance() {
        if (instance == null) {
            // Thread-safe singleton với double-checked locking
            synchronized (NotificationServiceImpl.class) {
                if (instance == null) {
                    instance = new NotificationServiceImpl();
                }
            }
        }
        return instance;
    }
    
    // Private fields để lưu các dependencies
    private NotificationBroadcaster notificationBroadcaster;
    private NotificationDao notificationDao;
    private UserDao userDao;
    private CommentDao commentDao;
    
    // Method để set các dependencies (có thể gọi từ configuration)
    public void setNotificationBroadcaster(NotificationBroadcaster notificationBroadcaster) {
        this.notificationBroadcaster = notificationBroadcaster;
    }
    
    public void setNotificationDao(NotificationDao notificationDao) {
        this.notificationDao = notificationDao;
    }
    
    public void setUserDao(UserDao userDao) {
        this.userDao = userDao;
    }
    
    public void setCommentDao(CommentDao commentDao) {
        this.commentDao = commentDao;
    }
    
    // Method để khởi tạo tất cả dependencies một lần
    public void initialize(NotificationBroadcaster notificationBroadcaster, 
                          NotificationDao notificationDao, 
                          UserDao userDao, 
                          CommentDao commentDao) {
        this.notificationBroadcaster = notificationBroadcaster;
        this.notificationDao = notificationDao;
        this.userDao = userDao;
        this.commentDao = commentDao;
    }
    
    // Method để kiểm tra xem service đã được khởi tạo chưa
    public boolean isInitialized() {
        return notificationBroadcaster != null && notificationDao != null && 
               userDao != null && commentDao != null;
    }

    @Override
    public Notification createNotification(Long recipientId, Long senderId, String message, String link) {
        if (!isInitialized()) {
            throw new IllegalStateException("NotificationService chưa được khởi tạo. Vui lòng gọi initialize() trước.");
        }
        
        Notification noti = NotificationBuilder.newNotification()
                .recipientId(recipientId)
                .senderId(senderId)
                .message(message)
                .link(link)
                .type(Notification.NotificationType.SYSTEM_MESSAGE)
                .build();

        Notification saved = notificationDao.save(noti);
        notificationBroadcaster.publish(recipientId, saved);
        return saved;
    }

    @Override
    public Notification createNotification(Long recipientId, Long senderId, String message) {
        if (!isInitialized()) {
            throw new IllegalStateException("NotificationService chưa được khởi tạo. Vui lòng gọi initialize() trước.");
        }
        
        Notification noti = NotificationBuilder.newNotification()
                .recipientId(recipientId)
                .senderId(senderId)
                .message(message)
                .type(Notification.NotificationType.SYSTEM_MESSAGE)
                .build();

        Notification saved = notificationDao.save(noti);
        notificationBroadcaster.publish(recipientId, saved);
        return saved;
    }

    @Override
    public List<Notification> getUserNotifications(Long userId) {
        if (!isInitialized()) {
            throw new IllegalStateException("NotificationService chưa được khởi tạo. Vui lòng gọi initialize() trước.");
        }
        
        return notificationDao.findByRecipientIdOrderByCreatedAtDesc(userId);
    }

    @Override
    public Long getUnreadCount(Long userId) {
        if (!isInitialized()) {
            throw new IllegalStateException("NotificationService chưa được khởi tạo. Vui lòng gọi initialize() trước.");
        }
        
        return notificationDao.countUnreadByRecipientId(userId);
    }

    @Override
    public void markAsRead(Long notificationId) {
        if (!isInitialized()) {
            throw new IllegalStateException("NotificationService chưa được khởi tạo. Vui lòng gọi initialize() trước.");
        }
        
        notificationDao.markAsReadById(notificationId);
    }

    @Override
    public void markAllAsRead(Long userId) {
        if (!isInitialized()) {
            throw new IllegalStateException("NotificationService chưa được khởi tạo. Vui lòng gọi initialize() trước.");
        }
        
        notificationDao.markAllAsReadByRecipientId(userId);
    }

    @Override
    public List<Map<String, Object>> getNotificationDtos(Long userId) {
        if (!isInitialized()) {
            throw new IllegalStateException("NotificationService chưa được khởi tạo. Vui lòng gọi initialize() trước.");
        }
        
        List<Notification> notifications = getUserNotifications(userId);
        return notifications.stream().map(n -> {
            Map<String, Object> dto = new HashMap<>();
            dto.put("id", n.getId());
            dto.put("message", n.getMessage());
            dto.put("link", n.getLink());
            dto.put("isRead", n.isRead());
            dto.put("createdAt", n.getCreatedAt());
            dto.put("type", n.getType());
            dto.put("relatedEntityId", n.getRelatedEntityId());
            dto.put("relatedEntityType", n.getRelatedEntityType());
            
            // Lấy thông tin sender
            if (n.getSenderId() != null) {
                Optional<User> sender = userDao.findById(n.getSenderId());
                if (sender.isPresent()) {
                    dto.put("senderId", n.getSenderId());
                    dto.put("senderName", sender.get().getUsername());
                }
            }
            
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    public Notification createPostLikeNotification(Long postId, Long postAuthorId, Long likerId) {
        if (!isInitialized()) {
            throw new IllegalStateException("NotificationService chưa được khởi tạo. Vui lòng gọi initialize() trước.");
        }
        
        // Lấy tên thật của người thích từ database
        Optional<User> liker = userDao.findById(likerId);
        String likerName = liker.map(User::getUsername).orElse("Người dùng");
        
        // Sử dụng Factory Manager thay vì Builder trực tiếp
        var factory = NotificationFactoryManager.createPostLikeFactory(postId, postAuthorId, likerId, likerName);
        
        Notification notification = factory.createNotification();
        Notification saved = notificationDao.save(notification);
        notificationBroadcaster.publish(postAuthorId, saved);
        return saved;
    }

    @Override
    public Notification createPostCommentNotification(Long postId, Long postAuthorId, Long commenterId, Long commentId) {
        if (!isInitialized()) {
            throw new IllegalStateException("NotificationService chưa được khởi tạo. Vui lòng gọi initialize() trước.");
        }
        
        Optional<User> commenter = userDao.findById(commenterId);
        String commenterName = commenter.map(User::getUsername).orElse("Người dùng");
        
        Notification notification = NotificationBuilder.postCommentNotification(postId, postAuthorId, commenterId, commentId)
                .message(commenterName + " đã bình luận bài viết của bạn")
                .build();

        Notification saved = notificationDao.save(notification);
        notificationBroadcaster.publish(postAuthorId, saved);
        return saved;
    }

    @Override
    public Notification createCommentLikeNotification(Long commentId, Long commentAuthorId, Long likerId) {
        if (!isInitialized()) {
            throw new IllegalStateException("NotificationService chưa được khởi tạo. Vui lòng gọi initialize() trước.");
        }
        
        Optional<User> liker = userDao.findById(likerId);
        String likerName = liker.map(User::getUsername).orElse("Người dùng");
        
        // Tìm postId từ commentId để tạo link
        Optional<Long> postIdOpt = commentDao.findPostIdByCommentId(commentId);
        String link = postIdOpt.map(postId -> "/posts/" + postId + "#comment-" + commentId)
                               .orElse("/"); // Fallback về trang chủ nếu không tìm thấy
        
        Notification notification = Notification.builder()
                .recipientId(commentAuthorId)
                .senderId(likerId)
                .type(Notification.NotificationType.COMMENT_LIKE)
                .message(likerName + " đã thích bình luận của bạn")
                .relatedEntityId(commentId)
                .relatedEntityType("COMMENT")
                .link(link)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        Notification saved = notificationDao.save(notification);
        notificationBroadcaster.publish(commentAuthorId, saved);
        return saved;
    }

    @Override
    public Notification createCommentReplyNotification(Long parentCommentId, Long parentCommentAuthorId, Long replierId, Long replyId) {
        if (!isInitialized()) {
            throw new IllegalStateException("NotificationService chưa được khởi tạo. Vui lòng gọi initialize() trước.");
        }
        
        Optional<User> replier = userDao.findById(replierId);
        String replierName = replier.map(User::getUsername).orElse("Người dùng");
        
        // Tìm postId từ commentId để tạo link
        Optional<Long> postIdOpt = commentDao.findPostIdByCommentId(parentCommentId);
        String link = postIdOpt.map(postId -> "/posts/" + postId + "#comment-" + parentCommentId)
                               .orElse("/"); // Fallback về trang chủ nếu không tìm thấy
        
        Notification notification = Notification.builder()
                .recipientId(parentCommentAuthorId)
                .senderId(replierId)
                .type(Notification.NotificationType.COMMENT_REPLY)
                .message(replierName + " đã trả lời bình luận của bạn")
                .relatedEntityId(parentCommentId)
                .relatedEntityType("COMMENT")
                .link(link)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        Notification saved = notificationDao.save(notification);
        notificationBroadcaster.publish(parentCommentAuthorId, saved);
        return saved;
    }

    @Override
    public Notification createFriendshipRequestNotification(Long recipientId, Long senderId) {
        if (!isInitialized()) {
            throw new IllegalStateException("NotificationService chưa được khởi tạo. Vui lòng gọi initialize() trước.");
        }
        
        // Lấy tên thật của người gửi yêu cầu từ database
        Optional<User> sender = userDao.findById(senderId);
        String senderName = sender.map(User::getUsername).orElse("Người dùng");
        
        // Sử dụng Factory Manager thay vì Builder trực tiếp
        var factory = NotificationFactoryManager.createFriendshipRequestFactory(recipientId, senderId, senderName);
        
        Notification notification = factory.createNotification();
        Notification saved = notificationDao.save(notification);
        notificationBroadcaster.publish(recipientId, saved);
        return saved;
    }

    @Override
    public Notification createFriendshipAcceptedNotification(Long recipientId, Long senderId) {
        if (!isInitialized()) {
            throw new IllegalStateException("NotificationService chưa được khởi tạo. Vui lòng gọi initialize() trước.");
        }
        
        Optional<User> sender = userDao.findById(senderId);
        String senderName = sender.map(User::getUsername).orElse("Người dùng");
        
        Notification notification = Notification.builder()
                .recipientId(recipientId)
                .senderId(senderId)
                .type(Notification.NotificationType.FRIENDSHIP_ACCEPTED)
                .message(senderName + " đã chấp nhận lời mời kết bạn")
                .relatedEntityId(senderId)
                .relatedEntityType("USER")
                .link("/profile/" + senderName)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        Notification saved = notificationDao.save(notification);
        notificationBroadcaster.publish(recipientId, saved);
        return saved;
    }

    @Override
    public Notification createFriendshipRejectedNotification(Long recipientId, Long senderId) {
        if (!isInitialized()) {
            throw new IllegalStateException("NotificationService chưa được khởi tạo. Vui lòng gọi initialize() trước.");
        }
        
        Optional<User> sender = userDao.findById(senderId);
        String senderName = sender.map(User::getUsername).orElse("Người dùng");
        
        Notification notification = Notification.builder()
                .recipientId(recipientId)
                .senderId(senderId)
                .type(Notification.NotificationType.FRIENDSHIP_REJECTED)
                .message(senderName + " đã từ chối lời mời kết bạn")
                .relatedEntityId(senderId)
                .relatedEntityType("USER")
                .link("/profile/" + senderName)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        Notification saved = notificationDao.save(notification);
        notificationBroadcaster.publish(recipientId, saved);
        return saved;
    }

    @Override
    public Notification createFriendshipCancelledNotification(Long recipientId, Long senderId) {
        if (!isInitialized()) {
            throw new IllegalStateException("NotificationService chưa được khởi tạo. Vui lòng gọi initialize() trước.");
        }
        
        Optional<User> sender = userDao.findById(senderId);
        String senderName = sender.map(User::getUsername).orElse("Người dùng");
        
        Notification notification = Notification.builder()
                .recipientId(recipientId)
                .senderId(senderId)
                .type(Notification.NotificationType.FRIENDSHIP_CANCELLED)
                .message(senderName + " đã hủy kết bạn")
                .relatedEntityId(senderId)
                .relatedEntityType("USER")
                .link("/profile/" + senderName)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        Notification saved = notificationDao.save(notification);
        notificationBroadcaster.publish(recipientId, saved);
        return saved;
    }

    @Override
    public Notification createMentionNotification(Long mentionedUserId, Long mentionerId, Long entityId, String entityType) {
        if (!isInitialized()) {
            throw new IllegalStateException("NotificationService chưa được khởi tạo. Vui lòng gọi initialize() trước.");
        }
        
        Optional<User> mentioner = userDao.findById(mentionerId);
        String mentionerName = mentioner.map(User::getUsername).orElse("Người dùng");
        
        String link = "/" + entityType.toLowerCase() + "s/" + entityId;
        
        Notification notification = Notification.builder()
                .recipientId(mentionedUserId)
                .senderId(mentionerId)
                .type(Notification.NotificationType.MENTION)
                .message(mentionerName + " đã nhắc đến bạn trong " + entityType.toLowerCase())
                .relatedEntityId(entityId)
                .relatedEntityType(entityType)
                .link(link)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        Notification saved = notificationDao.save(notification);
        notificationBroadcaster.publish(mentionedUserId, saved);
        return saved;
    }

    @Override
    public Notification createSystemNotification(Long userId, String message) {
        if (!isInitialized()) {
            throw new IllegalStateException("NotificationService chưa được khởi tạo. Vui lòng gọi initialize() trước.");
        }
        
        // Sử dụng Factory Manager thay vì Builder trực tiếp
        var factory = NotificationFactoryManager.createSystemMessageFactory(userId);
        
        Notification notification = factory.createNotification(message);
        Notification saved = notificationDao.save(notification);
        notificationBroadcaster.publish(userId, saved);
        return saved;
    }

    @Override
    public Notification createWelcomeNotification(Long userId) {
        if (!isInitialized()) {
            throw new IllegalStateException("NotificationService chưa được khởi tạo. Vui lòng gọi initialize() trước.");
        }
        
        Notification notification = NotificationBuilder.welcomeNotification(userId).build();

        Notification saved = notificationDao.save(notification);
        notificationBroadcaster.publish(userId, saved);
        return saved;
    }
    
    // Method để reset instance (chủ yếu dùng cho testing)
    public static void resetInstance() {
        instance = null;
    }
}
