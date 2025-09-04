package GraduationProject.forumikaa.patterns.factory;

import GraduationProject.forumikaa.entity.Notification;

/**
 * NotificationFactory interface - Factory Method Pattern
 * Định nghĩa contract để tạo các loại notification khác nhau
 */
public interface NotificationFactory {
    
    /**
     * Factory method để tạo notification
     * @return Notification object
     */
    Notification createNotification();
    
    /**
     * Factory method để tạo notification với message tùy chỉnh
     * @param message Message tùy chỉnh
     * @return Notification object
     */
    Notification createNotification(String message);
}
