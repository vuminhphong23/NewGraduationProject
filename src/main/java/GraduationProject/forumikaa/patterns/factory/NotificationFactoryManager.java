package GraduationProject.forumikaa.patterns.factory;

import GraduationProject.forumikaa.entity.Notification;
import GraduationProject.forumikaa.entity.Notification.NotificationType;
import GraduationProject.forumikaa.patterns.builder.NotificationBuilder;

/**
 * NotificationFactoryManager - Manager class để quản lý các factory
 * Implement Factory Method pattern với khả năng tạo factory phù hợp
 */
public class NotificationFactoryManager {
    
    /**
     * Tạo factory cho Post Like notification
     */
    public static NotificationFactory createPostLikeFactory(Long postId, Long postAuthorId, Long likerId, String likerName) {
        return PostLikeNotificationFactory.forPost(postId, postAuthorId, likerId, likerName);
    }
    
    /**
     * Tạo factory cho Post Comment notification
     */
    public static NotificationFactory createPostCommentFactory(Long postId, Long postAuthorId, Long commenterId, Long commentId, String commenterName) {
        return new NotificationFactory() {
            @Override
            public Notification createNotification() {
                return createNotification(commenterName + " đã bình luận bài viết của bạn");
            }
            
            @Override
            public Notification createNotification(String message) {
                return NotificationBuilder.postCommentNotification(postId, postAuthorId, commenterId, commentId)
                        .message(message)
                        .build();
            }
        };
    }
    
    /**
     * Tạo factory cho Friendship Request notification
     */
    public static NotificationFactory createFriendshipRequestFactory(Long recipientId, Long senderId, String senderName) {
        return FriendshipNotificationFactory.request(recipientId, senderId, senderName);
    }
    
    /**
     * Tạo factory cho Friendship Accepted notification
     */
    public static NotificationFactory createFriendshipAcceptedFactory(Long recipientId, Long senderId, String senderName) {
        return FriendshipNotificationFactory.accepted(recipientId, senderId, senderName);
    }
    
    /**
     * Tạo factory cho Friendship Rejected notification
     */
    public static NotificationFactory createFriendshipRejectedFactory(Long recipientId, Long senderId, String senderName) {
        return FriendshipNotificationFactory.rejected(recipientId, senderId, senderName);
    }
    
    /**
     * Tạo factory cho Friendship Cancelled notification
     */
    public static NotificationFactory createFriendshipCancelledFactory(Long recipientId, Long senderId, String senderName) {
        return FriendshipNotificationFactory.cancelled(recipientId, senderId, senderName);
    }
    
    /**
     * Tạo factory cho System Message notification
     */
    public static NotificationFactory createSystemMessageFactory(Long userId) {
        return SystemNotificationFactory.systemMessage(userId);
    }
    
    /**
     * Tạo factory cho Welcome notification
     */
    public static NotificationFactory createWelcomeFactory(Long userId) {
        return SystemNotificationFactory.welcome(userId);
    }
    
    /**
     * Tạo factory cho Comment Like notification
     */
    public static NotificationFactory createCommentLikeFactory(Long commentId, Long commentAuthorId, Long likerId, String likerName) {
        return new NotificationFactory() {
            @Override
            public Notification createNotification() {
                return createNotification(likerName + " đã thích bình luận của bạn");
            }
            
            @Override
            public Notification createNotification(String message) {
                return NotificationBuilder.newNotification()
                        .recipientId(commentAuthorId)
                        .senderId(likerId)
                        .type(NotificationType.COMMENT_LIKE)
                        .message(message)
                        .relatedEntityId(commentId)
                        .relatedEntityType("COMMENT")
                        .build();
            }
        };
    }
    
    /**
     * Tạo factory cho Comment Reply notification
     */
    public static NotificationFactory createCommentReplyFactory(Long parentCommentId, Long parentCommentAuthorId, Long replierId, Long replyId, String replierName) {
        return new NotificationFactory() {
            @Override
            public Notification createNotification() {
                return createNotification(replierName + " đã trả lời bình luận của bạn");
            }
            
            @Override
            public Notification createNotification(String message) {
                return NotificationBuilder.newNotification()
                        .recipientId(parentCommentAuthorId)
                        .senderId(replierId)
                        .type(NotificationType.COMMENT_REPLY)
                        .message(message)
                        .relatedEntityId(parentCommentId)
                        .relatedEntityType("COMMENT")
                        .build();
            }
        };
    }
    
    /**
     * Tạo factory cho Mention notification
     */
    public static NotificationFactory createMentionFactory(Long mentionedUserId, Long mentionerId, Long entityId, String entityType, String mentionerName) {
        return new NotificationFactory() {
            @Override
            public Notification createNotification() {
                return createNotification(mentionerName + " đã nhắc đến bạn trong " + entityType.toLowerCase());
            }
            
            @Override
            public Notification createNotification(String message) {
                String link = "/" + entityType.toLowerCase() + "s/" + entityId;
                return NotificationBuilder.newNotification()
                        .recipientId(mentionedUserId)
                        .senderId(mentionerId)
                        .type(NotificationType.MENTION)
                        .message(message)
                        .relatedEntityId(entityId)
                        .relatedEntityType(entityType)
                        .link(link)
                        .build();
            }
        };
    }
}
