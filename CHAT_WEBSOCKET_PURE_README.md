# Chat WebSocket System - WebSocket Thuần

## Tổng quan

Hệ thống chat WebSocket được xây dựng với Spring WebSocket thuần (không STOMP), hỗ trợ chat 1-1 và chat nhóm real-time với DTO để tránh lazy loading issues.

## Kiến trúc

### Backend Components

1. **DTO Classes** - Tránh lazy loading issues:
   - `ChatRoomDto` - Thông tin chat room
   - `ChatMessageDto` - Thông tin tin nhắn
   - `ChatRoomMemberDto` - Thông tin thành viên
   - `WebSocketMessageDto` - Message cho WebSocket

2. **WebSocket Configuration**:
   - `WebSocketConfig` - Cấu hình WebSocket thuần
   - Endpoint: `/ws/chat?userId={id}`
   - Handler: `ChatWebSocketHandler`

3. **WebSocket Handler**:
   - `ChatWebSocketHandler` - Xử lý các WebSocket events
   - Message types: JOIN_ROOM, LEAVE_ROOM, SEND_MESSAGE, TYPING, STOP_TYPING, MESSAGE_READ
   - Session management: Lưu trữ user sessions và current rooms

4. **Chat Broadcaster**:
   - `ChatBroadcaster` - Publish/Subscribe pattern cho room messages
   - Hỗ trợ DTO messages
   - Thread-safe với ConcurrentHashMap

5. **Service Layer**:
   - `ChatService` - Interface với DTO methods
   - `ChatServiceImpl` - Implementation sử dụng DTO
   - `ChatMapper` - Convert Entity ↔ DTO

### Frontend Components

1. **ChatWebSocketHandler** - JavaScript class:
   - Kết nối WebSocket thuần
   - Xử lý real-time messages
   - Typing indicators
   - Notification system
   - Auto-reconnection với exponential backoff

## WebSocket Connection

### Backend Endpoint
```
ws://localhost:8080/ws/chat?userId={userId}
```

### Frontend Connection
```javascript
const wsUrl = `ws://${window.location.host}/ws/chat?userId=${this.currentUserId}`;
this.websocket = new WebSocket(wsUrl);
```

## Message Protocol

### Message Format
```json
{
  "type": "MESSAGE_TYPE",
  "roomId": 123,
  "userId": 456,
  "username": "john_doe",
  "content": "Hello world",
  "message": { /* ChatMessageDto */ },
  "data": { /* Additional data */ },
  "timestamp": "2024-01-01T12:00:00"
}
```

### Message Types

#### Client → Server
- `JOIN_ROOM` - Join vào room
- `LEAVE_ROOM` - Leave khỏi room
- `SEND_MESSAGE` - Gửi tin nhắn
- `TYPING` - Đang typing
- `STOP_TYPING` - Dừng typing
- `MESSAGE_READ` - Đánh dấu đã đọc
- `PING` - Heartbeat

#### Server → Client
- `CONNECTED` - Kết nối thành công
- `MESSAGE` - Tin nhắn mới
- `TYPING` - User đang typing
- `STOP_TYPING` - User dừng typing
- `USER_ONLINE` - User online
- `USER_OFFLINE` - User offline
- `MESSAGE_READ` - Tin nhắn đã đọc
- `ROOM_UPDATE` - Cập nhật room
- `ERROR` - Lỗi
- `PONG` - Heartbeat response

## API Usage

### Frontend JavaScript

#### Khởi tạo
```javascript
const chatManager = new ChatManager();
const wsHandler = new ChatWebSocketHandler(chatManager);
```

#### Join Room
```javascript
wsHandler.joinRoom(roomId);
```

#### Gửi Tin Nhắn
```javascript
wsHandler.sendMessage(roomId, content, 'TEXT');
```

#### Typing Indicator
```javascript
wsHandler.sendTyping(roomId);
wsHandler.sendStopTyping(roomId);
```

#### Mark as Read
```javascript
wsHandler.markMessageAsRead(roomId, messageId);
```

#### Event Handling
```javascript
// Xử lý tin nhắn mới
function handleChatMessage(data) {
    const message = data.message;
    addMessageToUI(message);
    
    if (currentRoomId !== message.roomId) {
        showNotification(message);
    }
}

// Xử lý typing indicator
function handleTypingIndicator(data) {
    if (data.userId !== currentUserId) {
        showTypingIndicator(data.username);
    }
}
```

### Backend Java

#### WebSocket Handler Methods
```java
// Join room
private void handleJoinRoom(WebSocketSession session, Long userId, WebSocketMessageDto message)

// Send message
private void handleSendMessage(WebSocketSession session, Long userId, WebSocketMessageDto message)

