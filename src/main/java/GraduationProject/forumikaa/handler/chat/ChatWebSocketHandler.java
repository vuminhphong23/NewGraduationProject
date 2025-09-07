package GraduationProject.forumikaa.handler.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final ChatBroadcaster chatBroadcaster;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    
    // Lưu trữ các WebSocket session theo userId
    private final Map<Long, WebSocketSession> userSessions = new ConcurrentHashMap<>();
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // Lấy userId từ query parameter (giống như NotificationWebSocketHandler)
        Long userId = extractUserId(session);
        if (userId != null) {
            userSessions.put(userId, session);
            
            // Gửi thông báo kết nối thành công
            sendMessage(session, Map.of(
                "type", "CONNECTION_ESTABLISHED",
                "message", "Connected to chat WebSocket",
                "userId", userId
            ));
        } else {
            session.close(CloseStatus.BAD_DATA.withReason("No userId provided"));
        }
    }
    
    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        if (message instanceof TextMessage) {
            String payload = ((TextMessage) message).getPayload();
            
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> messageData = objectMapper.readValue(payload, Map.class);
                String type = (String) messageData.get("type");
                
                switch (type) {
                    case "JOIN_ROOM":
                        handleJoinRoom(session, messageData);
                        break;
                    case "LEAVE_ROOM":
                        handleLeaveRoom(session, messageData);
                        break;
                    case "MESSAGE_READ":
                        handleMessageRead(session, messageData);
                        break;
                }
            } catch (Exception e) {
                // Error processing message
            }
        }
    }
    
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        // Handle transport error
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        // Xóa session khỏi userSessions
        Long userId = extractUserId(session);
        if (userId != null) {
            userSessions.remove(userId);
        }
    }
    
    @Override
    public boolean supportsPartialMessages() {
        return false;
    }
    
    // Xử lý join room
    private void handleJoinRoom(WebSocketSession session, Map<String, Object> messageData) {
        Long userId = extractUserId(session);
        Long roomId = ((Number) messageData.get("roomId")).longValue();
        
        // Subscribe user to room events
        if (userId != null) {
            subscribeToRoom(userId, roomId);
        }
    }
    
    // Xử lý leave room
    private void handleLeaveRoom(WebSocketSession session, Map<String, Object> messageData) {
        Long userId = extractUserId(session);
        Long roomId = ((Number) messageData.get("roomId")).longValue();
        
        // Unsubscribe user from room events
        if (userId != null) {
            unsubscribeFromRoom(roomId);
        }
    }
    
    // Xử lý message read
    private void handleMessageRead(WebSocketSession session, Map<String, Object> messageData) {
        Long userId = extractUserId(session);
        Long roomId = ((Number) messageData.get("roomId")).longValue();
        
        // Broadcast cho các user khác trong room rằng user này đã đọc tin nhắn
        if (userId != null) {
            chatBroadcaster.publishMessageRead(roomId, userId);
        }
    }
    
    
    // Gửi message đến session
    private void sendMessage(WebSocketSession session, Map<String, Object> message) {
        try {
            if (session.isOpen()) {
                String json = objectMapper.writeValueAsString(message);
                session.sendMessage(new TextMessage(json));
            }
        } catch (Exception e) {
            // Error sending message
        }
    }
    
    // Gửi message đến user cụ thể
    public void sendToUser(Long userId, Map<String, Object> message) {
        WebSocketSession session = userSessions.get(userId);
        if (session != null && session.isOpen()) {
            sendMessage(session, message);
        }
    }
    
    // Gửi message đến tất cả user trong room
    public void sendToRoom(Long roomId, Map<String, Object> messageData, Long excludeUserId) {
        // Tạo message với format đúng
        Map<String, Object> message = Map.of(
            "type", "NEW_MESSAGE",
            "roomId", roomId,
            "message", messageData,
            "timestamp", System.currentTimeMillis()
        );
        
        // Gửi đến tất cả user online (trong thực tế cần filter theo room membership)
        userSessions.entrySet().stream()
                .filter(entry -> !entry.getKey().equals(excludeUserId)) // Loại trừ người gửi
                .filter(entry -> entry.getValue().isOpen())
                .forEach(entry -> {
                    try {
                        sendMessage(entry.getValue(), message);
                    } catch (Exception e) {
                        // Error sending message to user
                    }
                });
    }
    
    // Kiểm tra user có online không
    public boolean isUserOnline(Long userId) {
        WebSocketSession session = userSessions.get(userId);
        return session != null && session.isOpen();
    }
    
    // Lấy số lượng user online
    public int getOnlineUserCount() {
        return (int) userSessions.values().stream()
                .filter(WebSocketSession::isOpen)
                .count();
    }
    
    // Subscribe user to room events
    public void subscribeToRoom(Long userId, Long roomId) {
        chatBroadcaster.subscribe(roomId, message -> {
            WebSocketSession session = userSessions.get(userId);
            if (session != null && session.isOpen()) {
                sendMessage(session, message);
            }
        });
    }
    
    // Unsubscribe user from room events
    public void unsubscribeFromRoom(Long roomId) {
        chatBroadcaster.unsubscribe(roomId);
    }
    
    // Gửi message đến tất cả user (public method)
    public void broadcastToAllUsers(Map<String, Object> message, Long excludeUserId) {
        userSessions.entrySet().stream()
                .filter(entry -> !entry.getKey().equals(excludeUserId)) // Loại trừ người gửi
                .filter(entry -> entry.getValue().isOpen())
                .forEach(entry -> {
                    try {
                        sendMessage(entry.getValue(), message);
                    } catch (Exception e) {
                        // Error sending message to user
                    }
                });
    }
    
    // Extract userId from WebSocket session query parameters
    private Long extractUserId(WebSocketSession session) {
        try {
            String query = session.getUri() != null ? session.getUri().getQuery() : null;
            if (query == null || query.isEmpty()) {
                return null;
            }

            String[] params = query.split("&");
            for (String param : params) {
                String[] kv = param.split("=", 2);
                if (kv.length == 2 && "userId".equals(kv[0])) {
                    try {
                        return Long.parseLong(kv[1]);
                    } catch (NumberFormatException e) {
                        return null;
                    }
                }
            }
            
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}