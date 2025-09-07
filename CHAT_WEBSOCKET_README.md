# Chat WebSocket System - Hướng dẫn sử dụng

## Tổng quan

Hệ thống chat WebSocket được xây dựng với Spring WebSocket và STOMP protocol, hỗ trợ chat 1-1 và chat nhóm real-time.

## Kiến trúc

### Backend Components

1. **DTO Classes** - Tránh lazy loading issues:
   - `ChatRoomDto` - Thông tin chat room
   - `ChatMessageDto` - Thông tin tin nhắn
   - `ChatRoomMemberDto` - Thông tin thành viên
   - `WebSocketMessageDto` - Message cho WebSocket

2. **WebSocket Configuration**:
   - `WebSocketConfig` - Cấu hình STOMP endpoints
   - Endpoint: `/ws` với SockJS fallback
   - Topics: `/topic/room.{roomId}`
   - Queues: `/user/{userId}/queue/*`

3. **WebSocket Handler**:
   - `ChatWebSocketHandler` - Xử lý các WebSocket events
   - Message types: MESSAGE, TYPING, STOP_TYPING, USER_ONLINE, USER_OFFLINE, MESSAGE_READ

4. **Service Layer**:
   - `ChatService` - Interface với DTO methods
   - `ChatServiceImpl` - Implementation sử dụng DTO
   - `ChatMapper` - Convert Entity ↔ DTO

5. **REST API**:
   - `ChatController` - REST endpoints cho chat operations
   - Tất cả responses sử dụng DTO

### Frontend Components

1. **ChatWebSocketHandler** - JavaScript class:
   - Kết nối STOMP WebSocket
   - Xử lý real-time messages
   - Typing indicators
   - Notification system

## API Endpoints

### REST API

#### Chat Rooms
- `GET /api/chat/rooms` - Lấy danh sách chat rooms của user
- `POST /api/chat/private-chat?otherUserId={id}` - Tạo/tìm chat 1-1
- `POST /api/chat/group-chat?roomName={name}&memberIds={ids}` - Tạo chat nhóm
- `GET /api/chat/rooms/{roomId}` - Lấy thông tin room
- `GET /api/chat/rooms/{roomId}/members` - Lấy danh sách thành viên
- `GET /api/chat/rooms/{roomId}/unread-count` - Lấy số tin nhắn chưa đọc

#### Messages
- `GET /api/chat/rooms/{roomId}/messages?page={p}&size={s}` - Lấy tin nhắn
- `POST /api/chat/rooms/{roomId}/messages?content={text}&messageType={type}` - Gửi tin nhắn
- `PUT /api/chat/messages/{messageId}/read` - Đánh dấu đã đọc

#### Room Management
- `POST /api/chat/rooms/{roomId}/members?userId={id}` - Thêm thành viên
- `DELETE /api/chat/rooms/{roomId}/members/{userId}` - Xóa thành viên

### WebSocket API

#### Connection
```javascript
// Kết nối WebSocket
const socket = new SockJS('/ws');
const stompClient = Stomp.over(socket);
stompClient.connect({userId: currentUserId}, callback);
```

#### Room Operations
```javascript
// Join room
stompClient.send(`/app/chat.join.${roomId}`, {}, JSON.stringify({
    type: 'JOIN_ROOM',
    roomId: roomId,
    userId: userId,
    username: username
}));

// Leave room
stompClient.send(`/app/chat.leave.${roomId}`, {}, JSON.stringify({
    type: 'LEAVE_ROOM',
    roomId: roomId,
    userId: userId
}));
```

#### Message Operations
```javascript
// Gửi tin nhắn
stompClient.send(`/app/chat.send.${roomId}`, {}, JSON.stringify({
    type: 'SEND_MESSAGE',
    roomId: roomId,
    content: content,
    messageType: 'TEXT'
}));

// Typing indicator
stompClient.send(`/app/chat.typing.${roomId}`, {}, JSON.stringify({
    type: 'TYPING',
    roomId: roomId,
    userId: userId,
    username: username
}));

// Mark as read
stompClient.send(`/app/chat.read.${roomId}`, {}, JSON.stringify({
    type: 'MESSAGE_READ',
    roomId: roomId,
    messageId: messageId,
    userId: userId
}));
```

