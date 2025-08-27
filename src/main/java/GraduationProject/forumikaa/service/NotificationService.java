package GraduationProject.forumikaa.service;

import GraduationProject.forumikaa.entity.Notification;
import GraduationProject.forumikaa.entity.User;

import java.util.List;
import java.util.Map;

public interface NotificationService {
    Notification createNotification(Long recipientId, Long senderId, String message, String link);
    Notification createNotification(Long recipientId, Long senderId, String message);
    List<Notification> getUserNotifications(Long userId);
    Long getUnreadCount(Long userId);
    void markAsRead(Long notificationId);
    void markAllAsRead(Long userId);
    List<Map<String, Object>> getNotificationDtos(Long userId);
}

