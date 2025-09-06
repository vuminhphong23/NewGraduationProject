package GraduationProject.forumikaa.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "notifications")
@Builder
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "recipient_id", nullable = false)
    private Long recipientId;

    @Column(name = "sender_id")
    private Long senderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false)
    private NotificationType type;

    @Column(nullable = false, columnDefinition = "VARCHAR(500)")
    private String message;

    @Column(name = "related_entity_id")
    private Long relatedEntityId; // ID của bài viết, bình luận, hoặc người dùng liên quan

    @Column(name = "related_entity_type")
    private String relatedEntityType; // POST, COMMENT, USER, etc.

    @Column(columnDefinition = "VARCHAR(500)")
    private String link;

    @Column(nullable = false)
    private boolean isRead = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Enum cho các loại thông báo
    public enum NotificationType {
        POST_LIKE,           // Like bài viết
        POST_COMMENT,        // Comment bài viết
        POST_SHARE,          // Share bài viết
        COMMENT_LIKE,        // Like comment
        COMMENT_REPLY,       // Reply comment
        FRIENDSHIP_REQUEST,  // Yêu cầu kết bạn
        FRIENDSHIP_ACCEPTED, // Chấp nhận kết bạn
        FRIENDSHIP_REJECTED, // Từ chối kết bạn
        FRIENDSHIP_CANCELLED, // Hủy kết bạn
        MENTION,             // Được mention trong bài viết/comment
        SYSTEM_MESSAGE,      // Thông báo hệ thống
        WELCOME              // Chào mừng người dùng mới
    }
}

