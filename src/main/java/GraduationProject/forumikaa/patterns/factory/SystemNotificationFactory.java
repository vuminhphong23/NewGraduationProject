package GraduationProject.forumikaa.patterns.factory;

import GraduationProject.forumikaa.entity.Notification;
import GraduationProject.forumikaa.patterns.builder.NotificationBuilder;
import GraduationProject.forumikaa.entity.Notification.NotificationType;

/**
 * SystemNotificationFactory - Concrete Factory cho System notifications
 * Implement Factory Method pattern
 */
public class SystemNotificationFactory implements NotificationFactory {
    
    private final Long userId;
    private final NotificationType type;
    
    public SystemNotificationFactory(Long userId, NotificationType type) {
        this.userId = userId;
        this.type = type;
    }
    
    @Override
    public Notification createNotification() {
        return createNotification(getDefaultMessage());
    }
    
    @Override
    public Notification createNotification(String message) {
        return NotificationBuilder.newNotification()
                .recipientId(userId)
                .type(type)
                .message(message)
                .build();
    }
    
    private String getDefaultMessage() {
        switch (type) {
            case SYSTEM_MESSAGE:
                return "Thông báo hệ thống";
            case WELCOME:
                return "Chào mừng bạn đến với Forumikaa! Hãy khám phá cộng đồng tuyệt vời này.";
            default:
                return "Thông báo mới";
        }
    }
    
    // Convenience methods để tạo factory cho từng loại system notification
    public static SystemNotificationFactory systemMessage(Long userId) {
        return new SystemNotificationFactory(userId, NotificationType.SYSTEM_MESSAGE);
    }
    
    public static SystemNotificationFactory welcome(Long userId) {
        return new SystemNotificationFactory(userId, NotificationType.WELCOME);
    }
    
    public static SystemNotificationFactory custom(Long userId, NotificationType type) {
        return new SystemNotificationFactory(userId, type);
    }
}
