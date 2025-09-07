# Chat WebSocket Integration - Hướng dẫn test

## Tổng quan

Hệ thống chat đã được tích hợp hoàn chỉnh với WebSocket thuần vào file `chat.html`. Bao gồm:

- **Backend**: ChatWebSocketHandler với WebSocket thuần
- **Frontend**: ChatManager + ChatWebSocketHandler tích hợp vào chat.html
- **Real-time**: Gửi/nhận tin nhắn, typing indicators, online status

## Cách test

### 1. Khởi động ứng dụng
```bash
mvn spring-boot:run
```

### 2. Truy cập chat page
```
http://localhost:8080/chat
```

### 3. Test với 2 browser/tab khác nhau

#### Browser 1 (User 1):
1. Login với user ID 1
2. Truy cập `/chat`
3. Tạo chat mới với user khác
4. Gửi tin nhắn

#### Browser 2 (User 2):
1. Login với user ID 2  
2. Truy cập `/chat`
3. Chọn chat room với user 1
4. Gửi tin nhắn trả lời

## Tính năng đã tích hợp

### ✅ **WebSocket Connection**
- Tự động kết nối khi vào trang chat
- Auto-reconnection khi mất kết nối
- Heartbeat mechanism (PING/PONG)

### ✅ **Real-time Messaging**
- Gửi tin nhắn real-time
- Nhận tin nhắn từ user khác
- Hiển thị tin nhắn trong chat window

### ✅ **Typing Indicators**
- Hiển thị khi user đang typing
- Tự động ẩn sau 1 giây
- Chỉ hiển thị cho user khác

### ✅ **Chat Room Management**
- Load danh sách chat rooms
- Tạo chat 1-1 mới
- Join/leave room
- Cập nhật last message

### ✅ **User Interface**
- Responsive chat interface
- Message bubbles (sent/received)
- Online status
- Unread message count

## Message Flow

### 1. **Kết nối WebSocket**
```javascript
// Frontend
const wsUrl = `ws://localhost:8080/ws/chat?userId=${userId}`;
const websocket = new WebSocket(wsUrl);

// Backend
@MessageMapping("/ws/chat")
public void afterConnectionEstablished(WebSocketSession session)
```

### 2. **Join Room**
```javascript
// Frontend
wsHandler.joinRoom(roomId);

// Backend
private void handleJoinRoom(WebSocketSession session, Long userId, WebSocketMessageDto message)
```

### 3. **Gửi Tin Nhắn**
```javascript
// Frontend
wsHandler.sendMessage(roomId, content, 'TEXT');

// Backend
private void handleSendMessage(WebSocketSession session, Long userId, WebSocketMessageDto message)
```

### 4. **Nhận Tin Nhắn**
```javascript
// Frontend
handleChatMessage(data) {
    const message = data.message || data.data;
    this.addMessageToUI(message);
}

// Backend
private void broadcastToRoom(Long roomId, WebSocketMessageDto message)
```

## Debug & Troubleshooting

### 1. **Kiểm tra WebSocket Connection**
```javascript
// Mở Developer Tools Console
console.log('WebSocket status:', wsHandler.isConnected());
```

### 2. **Kiểm tra Message Flow**
```javascript
// Backend logs
log.info("User {} joined room {}", userId, roomId);
log.debug("Message sent by user {} to room {}", userId, roomId);

// Frontend logs
console.log('ChatManager received WebSocket message:', data);
```

### 3. **Common Issues**

#### WebSocket không kết nối được:
- Kiểm tra user đã login chưa
- Kiểm tra endpoint `/ws/chat`
- Kiểm tra CORS settings

#### Tin nhắn không hiển thị:
- Kiểm tra user có quyền truy cập room
- Kiểm tra message format
- Kiểm tra broadcast logic

#### Typing indicator không hoạt động:
- Kiểm tra timeout settings
- Kiểm tra event listeners
- Kiểm tra UI updates

## API Endpoints

### REST API
- `GET /api/chat/rooms` - Lấy danh sách chat rooms
- `POST /api/chat/private-chat?otherUserId={id}` - Tạo chat 1-1
- `GET /api/chat/rooms/{roomId}/messages` - Lấy tin nhắn
- `POST /api/chat/rooms/{roomId}/messages` - Gửi tin nhắn

### WebSocket API
- `ws://localhost:8080/ws/chat?userId={id}` - Kết nối WebSocket
- Message types: JOIN_ROOM, LEAVE_ROOM, SEND_MESSAGE, TYPING, STOP_TYPING

## File Structure

```
src/main/resources/
├── templates/user/
│   └── chat.html                 # Chat page với WebSocket tích hợp
├── static/
│   ├── js/
│   │   ├── chat-websocket.js     # WebSocket handler
│   │   └── chat-manager.js       # Chat UI manager
│   └── css/
│       └── chat.css              # Chat styles
└── java/.../handler/chat/
    ├── ChatWebSocketHandler.java # Backend WebSocket handler
    └── ChatBroadcaster.java      # Message broadcaster
```

## Performance Notes

- **Connection Management**: Auto-reconnection với exponential backoff
- **Message Broadcasting**: Efficient room-based broadcasting
- **UI Updates**: Debounced typing indicators
- **Memory Management**: Proper cleanup on disconnect

## Security

- **Authentication**: JWT token validation
- **Authorization**: Room access control
- **Input Validation**: Message content validation
- **CORS**: Configured for development

## Next Steps

1. **File Upload**: Hỗ trợ gửi file qua WebSocket
2. **Voice Messages**: Audio recording và streaming
3. **Video Calls**: WebRTC integration
4. **Message Encryption**: End-to-end encryption
5. **Message Search**: Full-text search
6. **Message Reactions**: Emoji reactions
7. **Message Threading**: Reply to specific messages




