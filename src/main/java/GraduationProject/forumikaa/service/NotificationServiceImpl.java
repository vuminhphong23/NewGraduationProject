package GraduationProject.forumikaa.service;

import GraduationProject.forumikaa.dao.NotificationDao;
import GraduationProject.forumikaa.dao.UserDao;
import GraduationProject.forumikaa.dao.CommentDao;
import GraduationProject.forumikaa.entity.Notification;
import GraduationProject.forumikaa.entity.User;
import GraduationProject.forumikaa.handler.notification.NotificationBroadcaster;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.HashMap;
import java.util.Optional;

@Service
@Transactional
public class NotificationServiceImpl implements NotificationService {

    @Autowired private NotificationBroadcaster notificationBroadcaster;
    @Autowired private NotificationDao notificationDao;
    @Autowired private UserDao userDao;
    @Autowired private CommentDao commentDao;

    @Override
    public Notification createNotification(Long recipientId, Long senderId, String message, String link) {
        Notification noti = Notification.builder()
                .senderId(senderId)
                .recipientId(recipientId)
                .message(message)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .link(link)
                .type(Notification.NotificationType.SYSTEM_MESSAGE)
                .build();

        Notification saved = notificationDao.save(noti);
        notificationBroadcaster.publish(recipientId, saved);
        return saved;
    }

    @Override
    public Notification createNotification(Long recipientId, Long senderId, String message) {
        Notification noti = Notification.builder()
                .senderId(senderId)
                .recipientId(recipientId)
                .message(message)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .type(Notification.NotificationType.SYSTEM_MESSAGE)
                .build();

        Notification saved = notificationDao.save(noti);
        notificationBroadcaster.publish(recipientId, saved);
        return saved;
    }

    @Override
    public List<Notification> getUserNotifications(Long userId) {
        return notificationDao.findByRecipientIdOrderByCreatedAtDesc(userId);
    }

    @Override
    public Long getUnreadCount(Long userId) {
        return notificationDao.countUnreadByRecipientId(userId);
    }

    @Override
    public void markAsRead(Long notificationId) {
        notificationDao.markAsReadById(notificationId);
    }

    @Override
    public void markAllAsRead(Long userId) {
        notificationDao.markAllAsReadByRecipientId(userId);
    }

    @Override
    public List<Map<String, Object>> getNotificationDtos(Long userId) {
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
                    dto.put("senderUsername", sender.get().getUsername());
                    
                    // Lấy avatar từ UserProfile
                    String senderAvatar = null;
                    if (sender.get().getUserProfile() != null && sender.get().getUserProfile().getAvatar() != null) {
                        senderAvatar = sender.get().getUserProfile().getAvatar();
                    }
                    dto.put("senderAvatar", senderAvatar);
                } else {
                    dto.put("senderUsername", "Người dùng không tồn tại");
                    dto.put("senderAvatar", null);
                }
            } else {
                dto.put("senderUsername", "Hệ thống");
                dto.put("senderAvatar", null);
            }
            
            return dto;
        }).collect(Collectors.toList());
    }

    // Các method tạo thông báo cụ thể
    @Override
    public Notification createPostLikeNotification(Long postId, Long postAuthorId, Long likerId) {
        Optional<User> liker = userDao.findById(likerId);
        String likerName = liker.map(User::getUsername).orElse("Người dùng");
        
        Notification notification = Notification.builder()
                .recipientId(postAuthorId)
                .senderId(likerId)
                .type(Notification.NotificationType.POST_LIKE)
                .message(likerName + " đã thích bài viết của bạn")
                .relatedEntityId(postId)
                .relatedEntityType("POST")
                .link("/posts/" + postId)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        Notification saved = notificationDao.save(notification);
        notificationBroadcaster.publish(postAuthorId, saved);
        return saved;
    }

    @Override
    public Notification createPostCommentNotification(Long postId, Long postAuthorId, Long commenterId, Long commentId) {
        Optional<User> commenter = userDao.findById(commenterId);
        String commenterName = commenter.map(User::getUsername).orElse("Người dùng");
        
        Notification notification = Notification.builder()
                .recipientId(postAuthorId)
                .senderId(commenterId)
                .type(Notification.NotificationType.POST_COMMENT)
                .message(commenterName + " đã bình luận bài viết của bạn")
                .relatedEntityId(postId)
                .relatedEntityType("POST")
                .link("/posts/" + postId + "#comment-" + commentId)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        Notification saved = notificationDao.save(notification);
        notificationBroadcaster.publish(postAuthorId, saved);
        return saved;
    }

    @Override
    public Notification createCommentLikeNotification(Long commentId, Long commentAuthorId, Long likerId) {
        Optional<User> liker = userDao.findById(likerId);
        String likerName = liker.map(User::getUsername).orElse("Người dùng");
        
        // Lấy postId từ comment để tạo link đúng
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
        Optional<User> replier = userDao.findById(replierId);
        String replierName = replier.map(User::getUsername).orElse("Người dùng");
        
        // Lấy postId từ comment để tạo link đúng
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
        Optional<User> sender = userDao.findById(senderId);
        String senderName = sender.map(User::getUsername).orElse("Người dùng");
        
        Notification notification = Notification.builder()
                .recipientId(recipientId)
                .senderId(senderId)
                .type(Notification.NotificationType.FRIENDSHIP_REQUEST)
                .message(senderName + " đã gửi lời mời kết bạn")
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
    public Notification createFriendshipAcceptedNotification(Long recipientId, Long senderId) {
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
        Optional<User> mentioner = userDao.findById(mentionerId);
        String mentionerName = mentioner.map(User::getUsername).orElse("Người dùng");
        
        String message = mentionerName + " đã nhắc đến bạn trong " + 
                        (entityType.equals("POST") ? "bài viết" : "bình luận");
        
        Notification notification = Notification.builder()
                .recipientId(mentionedUserId)
                .senderId(mentionerId)
                .type(Notification.NotificationType.MENTION)
                .message(message)
                .relatedEntityId(entityId)
                .relatedEntityType(entityType)
                .link("/" + entityType.toLowerCase() + "s/" + entityId)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        Notification saved = notificationDao.save(notification);
        notificationBroadcaster.publish(mentionedUserId, saved);
        return saved;
    }

    @Override
    public Notification createSystemNotification(Long userId, String message) {
        Notification notification = Notification.builder()
                .recipientId(userId)
                .senderId(null)
                .type(Notification.NotificationType.SYSTEM_MESSAGE)
                .message(message)
                .relatedEntityId(null)
                .relatedEntityType(null)
                .link(null)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        Notification saved = notificationDao.save(notification);
        notificationBroadcaster.publish(userId, saved);
        return saved;
    }

    @Override
    public Notification createWelcomeNotification(Long userId) {
        Notification notification = Notification.builder()
                .recipientId(userId)
                .senderId(null)
                .type(Notification.NotificationType.WELCOME)
                .message("Chào mừng bạn đến với Forumikaa! 🎉")
                .relatedEntityId(null)
                .relatedEntityType(null)
                .link("/profile")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        Notification saved = notificationDao.save(notification);
        notificationBroadcaster.publish(userId, saved);
        return saved;
    }
}