// Typing indicator
private void handleTyping(WebSocketSession session, Long userId, WebSocketMessageDto message)

// Broadcast to room
private void broadcastToRoom(Long roomId, WebSocketMessageDto message)
```

#### Public API Methods
```java
// Gửi message đến user cụ thể
public void sendMessageToUser(Long userId, WebSocketMessageDto message)

// Broadcast đến room
public void broadcastToRoomPublic(Long roomId, WebSocketMessageDto message)

// Kiểm tra user online
public boolean isUserOnline(Long userId)
```

## Session Management

### User Sessions
- `Map<Long, WebSocketSession> userSessions` - Lưu session theo userId
- `Map<WebSocketSession, Long> sessionUsers` - Lưu userId theo session
- `Map<Long, Long> userCurrentRooms` - Lưu room hiện tại của user

### Connection Lifecycle
1. **Connect**: User kết nối với userId parameter
2. **Join Room**: User join vào room cụ thể
3. **Send Messages**: Gửi tin nhắn trong room
4. **Leave Room**: User leave khỏi room
5. **Disconnect**: User ngắt kết nối

## Security

### Authentication
- User ID được truyền qua query parameter
- Kiểm tra quyền truy cập room trước khi join/send
- Session validation

### Authorization
- Chỉ thành viên room mới có thể gửi/nhận tin nhắn
- Kiểm tra `hasAccessToRoom(roomId, userId)` trước mọi operations

## Performance & Reliability

### Connection Management
- Auto-reconnection với exponential backoff
- Heartbeat mechanism (PING/PONG)
- Connection timeout detection
- Session cleanup on disconnect

### Message Broadcasting
- Efficient room-based broadcasting
- Thread-safe với ConcurrentHashMap
- Error handling cho failed sends

### DTO Usage
- Tránh lazy loading issues
- Efficient data transfer
- Type-safe message handling

## Error Handling

### WebSocket Errors
- Connection loss detection
- Automatic reconnection
- Error logging với SLF4J
- User notification

### Message Errors
- JSON parsing errors
- Invalid message types
- Access denied errors
- Database errors

## Testing

### Unit Tests
```java
@Test
public void testJoinRoom() {
    // Test join room functionality
}

@Test
public void testSendMessage() {
    // Test message sending
}

@Test
public void testBroadcastToRoom() {
    // Test room broadcasting
}
```

### Integration Tests
```java
@Test
public void testWebSocketConnection() {
    // Test WebSocket connection
}

@Test
public void testMessageFlow() {
    // Test complete message flow
}
```

## Deployment

### Requirements
- Spring Boot 2.7+
- WebSocket support
- Jackson for JSON processing
- SLF4J for logging

### Configuration
```properties
# WebSocket configuration
spring.websocket.enabled=true
```

### Production Considerations
- Load balancer WebSocket support
- Session clustering (Redis/Hazelcast)
- Connection pooling
- Monitoring và metrics

## Troubleshooting

### Common Issues
1. **Connection failed**: Kiểm tra CORS settings và endpoint URL
2. **Message not received**: Kiểm tra room subscription và user permissions
3. **Typing indicator not working**: Kiểm tra timeout settings
4. **Lazy loading errors**: Đảm bảo sử dụng DTO

### Debug Mode
```javascript
// Enable WebSocket debug
this.websocket.onopen = (event) => {
    console.log('WebSocket connected:', event);
};

this.websocket.onmessage = (event) => {
    console.log('WebSocket message received:', event.data);
};
```

### Logging
```java
// Backend logging
log.info("User {} joined room {}", userId, roomId);
log.debug("Message sent by user {} to room {}", userId, roomId);
log.error("Error sending message: {}", e.getMessage(), e);
```

## Comparison với STOMP

### Advantages của WebSocket Thuần
- **Simplicity**: Không cần STOMP protocol overhead
- **Performance**: Ít layer hơn, nhanh hơn
- **Control**: Full control over message format
- **Debugging**: Dễ debug hơn với raw WebSocket

### Disadvantages
- **Manual Protocol**: Phải tự implement message protocol
- **No Built-in Features**: Không có subscription management tự động
- **More Code**: Phải viết nhiều code hơn cho session management

## Future Enhancements

1. **Message Queuing**: Redis/RabbitMQ cho message persistence
2. **File Upload**: WebSocket file transfer
3. **Voice Messages**: Audio recording và streaming
4. **Video Calls**: WebRTC integration
5. **Message Encryption**: End-to-end encryption
6. **Message Search**: Full-text search
7. **Message Reactions**: Emoji reactions
8. **Message Threading**: Reply to specific messages
9. **Presence System**: Advanced online/offline status
10. **Message History**: Pagination và caching



