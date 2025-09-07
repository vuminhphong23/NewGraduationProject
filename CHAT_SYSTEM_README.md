# 💬 Chat System - Forumikaa

## 📋 Tổng quan

Chat System được thiết kế với giao diện hiện đại, hỗ trợ real-time messaging thông qua WebSocket. Hệ thống tự động phân biệt giữa chat 1-1 và chat nhóm dựa trên số lượng thành viên.

## 🎯 Tính năng chính

### ✅ Chat 1-1 (Private Chat)
- **Tự động tạo**: Khi 2 user bắt đầu chat lần đầu
- **Tên hiển thị**: Tên của người kia (không phải tên room)
- **Avatar**: Avatar của người kia
- **Trạng thái**: Hiển thị online/offline

### ✅ Chat nhóm (Group Chat)
- **Tự động chuyển đổi**: Khi có > 2 thành viên
- **Tên hiển thị**: Tên room do người tạo đặt
- **Avatar**: Avatar của group
- **Quản lý thành viên**: Thêm/xóa thành viên

### ✅ Real-time Features
- **Tin nhắn real-time**: Gửi/nhận tin nhắn ngay lập tức
- **Typing indicator**: Hiển thị khi ai đó đang nhập
- **Online status**: Trạng thái online/offline
- **Message read status**: Đánh dấu tin nhắn đã đọc
- **Notifications**: Thông báo tin nhắn mới

### ✅ UI/UX Features
- **Giao diện hiện đại**: Gradient backgrounds, animations
- **Responsive design**: Tương thích mobile/desktop
- **Smooth animations**: Slide, fade, pulse effects
- **Custom scrollbars**: Scrollbar đẹp mắt
- **Loading states**: Spinner và loading indicators

## 🏗️ Kiến trúc hệ thống

### Backend Components
```
📁 Entity Layer
├── ChatRoom.java          # Room entity với logic tự động isGroup
├── ChatMessage.java       # Message entity với các loại TEXT/IMAGE/FILE/EMOJI
└── ChatRoomMember.java    # Member entity với unique constraint

📁 DAO Layer
├── ChatRoomDao.java       # Query tìm room 1-1, load rooms
├── ChatMessageDao.java    # Query messages với pagination
└── ChatRoomMemberDao.java # Quản lý thành viên

📁 Service Layer
├── ChatService.java       # Interface định nghĩa business logic
└── ChatServiceImpl.java   # Implementation với logic tự động isGroup

📁 Controller Layer
└── ChatController.java    # REST API endpoints

📁 WebSocket Layer
├── ChatWebSocketHandler.java # WebSocket handler cho real-time
└── ChatBroadcaster.java      # Broadcast messages
```

### Frontend Components
```
📁 JavaScript
├── chat-manager.js        # Main chat manager
├── chat-websocket.js      # WebSocket handler
├── jwt-utils.js          # Authentication utilities
├── websocket-manager.js   # WebSocket connection manager
└── toast-manager.js       # Notification system

📁 CSS
└── chat.css              # Modern UI với gradients, animations

📁 HTML
└── chat.html             # Chat interface template
```

## 🚀 API Endpoints

### Chat Rooms
- `GET /api/chat/rooms` - Lấy danh sách rooms của user
- `POST /api/chat/private-chat` - Tạo/tìm chat 1-1
- `POST /api/chat/group-chat` - Tạo chat nhóm
- `GET /api/chat/rooms/{roomId}` - Chi tiết room

### Messages
- `GET /api/chat/rooms/{roomId}/messages` - Lấy tin nhắn (pagination)
- `POST /api/chat/rooms/{roomId}/messages` - Gửi tin nhắn
- `PUT /api/chat/messages/{messageId}/read` - Đánh dấu đã đọc

### Members
- `POST /api/chat/rooms/{roomId}/members` - Thêm thành viên
- `DELETE /api/chat/rooms/{roomId}/members/{userId}` - Xóa thành viên

## 🔧 Logic tự động

### 1. Tự động xác định loại chat
```java
// Trong ChatRoom entity
public boolean isPrivateChat() {
    return getMemberCount() == 2;
}

public boolean isGroupChat() {
    return getMemberCount() > 2;
}

public void updateGroupStatus() {
    this.isGroup = isGroupChat();
}
```

### 2. Tự động tạo room 1-1
```java
// Trong ChatServiceImpl
public ChatRoom findOrCreatePrivateChat(Long user1Id, Long user2Id) {
    // Tìm room 1-1 đã tồn tại
    Optional<ChatRoom> existingRoom = chatRoomDao.findPrivateChatBetweenUsers(user1Id, user2Id);
    
    if (existingRoom.isPresent()) {
        return existingRoom.get();
    }
    
    // Tạo room mới với isGroup = false
    ChatRoom room = ChatRoom.builder()
            .roomName(roomName)
            .isGroup(false) // Chat 1-1
            .createdBy(user1)
            .build();
    
    // Thêm 2 user vào room
    addMemberToRoom(room.getId(), user1Id);
    addMemberToRoom(room.getId(), user2Id);
    
    // Cập nhật trạng thái group
    updateRoomGroupStatus(room.getId());
    
    return room;
}
```

