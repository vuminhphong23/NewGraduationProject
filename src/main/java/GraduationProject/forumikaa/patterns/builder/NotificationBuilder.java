package GraduationProject.forumikaa.patterns.builder;

import GraduationProject.forumikaa.entity.Notification;

import java.time.LocalDateTime;

/**
 * NotificationBuilder sử dụng Builder Pattern
 * Cho phép tạo Notification object một cách linh hoạt và dễ đọc
 */
public class NotificationBuilder {
    
    // Các field tương ứng với Notification entity
    private Long recipientId;
    private Long senderId;
    private Notification.NotificationType type;
    private String message;
    private Long relatedEntityId;
    private String relatedEntityType;
    private String link;
    private boolean isRead = false;
    private LocalDateTime createdAt;
    
    // Constructor private để ngăn tạo instance trực tiếp
    private NotificationBuilder() {
        // Khởi tạo giá trị mặc định
        this.isRead = false;
        this.createdAt = LocalDateTime.now();
    }
    
    // Static method để bắt đầu builder
    public static NotificationBuilder newNotification() {
        return new NotificationBuilder();
    }
    
    // Builder methods cho từng field
    public NotificationBuilder recipientId(Long recipientId) {
        this.recipientId = recipientId;
        return this;
    }
    
    public NotificationBuilder senderId(Long senderId) {
        this.senderId = senderId;
        return this;
    }
    
    public NotificationBuilder type(Notification.NotificationType type) {
        this.type = type;
        return this;
    }
    
    public NotificationBuilder message(String message) {
        this.message = message;
        return this;
    }
    
    public NotificationBuilder relatedEntityId(Long relatedEntityId) {
        this.relatedEntityId = relatedEntityId;
        return this;
    }
    
    public NotificationBuilder relatedEntityType(String relatedEntityType) {
        this.relatedEntityType = relatedEntityType;
        return this;
    }
    
    public NotificationBuilder link(String link) {
        this.link = link;
        return this;
    }
    
    public NotificationBuilder isRead(boolean isRead) {
        this.isRead = isRead;
        return this;
    }
    
    public NotificationBuilder createdAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }
    
    // Method để build Notification object
    public Notification build() {
        // Validation trước khi build
        validate();
        
        // Tạo Notification object
        Notification notification = new Notification();
        notification.setRecipientId(recipientId);
        notification.setSenderId(senderId);
        notification.setType(type);
        notification.setMessage(message);
        notification.setRelatedEntityId(relatedEntityId);
        notification.setRelatedEntityType(relatedEntityType);
        notification.setLink(link);
        notification.setRead(isRead);
        notification.setCreatedAt(createdAt);
        
        return notification;
    }
    
    // Validation method
    private void validate() {
        if (recipientId == null) {
            throw new IllegalArgumentException("recipientId không được null");
        }
        if (type == null) {
            throw new IllegalArgumentException("type không được null");
        }
        if (message == null || message.trim().isEmpty()) {
            throw new IllegalArgumentException("message không được null hoặc rỗng");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt không được null");
        }
    }
    
    // Convenience methods cho các loại notification phổ biến
    
    /**
     * Builder cho Post Like notification
     */
    public static NotificationBuilder postLikeNotification(Long postId, Long postAuthorId, Long likerId) {
        return newNotification()
                .type(Notification.NotificationType.POST_LIKE)
                .recipientId(postAuthorId)
                .senderId(likerId)
                .relatedEntityId(postId)
                .relatedEntityType("POST")
                .link("/posts/" + postId);
    }
    
    /**
     * Builder cho Post Comment notification
     */
    public static NotificationBuilder postCommentNotification(Long postId, Long postAuthorId, Long commenterId, Long commentId) {
        return newNotification()
                .type(Notification.NotificationType.POST_COMMENT)
                .recipientId(postAuthorId)
                .senderId(commenterId)
                .relatedEntityId(postId)
                .relatedEntityType("POST")
                .link("/posts/" + postId + "#comment-" + commentId);
    }
    
    /**
     * Builder cho Friendship Request notification
     */
    public static NotificationBuilder friendshipRequestNotification(Long recipientId, Long senderId) {
        return newNotification()
                .type(Notification.NotificationType.FRIENDSHIP_REQUEST)
                .recipientId(recipientId)
                .senderId(senderId)
                .relatedEntityId(senderId)
                .relatedEntityType("USER");
    }
    
    /**
     * Builder cho System Message notification
     */
    public static NotificationBuilder systemNotification(Long userId, String message) {
        return newNotification()
                .type(Notification.NotificationType.SYSTEM_MESSAGE)
                .recipientId(userId)
                .message(message);
    }
    
    /**
     * Builder cho Welcome notification
     */
    public static NotificationBuilder welcomeNotification(Long userId) {
        return newNotification()
                .type(Notification.NotificationType.WELCOME)
                .recipientId(userId)
                .message("Chào mừng bạn đến với Forumikaa! Hãy khám phá cộng đồng tuyệt vời này.")
                .link("/");
    }
}
