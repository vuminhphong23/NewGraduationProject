package GraduationProject.forumikaa.handler.notification;

import GraduationProject.forumikaa.entity.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

@Slf4j
@Component
public class NotificationBroadcaster {

    private final Map<Long, List<Consumer<Notification>>> subscribers = new ConcurrentHashMap<>();

    // Publish notification tới tất cả subscriber theo recipient
    public void publish(Long recipientId, Notification notification) {
        List<Consumer<Notification>> recipientSubscribers = subscribers.get(recipientId);
        if (recipientSubscribers == null || recipientSubscribers.isEmpty()) {
            log.debug("Không có subscriber nào cho recipientId: {}", recipientId);
            return;
        }

        recipientSubscribers.forEach(consumer -> {
            try {
                consumer.accept(notification);
            } catch (Exception e) {
                log.error("Lỗi khi gửi notification tới subscriber: {}", e.getMessage());
            }
        });
    }

    // Đăng ký callback khi có notification mới
    public void subscribe(Long recipientId, Consumer<Notification> consumer) {
        subscribers.computeIfAbsent(recipientId, id -> new CopyOnWriteArrayList<>())
                .add(consumer);
    }

    // Hủy đăng ký
    public void unsubscribe(Long recipientId) {
        List<Consumer<Notification>> recipientSubscribers = subscribers.remove(recipientId);
        if (recipientSubscribers != null) {
            log.info("User {} đã hủy đăng ký notification", recipientId);
        }
    }

//    // Hủy đăng ký consumer cụ thể
//    public void unsubscribe(Long recipientId, Consumer<Notification> consumer) {
//        List<Consumer<Notification>> recipientSubscribers = subscribers.get(recipientId);
//        if (recipientSubscribers != null) {
//            boolean removed = recipientSubscribers.remove(consumer);
//            if (removed) {
//                // Nếu không còn subscriber nào, xóa key
//                if (recipientSubscribers.isEmpty()) {
//                    subscribers.remove(recipientId);
//                }
//            }
//        }
//    }
//
//
//    // Kiểm tra xem một recipient có đang subscribe không
//    public boolean isSubscribed(Long recipientId) {
//        List<Consumer<Notification>> recipientSubscribers = subscribers.get(recipientId);
//        return recipientSubscribers != null && !recipientSubscribers.isEmpty();
//    }
}
