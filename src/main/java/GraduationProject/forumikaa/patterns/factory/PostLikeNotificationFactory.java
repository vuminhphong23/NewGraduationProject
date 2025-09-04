package GraduationProject.forumikaa.patterns.factory;

import GraduationProject.forumikaa.entity.Notification;
import GraduationProject.forumikaa.patterns.builder.NotificationBuilder;

/**
 * PostLikeNotificationFactory - Concrete Factory cho Post Like notifications
 * Implement Factory Method pattern
 */
public class PostLikeNotificationFactory implements NotificationFactory {
    
    private final Long postId;
    private final Long postAuthorId;
    private final Long likerId;
    private final String likerName;
    
    public PostLikeNotificationFactory(Long postId, Long postAuthorId, Long likerId, String likerName) {
        this.postId = postId;
        this.postAuthorId = postAuthorId;
        this.likerId = likerId;
        this.likerName = likerName;
    }
    
    @Override
    public Notification createNotification() {
        return createNotification(likerName + " đã thích bài viết của bạn");
    }
    
    @Override
    public Notification createNotification(String message) {
        return NotificationBuilder.postLikeNotification(postId, postAuthorId, likerId)
                .message(message)
                .build();
    }
    
    // Convenience method để tạo factory với thông tin cơ bản
    public static PostLikeNotificationFactory forPost(Long postId, Long postAuthorId, Long likerId, String likerName) {
        return new PostLikeNotificationFactory(postId, postAuthorId, likerId, likerName);
    }
}
