package GraduationProject.forumikaa.patterns.singleton;

import GraduationProject.forumikaa.patterns.singleton.NotificationServiceImpl;
import GraduationProject.forumikaa.dao.NotificationDao;
import GraduationProject.forumikaa.dao.UserDao;
import GraduationProject.forumikaa.dao.CommentDao;
import GraduationProject.forumikaa.handler.notification.NotificationBroadcaster;
import GraduationProject.forumikaa.patterns.adapter.NotificationServiceWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * Configuration class để khởi tạo NotificationService singleton
 * Được gọi sau khi Spring context được khởi tạo
 */
@Configuration
public class NotificationConfig {
    
    @Autowired
    private NotificationBroadcaster notificationBroadcaster;
    
    @Autowired
    private NotificationDao notificationDao;
    
    @Autowired
    private UserDao userDao;
    
    @Autowired
    private CommentDao commentDao;
    
    /**
     * Khởi tạo NotificationService singleton sau khi Spring context được tạo
     * Method này sẽ được gọi tự động sau khi tất cả beans được khởi tạo
     * Sử dụng Adapter Pattern thay vì truy cập trực tiếp Singleton
     */
    @PostConstruct
    public void initializeNotificationService() {
        try {
            // Lấy instance của NotificationServiceWrapper đã được Spring quản lý
            // Thay vì tạo mới, chúng ta sẽ inject nó
            System.out.println("🔄 NotificationConfig: Bắt đầu khởi tạo NotificationService singleton...");
            
            // Lấy singleton instance trực tiếp
            NotificationServiceImpl singleton = NotificationServiceImpl.getInstance();
            
            // Khởi tạo singleton với dependencies thật
            singleton.initialize(notificationBroadcaster, notificationDao, userDao, commentDao);
            
            // Kiểm tra xem singleton đã được khởi tạo thành công chưa
            if (singleton.isInitialized()) {
                System.out.println("✅ NotificationService singleton đã được khởi tạo thành công!");
                System.out.println("📧 Sẵn sàng xử lý các loại notification: POST_LIKE, COMMENT_REPLY, FRIENDSHIP_REQUEST, etc.");
                System.out.println("🔧 Dependencies: " + 
                    "NotificationBroadcaster=" + (notificationBroadcaster != null) + ", " +
                    "NotificationDao=" + (notificationDao != null) + ", " +
                    "UserDao=" + (userDao != null) + ", " +
                    "CommentDao=" + (commentDao != null));
            } else {
                System.err.println("❌ NotificationService singleton khởi tạo thất bại!");
            }
            
        } catch (Exception e) {
            System.err.println("❌ NotificationConfig: Lỗi khi khởi tạo NotificationService singleton: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
