package GraduationProject.forumikaa.handler.notification;

import GraduationProject.forumikaa.dao.NotificationDao;
import GraduationProject.forumikaa.dao.UserDao;
import GraduationProject.forumikaa.entity.Notification;
import GraduationProject.forumikaa.entity.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationWebSocketHandler extends TextWebSocketHandler {

    private final NotificationDao notificationDao;
    private final NotificationBroadcaster broadcaster;
    private final UserDao userDao;
    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private final Map<Long, WebSocketSession> userSessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long recipientId = extractRecipientId(session);
        if (recipientId == null) {
            log.error("Không thể lấy recipientId từ session: {}", session.getId());
            session.close();
            return;
        }

        // Lưu session
        userSessions.put(recipientId, session);

        // Gửi lại các thông báo cũ (chỉ 10 thông báo gần nhất)
        List<Notification> oldNotifications = notificationDao.findByRecipientId(recipientId);
        if (oldNotifications.size() > 10) {
            oldNotifications = oldNotifications.subList(0, 10);
        }

        for (Notification notification : oldNotifications) {
            try {
                String message = createNotificationMessage("NOTIFICATION", notification);
                session.sendMessage(new TextMessage(message));
            } catch (Exception e) {
                log.error("Lỗi gửi thông báo cũ: {}", e.getMessage());
            }
        }

        // Gửi số lượng chưa đọc
        Long unreadCount = notificationDao.countUnreadByRecipientId(recipientId);
        String unreadMessage = createUnreadCountMessage(unreadCount);
        session.sendMessage(new TextMessage(unreadMessage));

        // Đăng ký nhận thông báo mới
        broadcaster.subscribe(recipientId, notification -> {
            try {
                WebSocketSession userSession = userSessions.get(recipientId);
                if (userSession != null && userSession.isOpen()) {
                    String message = createNotificationMessage("NOTIFICATION", notification);
                    userSession.sendMessage(new TextMessage(message));
                    
                    // Cập nhật số lượng chưa đọc
                    Long newUnreadCount = notificationDao.countUnreadByRecipientId(recipientId);
                    String unreadUpdateMessage = createUnreadCountMessage(newUnreadCount);
                    userSession.sendMessage(new TextMessage(unreadUpdateMessage));
                }
            } catch (Exception e) {
                log.error("Lỗi gửi thông báo mới tới user {}: {}", recipientId, e.getMessage());
            }
        });
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            String payload = message.getPayload();
            if (payload == null || payload.trim().isEmpty()) return;

            Map<String, Object> data = mapper.readValue(payload, Map.class);
            String type = (String) data.get("type");
            Long userId = extractRecipientId(session);

            if (userId == null) {
                log.error("Không thể lấy userId từ session");
                return;
            }

            switch (type) {
                case "CONNECT":
                    log.info("User {} xác nhận kết nối WebSocket", userId);
                    break;
                    
                case "MARK_AS_READ":
                    Long notificationId = Long.valueOf(data.get("notificationId").toString());
                    handleMarkAsRead(notificationId, userId);
                    break;
                    
                case "MARK_ALL_AS_READ":
                    handleMarkAllAsRead(userId);
                    break;
                    
                case "FRIENDSHIP_ACCEPTED":
                    handleFriendshipAccepted(data, userId);
                    break;
                    
                case "FRIENDSHIP_REJECTED":
                    handleFriendshipRejected(data, userId);
                    break;
                    
                case "FRIENDSHIP_CANCELLED":
                    handleFriendshipCancelled(data, userId);
                    break;
                    
                default:
                    log.warn("Loại message không xác định: {}", type);
            }
            
        } catch (Exception e) {
            log.error("Lỗi xử lý WebSocket message: {}", e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus status) throws Exception {
        Long recipientId = extractRecipientId(session);
        if (recipientId != null) {
            userSessions.remove(recipientId);
            broadcaster.unsubscribe(recipientId);
        }
    }

    private void handleMarkAsRead(Long notificationId, Long userId) {
        try {
            notificationDao.markAsReadById(notificationId);
            log.info("User {} đã đánh dấu notification {} là đã đọc", userId, notificationId);
            
            // Gửi cập nhật số lượng chưa đọc
            Long unreadCount = notificationDao.countUnreadByRecipientId(userId);
            WebSocketSession userSession = userSessions.get(userId);
            if (userSession != null && userSession.isOpen()) {
                String unreadMessage = createUnreadCountMessage(unreadCount);
                userSession.sendMessage(new TextMessage(unreadMessage));
            }
        } catch (Exception e) {
            log.error("Lỗi đánh dấu đã đọc notification {}: {}", notificationId, e.getMessage());
        }
    }

    private void handleMarkAllAsRead(Long userId) {
        try {
            notificationDao.markAllAsReadByRecipientId(userId);
            log.info("User {} đã đánh dấu tất cả notification là đã đọc", userId);
            
            // Gửi cập nhật số lượng chưa đọc
            WebSocketSession userSession = userSessions.get(userId);
            if (userSession != null && userSession.isOpen()) {
                String unreadMessage = createUnreadCountMessage(0L);
                userSession.sendMessage(new TextMessage(unreadMessage));
            }
        } catch (Exception e) {
            log.error("Lỗi đánh dấu tất cả đã đọc cho user {}: {}", userId, e.getMessage());
        }
    }

    private void handleFriendshipAccepted(Map<String, Object> data, Long userId) {
        try {
            Long senderId = Long.valueOf(data.get("senderId").toString());
            Long notificationId = data.get("notificationId") != null ? 
                Long.valueOf(data.get("notificationId").toString()) : null;
            
            // Gửi thông báo cập nhật trạng thái friendship
            Map<String, Object> message = Map.of(
                "type", "FRIENDSHIP_STATUS_UPDATE",
                "notificationId", notificationId,
                "senderId", senderId,
                "action", "accepted",
                "friendshipStatus", "ACCEPTED"
            );
            
            WebSocketSession userSession = userSessions.get(userId);
            if (userSession != null && userSession.isOpen()) {
                userSession.sendMessage(new TextMessage(mapper.writeValueAsString(message)));
            }
        } catch (Exception e) {
            log.error("Lỗi xử lý friendship accepted: {}", e.getMessage());
        }
    }

    private void handleFriendshipRejected(Map<String, Object> data, Long userId) {
        try {
            Long senderId = Long.valueOf(data.get("senderId").toString());
            Long notificationId = data.get("notificationId") != null ? 
                Long.valueOf(data.get("notificationId").toString()) : null;
            
            // Gửi thông báo cập nhật trạng thái friendship
            Map<String, Object> message = Map.of(
                "type", "FRIENDSHIP_STATUS_UPDATE",
                "notificationId", notificationId,
                "senderId", senderId,
                "action", "rejected",
                "friendshipStatus", "REJECTED"
            );
            
            WebSocketSession userSession = userSessions.get(userId);
            if (userSession != null && userSession.isOpen()) {
                userSession.sendMessage(new TextMessage(mapper.writeValueAsString(message)));
            }
        } catch (Exception e) {
            log.error("Lỗi xử lý friendship rejected: {}", e.getMessage());
        }
    }

    private void handleFriendshipCancelled(Map<String, Object> data, Long userId) {
        try {
            Long senderId = Long.valueOf(data.get("senderId").toString());
            
            // Gửi thông báo cập nhật trạng thái friendship
            Map<String, Object> message = Map.of(
                "type", "FRIENDSHIP_STATUS_UPDATE",
                "senderId", senderId,
                "action", "cancelled",
                "friendshipStatus", "CANCELLED"
            );
            
            WebSocketSession userSession = userSessions.get(userId);
            if (userSession != null && userSession.isOpen()) {
                userSession.sendMessage(new TextMessage(mapper.writeValueAsString(message)));
            }
        } catch (Exception e) {
            log.error("Lỗi xử lý friendship cancelled: {}", e.getMessage());
        }
    }

    private Long extractRecipientId(WebSocketSession session) {
        try {
            String query = session.getUri() != null ? session.getUri().getQuery() : null;
            if (query == null || query.isEmpty()) {
                log.error("Thiếu query parameters trong WebSocket URI");
                return null;
            }

            String[] params = query.split("&");
            for (String param : params) {
                String[] kv = param.split("=", 2);
                if (kv.length == 2 && "recipientId".equals(kv[0])) {
                    try {
                        return Long.parseLong(kv[1]);
                    } catch (NumberFormatException e) {
                        log.error("recipientId không hợp lệ: {}", kv[1]);
                        return null;
                    }
                }
            }
            log.error("Không tìm thấy recipientId trong query parameters");
            return null;
        } catch (Exception e) {
            log.error("Lỗi extract recipientId: {}", e.getMessage());
            return null;
        }
    }

    private String createNotificationMessage(String type, Notification notification) {
        try {
            LocalDateTime createdAt = notification.getCreatedAt();
            long createdAtMillis = createdAt != null 
                ? createdAt.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                : 0L;
            
            // Lấy thông tin sender
            String senderUsername = "Hệ thống";
            String senderAvatar = null;
            if (notification.getSenderId() != null) {
                Optional<User> sender = userDao.findById(notification.getSenderId());
                if (sender.isPresent()) {
                    senderUsername = sender.get().getUsername();
                    if (sender.get().getUserProfile() != null && sender.get().getUserProfile().getAvatar() != null) {
                        senderAvatar = sender.get().getUserProfile().getAvatar();
                    }
                }
            }
                
            Map<String, Object> notificationData = new HashMap<>();
            notificationData.put("id", notification.getId());
            notificationData.put("message", notification.getMessage());
            notificationData.put("senderId", notification.getSenderId());
            notificationData.put("senderUsername", senderUsername);
            notificationData.put("senderAvatar", senderAvatar);
            notificationData.put("recipientId", notification.getRecipientId());
            notificationData.put("createdAt", createdAtMillis);
            notificationData.put("isRead", notification.isRead());
            notificationData.put("link", notification.getLink());
            notificationData.put("notificationType", notification.getType());
            notificationData.put("relatedEntityId", notification.getRelatedEntityId());
            notificationData.put("relatedEntityType", notification.getRelatedEntityType());
            
            Map<String, Object> message = new HashMap<>();
            message.put("type", type);
            message.put("notification", notificationData);
            
            return mapper.writeValueAsString(message);
        } catch (Exception e) {
            log.error("Lỗi tạo notification message: {}", e.getMessage());
            return "{}";
        }
    }

    private String createUnreadCountMessage(Long count) {
        try {
            Map<String, Object> message = Map.of(
                "type", "UNREAD_COUNT_UPDATE",
                "count", count
            );
            
            return mapper.writeValueAsString(message);
        } catch (Exception e) {
            log.error("Lỗi tạo unread count message: {}", e.getMessage());
            return "{}";
        }
    }
}
