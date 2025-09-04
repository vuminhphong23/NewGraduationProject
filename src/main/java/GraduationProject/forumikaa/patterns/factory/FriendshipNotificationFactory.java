package GraduationProject.forumikaa.patterns.factory;

import GraduationProject.forumikaa.entity.Notification;
import GraduationProject.forumikaa.patterns.builder.NotificationBuilder;
import GraduationProject.forumikaa.entity.Notification.NotificationType;

/**
 * FriendshipNotificationFactory - Concrete Factory cho Friendship notifications
 * Implement Factory Method pattern
 */
public class FriendshipNotificationFactory implements NotificationFactory {
    
    private final Long recipientId;
    private final Long senderId;
    private final String senderName;
    private final NotificationType type;
    
    public FriendshipNotificationFactory(Long recipientId, Long senderId, String senderName, NotificationType type) {
        this.recipientId = recipientId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.type = type;
    }
    
    @Override
    public Notification createNotification() {
        return createNotification(getDefaultMessage());
    }
    
    @Override
    public Notification createNotification(String message) {
        return NotificationBuilder.newNotification()
                .recipientId(recipientId)
                .senderId(senderId)
                .type(type)
                .message(message)
                .relatedEntityId(senderId)
                .relatedEntityType("USER")
                .link("/profile/" + senderName)
                .build();
    }
    
    private String getDefaultMessage() {
        switch (type) {
            case FRIENDSHIP_REQUEST:
                return senderName + " đã gửi lời mời kết bạn";
            case FRIENDSHIP_ACCEPTED:
                return senderName + " đã chấp nhận lời mời kết bạn";
            case FRIENDSHIP_REJECTED:
                return senderName + " đã từ chối lời mời kết bạn";
            case FRIENDSHIP_CANCELLED:
                return senderName + " đã hủy kết bạn";
            default:
                return "Có thay đổi về tình trạng kết bạn";
        }
    }
    
    // Convenience methods để tạo factory cho từng loại friendship
    public static FriendshipNotificationFactory request(Long recipientId, Long senderId, String senderName) {
        return new FriendshipNotificationFactory(recipientId, senderId, senderName, NotificationType.FRIENDSHIP_REQUEST);
    }
    
    public static FriendshipNotificationFactory accepted(Long recipientId, Long senderId, String senderName) {
        return new FriendshipNotificationFactory(recipientId, senderId, senderName, NotificationType.FRIENDSHIP_ACCEPTED);
    }
    
    public static FriendshipNotificationFactory rejected(Long recipientId, Long senderId, String senderName) {
        return new FriendshipNotificationFactory(recipientId, senderId, senderName, NotificationType.FRIENDSHIP_REJECTED);
    }
    
    public static FriendshipNotificationFactory cancelled(Long recipientId, Long senderId, String senderName) {
        return new FriendshipNotificationFactory(recipientId, senderId, senderName, NotificationType.FRIENDSHIP_CANCELLED);
    }
}