#### Subscriptions
```javascript
// Subscribe to room messages
stompClient.subscribe(`/topic/room.${roomId}`, (message) => {
    const data = JSON.parse(message.body);
    handleRoomMessage(data);
});

// Subscribe to user-specific updates
stompClient.subscribe(`/user/${userId}/queue/online-users`, (message) => {
    const data = JSON.parse(message.body);
    handleOnlineUsers(data);
});
```

## Message Types

### WebSocket Message Types
- `MESSAGE` - Tin nhắn mới
- `TYPING` - User đang typing
- `STOP_TYPING` - User dừng typing
- `USER_ONLINE` - User online
- `USER_OFFLINE` - User offline
- `MESSAGE_READ` - Tin nhắn đã đọc
- `ROOM_UPDATE` - Cập nhật room
- `ONLINE_USERS` - Danh sách user online

### Chat Message Types
- `TEXT` - Tin nhắn văn bản
- `IMAGE` - Hình ảnh
- `FILE` - Tệp đính kèm
- `EMOJI` - Emoji

## Chat Room Logic

### Chat 1-1 vs Group Chat
- **Chat 1-1**: `isGroup = false`, chỉ có 2 thành viên
- **Group Chat**: `isGroup = true`, có nhiều hơn 2 thành viên
- Tự động cập nhật `isGroup` dựa trên số lượng thành viên

### Room Naming
- **Chat 1-1**: Tên tự động = "FirstName LastName & FirstName LastName"
- **Group Chat**: Tên do user đặt

## Frontend Integration

### Khởi tạo ChatWebSocketHandler
```javascript
// Trong chat page
const chatManager = new ChatManager();
const wsHandler = new ChatWebSocketHandler(chatManager);

// Join room khi user chọn room
wsHandler.joinRoom(roomId);

// Gửi tin nhắn
wsHandler.sendMessage(roomId, content, 'TEXT');

// Typing indicator
wsHandler.sendTyping(roomId);
```

### Event Handling
```javascript
// Xử lý tin nhắn mới
function handleChatMessage(data) {
    const message = data.message;
    // Thêm message vào UI
    addMessageToUI(message);
    
    // Notification nếu không đang xem room
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

## Security

### Authentication
- User ID được gửi trong WebSocket headers
- Kiểm tra quyền truy cập room trước khi gửi/nhận tin nhắn
- JWT token validation (nếu cần)

### Authorization
- Chỉ thành viên room mới có thể gửi/nhận tin nhắn
- Kiểm tra `hasAccessToRoom(roomId, userId)` trước mọi operations

## Performance Considerations

### DTO Usage
- Sử dụng DTO để tránh lazy loading issues
- Chỉ load dữ liệu cần thiết
- Efficient mapping với `ChatMapper`

### WebSocket Optimization
- Connection pooling
- Message batching (nếu cần)
- Heartbeat mechanism
- Auto-reconnection với exponential backoff

## Error Handling

### WebSocket Errors
- Connection loss detection
- Automatic reconnection
- Error logging
- User notification

### API Errors
- Standardized error responses
- HTTP status codes
- Error messages trong tiếng Việt

## Testing

### Unit Tests
- Test DTO mapping
- Test service methods
- Test WebSocket handlers

### Integration Tests
- Test WebSocket connections
- Test message flow
- Test room operations

## Deployment

### Requirements
- Spring Boot 2.7+
- WebSocket support
- STOMP protocol
- SockJS fallback

### Configuration
```properties
# WebSocket configuration
spring.websocket.enabled=true
spring.websocket.stomp.enabled=true
```

## Troubleshooting

### Common Issues
1. **WebSocket connection failed**: Kiểm tra CORS settings
2. **Lazy loading errors**: Đảm bảo sử dụng DTO
3. **Message not received**: Kiểm tra room subscription
4. **Typing indicator not working**: Kiểm tra timeout settings

### Debug Mode
```javascript
// Enable STOMP debug
stompClient.debug = function(str) {
    console.log('STOMP: ' + str);
};
```

## Future Enhancements

1. **File Upload**: Hỗ trợ gửi file qua WebSocket
2. **Voice Messages**: Ghi âm và gửi voice messages
3. **Video Calls**: Tích hợp WebRTC
4. **Message Encryption**: End-to-end encryption
5. **Message Search**: Tìm kiếm tin nhắn
6. **Message Reactions**: React với emoji
7. **Message Threading**: Reply to specific messages



