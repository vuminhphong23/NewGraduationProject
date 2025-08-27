package GraduationProject.forumikaa.handler.notification;

import GraduationProject.forumikaa.dao.NotificationDao;
import GraduationProject.forumikaa.entity.Notification;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class NotificationWebSocketHandler extends TextWebSocketHandler {

    private final NotificationDao notificationDao;
    private final NotificationBroadcaster broadcaster;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long recipientId = extractRecipientId(session);

        // Gửi lại các thông báo cũ
        List<Notification> oldNotifications = notificationDao.findByRecipientId(recipientId);

        oldNotifications.forEach(n -> {
            try {
                String message = formatNotification(n);
                session.sendMessage(new TextMessage(message));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        // Đăng ký nhận thông báo mới
        broadcaster.subscribe(recipientId, n -> {
            try {
                String message = formatNotification(n);
                session.sendMessage(new TextMessage(message));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private Long extractRecipientId(WebSocketSession session) {
        String query = session.getUri() != null ? session.getUri().getQuery() : null;
        if (query == null || query.isEmpty()) {
            throw new IllegalArgumentException("Thiếu recipientId trong query!");
        }

        String[] params = query.split("&");
        for (String p : params) {
            String[] kv = p.split("=", 2);
            if (kv.length == 2 && "recipientId".equals(kv[0])) {
                try {
                    return Long.parseLong(kv[1]);
                } catch (NumberFormatException ignored) { /* continue */ }
            }
        }
        throw new IllegalArgumentException("recipientId không hợp lệ hoặc thiếu!");
    }

    private String formatNotification(Notification n) {
        LocalDateTime createdAt = n.getCreatedAt();
        long createdAtMillis = createdAt != null 
            ? createdAt.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
            : 0L;
            
        return String.format(
                "{\"id\":%d,\"message\":\"%s\",\"senderId\":%d,\"recipientId\":%d,\"createdAt\":%d,\"read\":%b}",
                n.getId(),
                escapeJson(n.getMessage()),
                n.getSenderId(),
                n.getRecipientId(),
                createdAtMillis,
                n.isRead()
        );
    }

    private String escapeJson(String str) {
        return str.replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
