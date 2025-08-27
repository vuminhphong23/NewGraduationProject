package GraduationProject.forumikaa.handler.notification;

import GraduationProject.forumikaa.entity.Notification;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

@Component
public class NotificationBroadcaster {

    private final Map<Long, List<Consumer<Notification>>> subscribers = new ConcurrentHashMap<>();

    // Publish notification tới tất cả subscriber theo recipient
    public void publish(Long recipientId, Notification notification) {
        List<Consumer<Notification>> recipientSubscribers = subscribers.get(recipientId);
        if (recipientSubscribers == null || recipientSubscribers.isEmpty()) {
            return;
        }
        
        System.out.println("✅ Found " + recipientSubscribers.size() + " subscribers for recipientId: " + recipientId);
        
        recipientSubscribers.forEach(consumer -> {
            try {
                consumer.accept(notification);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    // Đăng ký callback khi có notification mới
    public void subscribe(Long recipientId, Consumer<Notification> consumer) {
        subscribers.computeIfAbsent(recipientId, id -> new CopyOnWriteArrayList<>())
                .add(consumer);
    }
}
