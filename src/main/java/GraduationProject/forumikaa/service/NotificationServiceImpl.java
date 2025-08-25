package GraduationProject.forumikaa.service;

import GraduationProject.forumikaa.dao.NotificationDao;
import GraduationProject.forumikaa.entity.Notification;
import GraduationProject.forumikaa.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.HashMap;

@Service
@Transactional
public class NotificationServiceImpl implements NotificationService {

    @Autowired private NotificationDao notificationDao;

    @Override
    public Notification createNotification(User recipient, User sender, String message, String link) {
        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setSender(sender);
        notification.setMessage(message);
        notification.setLink(link);
        return notificationDao.save(notification);
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
            dto.put("senderUsername", n.getSender() != null ? n.getSender().getUsername() : "System");
            
            // Lấy avatar của sender
            String senderAvatar = null;
            if (n.getSender() != null && n.getSender().getUserProfile() != null) {
                senderAvatar = n.getSender().getUserProfile().getAvatar();
            }
            dto.put("senderAvatar", senderAvatar);
            
            return dto;
        }).collect(Collectors.toList());
    }
}
