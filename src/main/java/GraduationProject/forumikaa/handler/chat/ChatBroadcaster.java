package GraduationProject.forumikaa.handler.chat;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

@Slf4j
@Component
public class ChatBroadcaster {

    private final Map<Long, List<Consumer<Map<String, Object>>>> roomSubscribers = new ConcurrentHashMap<>();

    // Publish message tới tất cả subscriber trong room
    public void publishMessage(Long roomId, Map<String, Object> message) {
        List<Consumer<Map<String, Object>>> roomSubscribersList = roomSubscribers.get(roomId);
        if (roomSubscribersList == null || roomSubscribersList.isEmpty()) {
            log.debug("Không có subscriber nào cho roomId: {}", roomId);
            return;
        }

        roomSubscribersList.forEach(consumer -> {
            try {
                consumer.accept(message);
            } catch (Exception e) {
                log.error("Lỗi khi gửi message tới subscriber: {}", e.getMessage());
            }
        });
    }

    // Publish message read status tới tất cả subscriber trong room
    public void publishMessageRead(Long roomId, Long userId) {
        Map<String, Object> message = Map.of(
            "type", "MESSAGE_READ",
            "roomId", roomId,
            "userId", userId,
            "timestamp", System.currentTimeMillis()
        );
        publishMessage(roomId, message);
    }


    // Publish new message tới tất cả subscriber trong room
    public void publishNewMessage(Long roomId, Map<String, Object> messageData) {
        Map<String, Object> message = Map.of(
            "type", "NEW_MESSAGE",
            "roomId", roomId,
            "message", messageData,
            "timestamp", System.currentTimeMillis()
        );
        publishMessage(roomId, message);
    }

    // Đăng ký callback khi có message mới trong room
    public void subscribe(Long roomId, Consumer<Map<String, Object>> consumer) {
        roomSubscribers.computeIfAbsent(roomId, id -> new CopyOnWriteArrayList<>())
                .add(consumer);
    }

    // Hủy đăng ký
    public void unsubscribe(Long roomId) {
        List<Consumer<Map<String, Object>>> roomSubscribersList = roomSubscribers.remove(roomId);
        if (roomSubscribersList != null) {
            log.info("Room {} đã hủy đăng ký chat", roomId);
        }
    }

    // Kiểm tra xem một room có đang subscribe không
    public boolean isSubscribed(Long roomId) {
        List<Consumer<Map<String, Object>>> roomSubscribersList = roomSubscribers.get(roomId);
        return roomSubscribersList != null && !roomSubscribersList.isEmpty();
    }
}
