package GraduationProject.forumikaa.service;

import GraduationProject.forumikaa.dao.NotificationDao;
import GraduationProject.forumikaa.entity.Notification;
import GraduationProject.forumikaa.handler.notification.NotificationBroadcaster;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.HashMap;

@Service
@Transactional
public class NotificationServiceImpl implements NotificationService {

    @Autowired private NotificationBroadcaster notificationBroadcaster;
    @Autowired private NotificationDao notificationDao;

    @Override
    public Notification createNotification(Long recipientId, Long senderId, String message, String link) {
        Notification noti = Notification.builder()
                .senderId(senderId)
                .recipientId(recipientId)
                .message(message)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .link(link)
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
            dto.put("senderUsername", n.getSenderId() != null ? "User " + n.getSenderId() : "System");
            
            // Lấy avatar của sender - cần query User từ sender ID
            String senderAvatar = null;
            // TODO: Implement if needed - query User by sender ID
            dto.put("senderAvatar", senderAvatar);
            
            return dto;
        }).collect(Collectors.toList());
    }
}
