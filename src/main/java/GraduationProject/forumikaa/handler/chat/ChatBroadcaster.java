package GraduationProject.forumikaa.handler.chat;

import GraduationProject.forumikaa.entity.ChatMessage;
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

    // Lưu trữ các subscriber theo roomId
    private final Map<Long, List<Consumer<ChatMessage>>> roomSubscribers = new ConcurrentHashMap<>();

    // Publish message tới tất cả subscriber của room
    public void publish(ChatMessage message) {
        Long roomId = message.getRoomId();
        List<Consumer<ChatMessage>> subscribers = roomSubscribers.get(roomId);
        
        if (subscribers == null || subscribers.isEmpty()) {
            log.debug("Không có subscriber nào cho roomId: {}", roomId);
            return;
        }

        subscribers.forEach(consumer -> {
            try {
                consumer.accept(message);
            } catch (Exception e) {
                log.error("Lỗi khi gửi message tới subscriber: {}", e.getMessage());
            }
        });
    }

    // Đăng ký callback khi có message mới trong room
    public void subscribe(Long roomId, Consumer<ChatMessage> consumer) {
        roomSubscribers.computeIfAbsent(roomId, id -> new CopyOnWriteArrayList<>())
                .add(consumer);
    }

    // Hủy đăng ký
    public void unsubscribe(Long roomId) {
        List<Consumer<ChatMessage>> subscribers = roomSubscribers.remove(roomId);
        if (subscribers != null) {
            log.info("Room {} đã hủy đăng ký chat", roomId);
        }
    }

    // Hủy đăng ký consumer cụ thể
    public void unsubscribe(Long roomId, Consumer<ChatMessage> consumer) {
        List<Consumer<ChatMessage>> subscribers = roomSubscribers.get(roomId);
        if (subscribers != null) {
            boolean removed = subscribers.remove(consumer);
            if (removed) {
                // Nếu không còn subscriber nào, xóa key
                if (subscribers.isEmpty()) {
                    roomSubscribers.remove(roomId);
                }
            }
        }
    }

    // Kiểm tra xem một room có đang subscribe không
    public boolean isSubscribed(Long roomId) {
        List<Consumer<ChatMessage>> subscribers = roomSubscribers.get(roomId);
        return subscribers != null && !subscribers.isEmpty();
    }
}