### 3. Tự động cập nhật khi thay đổi thành viên
```java
public ChatRoomMember addMemberToRoom(Long roomId, Long userId) {
    // Thêm thành viên
    ChatRoomMember member = ChatRoomMember.builder()
            .room(room)
            .user(user)
            .build();
    
    member = chatRoomMemberDao.save(member);
    
    // Tự động cập nhật trạng thái group
    updateRoomGroupStatus(roomId);
    
    return member;
}
```

## 🎨 UI/UX Features

### Modern Design
- **Gradient backgrounds**: Purple-blue gradient theme
- **Glass morphism**: Backdrop blur effects
- **Smooth animations**: Slide, fade, pulse, float
- **Custom scrollbars**: Styled scrollbars
- **Responsive layout**: Mobile-first design

### Interactive Elements
- **Hover effects**: Transform, shadow, scale
- **Loading states**: Spinner animations
- **Typing indicators**: Animated dots
- **Message animations**: Slide-in effects
- **Notification badges**: Pulsing animation

### Color Scheme
```css
Primary: linear-gradient(135deg, #667eea 0%, #764ba2 100%)
Success: linear-gradient(135deg, #10b981 0%, #059669 100%)
Error: linear-gradient(135deg, #ef4444 0%, #dc2626 100%)
Warning: linear-gradient(135deg, #f59e0b 0%, #d97706 100%)
```

## 🔌 WebSocket Integration

### Connection Management
```javascript
// Tự động kết nối WebSocket
const chatWebSocket = new ChatWebSocketHandler(chatManager);

// Heartbeat để duy trì kết nối
setInterval(() => {
    if (websocket.readyState === WebSocket.OPEN) {
        websocket.send({ type: 'PING' });
    }
}, 30000);
```

### Message Types
- `CHAT_MESSAGE` - Tin nhắn mới
- `CHAT_TYPING` - Typing indicator
- `CHAT_USER_ONLINE` - User online
- `CHAT_USER_OFFLINE` - User offline
- `CHAT_MESSAGE_READ` - Message đã đọc
- `PING/PONG` - Heartbeat

## 📱 Responsive Design

### Mobile (< 768px)
- Sidebar chuyển thành top panel
- Chat area chiếm 40% chiều cao
- Touch-friendly buttons
- Optimized typography

### Desktop (> 768px)
- Sidebar 350px width
- Chat area full height
- Hover effects
- Keyboard shortcuts

## 🚀 Cách sử dụng

### 1. Khởi tạo
```javascript
// Tự động khởi tạo khi DOM ready
document.addEventListener('DOMContentLoaded', () => {
    window.chatManager = new ChatManager();
});
```

### 2. Tạo chat 1-1
```javascript
// Tìm kiếm user và bắt đầu chat
chatManager.startPrivateChat(userId);
```

### 3. Gửi tin nhắn
```javascript
// Tự động gửi qua WebSocket + API
chatManager.sendMessage();
```

### 4. Real-time updates
```javascript
// Tự động nhận tin nhắn qua WebSocket
chatManager.handleWebSocketMessage(data);
```

## 🔒 Security Features

- **JWT Authentication**: Tất cả API calls đều có JWT token
- **Authorization**: Kiểm tra quyền truy cập room
- **Input validation**: Escape HTML, validate input
- **Rate limiting**: Giới hạn số lượng tin nhắn
- **CORS protection**: Cross-origin request protection

## 📊 Performance Optimizations

- **Pagination**: Load messages theo trang
- **Lazy loading**: Load thêm messages khi scroll
- **Connection pooling**: Tái sử dụng WebSocket connections
- **Caching**: Cache user info, room data
- **Debouncing**: Debounce typing indicators
- **Image optimization**: Compress images trước khi gửi

## 🐛 Error Handling

- **Connection errors**: Auto-reconnect với exponential backoff
- **API errors**: Toast notifications cho user
- **Validation errors**: Form validation với error messages
- **Network errors**: Offline detection và retry logic
- **WebSocket errors**: Graceful degradation

## 🔮 Future Enhancements

- [ ] File sharing với drag & drop
- [ ] Voice messages
- [ ] Video calls integration
- [ ] Message reactions (emoji)
- [ ] Message search
- [ ] Chat history export
- [ ] Dark mode
- [ ] Message encryption
- [ ] Bot integration
- [ ] Chat analytics

---

## 📝 Kết luận

Chat System được thiết kế với:
- ✅ **Logic tự động**: Tự động phân biệt chat 1-1 và nhóm
- ✅ **Real-time**: WebSocket cho tin nhắn tức thời
- ✅ **Modern UI**: Giao diện đẹp với animations
- ✅ **Responsive**: Tương thích mọi thiết bị
- ✅ **Scalable**: Kiến trúc có thể mở rộng
- ✅ **Secure**: Bảo mật với JWT và validation

Hệ thống sẵn sàng sử dụng và có thể mở rộng thêm nhiều tính năng trong tương lai!




