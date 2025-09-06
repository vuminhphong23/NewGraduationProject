/**
 * Simple Chat Manager - Code lại từ đầu
 */
class SimpleChatManager {
    constructor() {
        this.currentUserId = null;
        this.currentRoomId = null;
        this.chatRooms = [];
        this.filteredChatRooms = [];
        this.messages = [];
        this.pollingInterval = null;
        this.searchQuery = '';
        
        this.init();
    }

    async init() {
        try {
            // Lấy user ID
            this.currentUserId = await this.getCurrentUserId();
            if (!this.currentUserId) {
                console.error('Cannot get current user ID');
                return;
            }
            
            console.log('SimpleChatManager initialized for user:', this.currentUserId);
            
            // Load chat rooms
            await this.loadChatRooms();
            
            // Setup event listeners
            this.setupEventListeners();
            
            // Khởi tạo WebSocket connection
            this.initializeWebSocket();
            
        } catch (error) {
            console.error('Error initializing SimpleChatManager:', error);
        }
    }

    async getCurrentUserId() {
        try {
            // Kiểm tra cache trước (giống như websocket-manager.js)
            if (window.currentUser && window.currentUser.id) {
                return window.currentUser.id;
            }
            
            // Gọi API /me để lấy thông tin user
            const response = await fetch('/api/auth/me', {
                method: 'GET',
                credentials: 'include'
            });
            
            if (response.ok) {
                const userData = await response.json();
                if (userData.id) {
                    // Lưu vào cache để lần sau sử dụng
                    window.currentUser = userData;
                    return userData.id;
                }
            }
            
            return null;
        } catch (error) {
            console.error('Error getting current user ID:', error);
            return null;
        }
    }

    async loadChatRooms() {
        try {
            const response = await fetch('/api/chat/rooms', {
                method: 'GET',
                credentials: 'include'
            });

                            if (response.ok) {
                    const data = await response.json();
                    if (data.success) {
                        this.chatRooms = data.data || [];
                        this.filteredChatRooms = [...this.chatRooms];
                        this.renderChatRooms();
                        this.updateMessageCount();
                    }
                }
        } catch (error) {
            console.error('Error loading chat rooms:', error);
        }
    }

    renderChatRooms() {
        const chatRoomsList = document.getElementById('chatRoomsList');
        if (!chatRoomsList) return;

        const roomsToRender = this.searchQuery ? this.filteredChatRooms : this.chatRooms;

        if (roomsToRender.length === 0) {
            chatRoomsList.innerHTML = `
                <div class="no-rooms" style="text-align: center; padding: 2rem; color: #6c757d;">
                    <i class="fas fa-comments" style="font-size: 2rem; margin-bottom: 1rem; color: #9ca3af;"></i>
                    <p>Chưa có cuộc trò chuyện nào</p>
                    <p style="font-size: 0.9rem; margin-top: 0.5rem;">Bắt đầu cuộc trò chuyện mới với bạn bè!</p>
                </div>
            `;
            return;
        }

        chatRoomsList.innerHTML = '';

        roomsToRender.forEach(room => {
            const roomDiv = document.createElement('div');
            roomDiv.className = 'chat-room-item';
            roomDiv.setAttribute('data-room-id', room.id);
            
            // Tìm user khác trong room để hiển thị online status
            const otherUser = room.members ? room.members.find(m => m.id !== this.currentUserId) : null;
            const isOnline = otherUser ? otherUser.isOnline : false;
            
            // Xác định tin nhắn hiển thị: ưu tiên tin nhắn chưa đọc
            let displayMessage = 'Chưa có tin nhắn';
            let messageClass = 'room-last-message';
            
            if (room.unreadCount > 0 && room.lastUnreadMessage) {
                // Hiển thị tin nhắn cuối cùng chưa đọc
                displayMessage = room.lastUnreadMessage;
                messageClass = 'room-last-message unread-message';
            } else if (room.lastMessage && room.lastMessage.content) {
                // Hiển thị tin nhắn cuối cùng đã đọc (lấy content từ object)
                displayMessage = room.lastMessage.content;
            }
            
            roomDiv.innerHTML = `
                <img class="room-avatar" src="${room.avatar || 'https://cdn.pixabay.com/photo/2023/02/18/11/00/icon-7797704_640.png'}" alt="Avatar" onerror="this.src='https://cdn.pixabay.com/photo/2023/02/18/11/00/icon-7797704_640.png'">
                <div class="room-info">
                    <div class="room-name">${room.roomName}</div>
                    <div class="${messageClass}">${displayMessage}</div>
                </div>
                <div class="room-meta">
                    <div class="room-time">${this.formatTime(room.updatedAt)}</div>
                    ${room.unreadCount > 0 ? `<div class="unread-badge">${room.unreadCount}</div>` : ''}
                </div>
                <div class="online-status ${isOnline ? 'online' : 'offline'}"></div>
            `;

            roomDiv.addEventListener('click', () => {
                this.selectRoom(room.id);
            });

            chatRoomsList.appendChild(roomDiv);
        });
    }

    async selectRoom(roomId) {
        try {
            // Leave previous room
            if (this.currentRoomId && window.chatWebSocketManager) {
                window.chatWebSocketManager.leaveRoom(this.currentRoomId);
            }
            
            this.currentRoomId = roomId;
            
            // Update UI - highlight selected room
            document.querySelectorAll('.chat-room-item').forEach(item => {
                item.classList.remove('active');
            });
            
            const selectedRoom = document.querySelector(`[data-room-id="${roomId}"]`);
            if (selectedRoom) {
                selectedRoom.classList.add('active');
            }
            
            // Show chat window and hide placeholder
            const chatPlaceholder = document.getElementById('chatPlaceholder');
            const chatWindow = document.getElementById('chatWindow');
            
            if (chatPlaceholder) {
                chatPlaceholder.style.display = 'none';
            }
            if (chatWindow) {
                chatWindow.style.display = 'block';
            }
            
            // Update chat header
            this.updateChatHeader(roomId);
            
            // Join new room via WebSocket
            if (window.chatWebSocketManager) {
                window.chatWebSocketManager.joinRoom(roomId);
            }
            
            // Load messages
            await this.loadRoomMessages(roomId);
            
        } catch (error) {
            console.error('Error selecting room:', error);
        }
    }

    async markRoomAsRead(roomId) {
        try {
            const response = await fetch(`/api/chat/rooms/${roomId}/read`, {
                method: 'POST',
                credentials: 'include'
            });

            if (response.ok) {
                console.log('Room marked as read');
                
                // Cập nhật room data
                const room = this.chatRooms.find(r => r.id === roomId);
                if (room) {
                    room.unreadCount = 0;
                    room.lastUnreadMessage = null;
                }
                
                // Gửi WebSocket event
                this.sendMessageReadStatus();
                
                // Cập nhật UI ngay lập tức
                this.updateLastMessageReadStatus();
                
                // Re-render rooms list
                this.renderChatRooms();
            }
        } catch (error) {
            console.error('Error marking room as read:', error);
        }
    }

    markRoomAsReadIfScrolledToBottom() {
        const chatMessages = document.getElementById('chatMessages');
        if (!chatMessages || !this.currentRoomId) return;
        
        // Kiểm tra xem đã scroll xuống cuối chưa
        const isAtBottom = chatMessages.scrollTop + chatMessages.clientHeight >= chatMessages.scrollHeight - 10;
        
        if (isAtBottom) {
            // Chỉ đánh dấu đã đọc nếu có tin nhắn của người khác chưa đọc
            const hasUnreadReceivedMessages = this.messages.some(msg => 
                msg.senderId !== this.currentUserId && !msg.isRead
            );
            if (hasUnreadReceivedMessages) {
                // Gọi API để đánh dấu đã đọc nhưng không reload messages
                this.markRoomAsReadSilently(this.currentRoomId);
            }
        }
    }
    
    // Đánh dấu đã đọc mà không reload messages
    async markRoomAsReadSilently(roomId) {
        try {
            const response = await fetch(`/api/chat/rooms/${roomId}/read`, {
                method: 'POST',
                credentials: 'include'
            });

            if (response.ok) {
                console.log('Room marked as read silently');
                
                // Cập nhật room data
                const room = this.chatRooms.find(r => r.id === roomId);
                if (room) {
                    room.unreadCount = 0;
                    room.lastUnreadMessage = null;
                }
                
                // Gửi WebSocket event
                this.sendMessageReadStatus();
                
                // Cập nhật UI ngay lập tức
                this.updateLastMessageReadStatus();
                
                // Re-render rooms list
                this.renderChatRooms();
            }
        } catch (error) {
            console.error('Error marking room as read silently:', error);
        }
    }

    findLastSentMessageIndex() {
        // Tìm index của tin nhắn cuối cùng do người gửi hiện tại gửi
        for (let i = this.messages.length - 1; i >= 0; i--) {
            if (this.messages[i].senderId === this.currentUserId) {
                return i;
            }
        }
        return -1; // Không tìm thấy tin nhắn nào của người gửi
    }

    updateMessageReadStatus(roomId) {
        // Chỉ cập nhật tin nhắn trong room hiện tại
        if (this.currentRoomId !== roomId) {
            return;
        }
        
        // Chỉ cập nhật UI, không reload messages để tránh vòng lặp
        this.updateLastMessageReadStatus();
    }
    
    // Cập nhật trạng thái đã xem cho tin nhắn cuối cùng (chỉ UI)
    updateLastMessageReadStatus() {
        const lastSentMessageIndex = this.findLastSentMessageIndex();
        if (lastSentMessageIndex !== -1) {
            const lastSentMessage = this.messages[lastSentMessageIndex];
            if (lastSentMessage && !lastSentMessage.isRead) {
                // Cập nhật trạng thái trong data
                lastSentMessage.isRead = true;
                lastSentMessage.readAt = new Date().toISOString();
                
                // Cập nhật UI
                const messageElement = document.querySelector(`[data-message-id="${lastSentMessage.id}"]`);
                if (messageElement) {
                    const statusElement = messageElement.querySelector('.message-status.sent');
                    if (statusElement) {
                        statusElement.textContent = 'Đã xem';
                        statusElement.classList.remove('sent');
                        statusElement.classList.add('read');
                    }
                }
            }
        }
    }

    updateChatHeader(roomId) {
        const room = this.chatRooms.find(r => r.id === roomId);
        if (!room) return;
        
        const chatUserName = document.getElementById('chatUserName');
        const chatAvatar = document.getElementById('chatAvatar');
        const onlineStatus = document.getElementById('onlineStatus');
        const onlineIndicator = document.getElementById('onlineIndicator');
        
        if (chatUserName) {
            chatUserName.textContent = room.roomName || 'Chat Room';
        }
        
        if (chatAvatar) {
            chatAvatar.src = room.avatar || 'https://cdn.pixabay.com/photo/2023/02/18/11/00/icon-7797704_640.png';
            chatAvatar.onerror = function() {
                this.src = 'https://cdn.pixabay.com/photo/2023/02/18/11/00/icon-7797704_640.png';
            };
        }
        
        if (onlineStatus) {
            onlineStatus.textContent = 'Trực tuyến';
        }
        
        if (onlineIndicator) {
            onlineIndicator.style.display = 'block';
        }
        
        if (onlineStatus) {
            onlineStatus.textContent = room.isGroup ? 'Nhóm' : 'Trực tuyến';
        }
    }

    async loadRoomMessages(roomId) {
        try {
            console.log('Loading messages for room:', roomId);
            const response = await fetch(`/api/chat/rooms/${roomId}/messages`, {
                method: 'GET',
                credentials: 'include'
            });

            console.log('Response status:', response.status);
            console.log('Response ok:', response.ok);

            if (response.ok) {
                const data = await response.json();
                console.log('Response data:', data);
                if (data.success) {
                    this.messages = data.data || [];
                    console.log('Messages loaded:', this.messages.length);
                    this.renderMessages();
                } else {
                    console.error('API returned success=false:', data.message);
                }
            } else {
                console.error('HTTP error:', response.status, response.statusText);
                const errorText = await response.text();
                console.error('Error response:', errorText);
            }
        } catch (error) {
            console.error('Error loading room messages:', error);
        }
    }

    renderMessages() {
        console.log('Rendering messages:', this.messages.length);
        const chatMessages = document.getElementById('chatMessages');
        if (!chatMessages) {
            console.error('chatMessages element not found');
            return;
        }

        console.log('chatMessages element found:', chatMessages);
        chatMessages.innerHTML = '';

        if (this.messages.length === 0) {
            console.log('No messages to render');
            chatMessages.innerHTML = '<div class="no-messages">Chưa có tin nhắn nào</div>';
            return;
        }

        // Tìm tin nhắn cuối cùng của người gửi hiện tại
        const lastSentMessageIndex = this.findLastSentMessageIndex();
        
        this.messages.forEach((message, index) => {
            console.log(`Rendering message ${index}:`, message);
            const messageDiv = document.createElement('div');
            messageDiv.className = `message ${message.senderId === this.currentUserId ? 'sent' : 'received'}`;
            
            // Chỉ hiển thị read status cho tin nhắn cuối cùng của người gửi
            let readStatus = '';
            if (message.senderId === this.currentUserId && index === lastSentMessageIndex) {
                // Chỉ hiển thị "đã xem" nếu tin nhắn thực sự đã được đọc bởi người nhận
                if (message.isRead && message.readAt) {
                    readStatus = `<div class="message-status read">✓✓ Đã xem</div>`;
                } else {
                    readStatus = `<div class="message-status sent">✓ Đã gửi</div>`;
                }
            }
            
            messageDiv.innerHTML = `
                <div class="message-content">${message.content}</div>
                <div class="message-time">${this.formatTime(message.createdAt)}</div>
                ${readStatus}
            `;

            chatMessages.appendChild(messageDiv);
            console.log(`Message ${index} appended to DOM`);
        });

        // Scroll to bottom
        setTimeout(() => {
            chatMessages.scrollTop = chatMessages.scrollHeight;
        }, 100);
        console.log('Messages rendered successfully, total messages in DOM:', chatMessages.children.length);
    }

    async sendMessage() {
        const messageInput = document.getElementById('messageInput');
        const content = messageInput.value.trim();

        if (!content || !this.currentRoomId) {
            return;
        }

        
        // Hiển thị tin nhắn ngay lập tức với trạng thái "đang gửi"
        this.showSendingMessage(content);
        messageInput.value = '';

        try {
            const response = await fetch(`/api/chat/rooms/${this.currentRoomId}/messages`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                credentials: 'include',
                body: JSON.stringify({ content: content })
            });

            if (response.ok) {
                const data = await response.json();
                console.log('📤 Message sent successfully:', data);
                if (data.success) {
                    // Xóa tin nhắn tạm và thêm tin nhắn thật từ server
                    this.removeSendingMessage();
                    this.addMessageToUI(data.data);
                }
            } else {
                console.error('📤 Failed to send message:', response.status, response.statusText);
                // Nếu gửi thất bại, xóa tin nhắn tạm
                this.removeSendingMessage();
            }
        } catch (error) {
            console.error('Error sending message:', error);
            this.removeSendingMessage();
        }
    }

    showSendingMessage(content) {
        const chatMessages = document.getElementById('chatMessages');
        
        // Xóa trạng thái "đang gửi" cũ nếu có
        this.removeSendingMessage();
        
        const messageDiv = document.createElement('div');
        messageDiv.className = 'message sent sending';
        messageDiv.innerHTML = `
            <div class="message-content">${content}</div>
            <div class="message-time">${this.formatTime(new Date())}</div>
            <div class="message-status sending">⏳ Đang gửi...</div>
        `;
        
        chatMessages.appendChild(messageDiv);
        setTimeout(() => {
            chatMessages.scrollTop = chatMessages.scrollHeight;
        }, 100);
    }

    removeSendingMessage() {
        // Chỉ xóa tin nhắn "đang gửi" cuối cùng
        const sendingMessages = document.querySelectorAll('.message.sending');
        if (sendingMessages.length > 0) {
            const lastSendingMessage = sendingMessages[sendingMessages.length - 1];
            lastSendingMessage.remove();
        }
    }

    setupEventListeners() {
        // Send message button
        const sendBtn = document.getElementById('sendBtn');
        if (sendBtn) {
            sendBtn.addEventListener('click', () => {
                this.sendMessage();
            });
        }

        // Message input enter key
        const messageInput = document.getElementById('messageInput');
        if (messageInput) {
            messageInput.addEventListener('keypress', (e) => {
                if (e.key === 'Enter') {
                    this.sendMessage();
                }
            });
            
            // Search functionality
            const searchBtn = document.getElementById('searchBtn');
            const searchInput = document.getElementById('chatSearchInput');
            const searchClearBtn = document.getElementById('searchClearBtn');
            
            if (searchBtn) {
                searchBtn.addEventListener('click', () => {
                    this.toggleSearch();
                });
            }
            
            if (searchInput) {
                searchInput.addEventListener('input', (e) => {
                    const query = e.target.value;
                    this.searchChatRooms(query);
                    
                    // Hiển thị/ẩn nút clear
                    if (searchClearBtn) {
                        searchClearBtn.style.display = query ? 'block' : 'none';
                    }
                });
                
                // Xử lý phím ESC để đóng search
                searchInput.addEventListener('keydown', (e) => {
                    if (e.key === 'Escape') {
                        this.toggleSearch();
                    }
                });
            }
            
            if (searchClearBtn) {
                searchClearBtn.addEventListener('click', () => {
                    searchInput.value = '';
                    this.searchChatRooms('');
                    searchClearBtn.style.display = 'none';
                });
            }
            
            // New chat functionality
            const newChatBtn = document.getElementById('newChatBtn');
            if (newChatBtn) {
                newChatBtn.addEventListener('click', () => {
                    this.showNewChatModal();
                });
            }
            
        }

        // Scroll event để đánh dấu đã đọc
        const chatMessages = document.getElementById('chatMessages');
        if (chatMessages) {
            chatMessages.addEventListener('scroll', () => {
                this.markRoomAsReadIfScrolledToBottom();
            });
        }
    }

    formatTime(dateString) {
        const date = new Date(dateString);
        return date.toLocaleTimeString('vi-VN', { 
            hour: '2-digit', 
            minute: '2-digit' 
        });
    }
    
    // Khởi tạo WebSocket connection
    initializeWebSocket() {
        if (window.chatWebSocketManager) {
            window.chatWebSocketManager.connect();
            console.log('Chat WebSocket initialized');
            
            // Dừng polling nếu WebSocket hoạt động
            this.stopPolling();
        } else {
            console.warn('Chat WebSocket manager not available, using polling fallback');
            this.startPolling();
        }
    }
    
    // Thêm message vào UI (được gọi từ WebSocket)
    addMessageToUI(messageData) {
        console.log('🔄 addMessageToUI called with:', messageData);
        
        // Kiểm tra xem message đã tồn tại chưa
        const existingMessage = this.messages.find(m => m.id === messageData.id);
        if (existingMessage) {
            console.log('🔄 Message already exists, skipping');
            return; // Message đã tồn tại, không thêm lại
        }
        
        console.log('🔄 Adding new message to array');
        // Thêm message mới
        this.messages.push(messageData);
        
        console.log('🔄 Total messages now:', this.messages.length);
        
        // Render lại messages
        console.log('🔄 Calling renderMessages');
        this.renderMessages();
        
        // Scroll xuống cuối
        console.log('🔄 Calling scrollToBottom');
        this.scrollToBottom();
    }
    
    // Cập nhật last message của room (được gọi từ WebSocket)
    updateRoomLastMessage(roomId, messageData) {
        const room = this.chatRooms.find(r => r.id === roomId);
        if (room) {
            // Nếu tin nhắn không phải từ user hiện tại và chưa đọc
            if (messageData.senderId !== this.currentUserId && !messageData.isRead) {
                room.lastUnreadMessage = messageData.content;
                room.unreadCount = (room.unreadCount || 0) + 1;
            }
            
            // Luôn cập nhật last message (tạo object giống backend)
            room.lastMessage = {
                content: messageData.content,
                createdAt: messageData.createdAt,
                senderName: messageData.senderName,
                senderId: messageData.senderId,
                isRead: messageData.isRead
            };
            room.lastMessageTime = messageData.createdAt;
            room.lastMessageSender = messageData.senderName;
            
            // Re-render rooms list
            this.renderChatRooms();
            
            // Cập nhật message count
            this.updateMessageCount();
        }
    }

    // Tìm kiếm chat rooms
    searchChatRooms(query) {
        this.searchQuery = query.toLowerCase().trim();
        
        if (!this.searchQuery) {
            this.filteredChatRooms = [...this.chatRooms];
        } else {
            this.filteredChatRooms = this.chatRooms.filter(room => {
                // Tìm kiếm theo tên room
                const roomNameMatch = room.roomName.toLowerCase().includes(this.searchQuery);
                
                // Tìm kiếm theo tên thành viên
                const memberMatch = room.members && room.members.some(member => 
                    (member.fullName && member.fullName.toLowerCase().includes(this.searchQuery)) ||
                    (member.username && member.username.toLowerCase().includes(this.searchQuery))
                );
                
                // Tìm kiếm theo tin nhắn cuối cùng
                const lastMessageMatch = (room.lastMessage && room.lastMessage.content && 
                    room.lastMessage.content.toLowerCase().includes(this.searchQuery)) ||
                    (room.lastUnreadMessage && room.lastUnreadMessage.toLowerCase().includes(this.searchQuery));
                
                return roomNameMatch || memberMatch || lastMessageMatch;
            });
        }
        
        this.renderChatRooms();
    }

    // Hiển thị/ẩn search bar
    toggleSearch() {
        const searchContainer = document.getElementById('searchContainer');
        const searchInput = document.getElementById('chatSearchInput');
        const searchBtn = document.getElementById('searchBtn');
        
        if (searchContainer.style.display === 'none') {
            searchContainer.style.display = 'block';
            searchInput.focus();
            searchBtn.classList.add('active');
        } else {
            searchContainer.style.display = 'none';
            searchInput.value = '';
            this.searchQuery = '';
            this.filteredChatRooms = [...this.chatRooms];
            this.renderChatRooms();
            searchBtn.classList.remove('active');
        }
    }

    // Cập nhật message count badge
    updateMessageCount() {
        const messageCount = document.getElementById('messageCount');
        if (messageCount) {
            const totalUnread = this.chatRooms.reduce((sum, room) => sum + (room.unreadCount || 0), 0);
            messageCount.textContent = totalUnread;
            messageCount.style.display = totalUnread > 0 ? 'block' : 'none';
        }
    }

    // Hiển thị modal tạo chat mới
    showNewChatModal() {
        const modal = document.getElementById('newChatModal');
        if (modal) {
            modal.style.display = 'flex';
            const searchInput = document.getElementById('userSearchInput');
            if (searchInput) {
                searchInput.focus();
            }
        }
    }

    // Cập nhật online status của user
    updateUserOnlineStatus(userId, isOnline) {
        console.log('🟢 User', userId, 'is', isOnline ? 'online' : 'offline');
        
        // Cập nhật trong current room
        if (this.currentRoomId) {
            const room = this.chatRooms.find(r => r.id === this.currentRoomId);
            if (room) {
                const member = room.members.find(m => m.id === userId);
                if (member) {
                    member.isOnline = isOnline;
                    this.updateChatHeader(this.currentRoomId);
                }
            }
        }
        
        // Cập nhật trong tất cả rooms
        this.chatRooms.forEach(room => {
            const member = room.members.find(m => m.id === userId);
            if (member) {
                member.isOnline = isOnline;
            }
        });
        
        this.renderChatRooms();
    }
    
    
    // Bắt đầu polling (fallback khi WebSocket không hoạt động)
    startPolling() {
        if (this.pollingInterval) {
            clearInterval(this.pollingInterval);
        }
        
        // Chỉ polling nếu WebSocket không hoạt động
        if (!window.chatWebSocketManager || !window.chatWebSocketManager.isConnected()) {
            this.pollingInterval = setInterval(() => {
                if (this.currentRoomId) {
                    this.loadRoomMessages(this.currentRoomId);
                }
            }, 10000); // Poll mỗi 10 giây (chậm hơn để tránh spam)
            
            console.log('Started polling for chat messages (WebSocket not available)');
        }
    }
    
    // Dừng polling
    stopPolling() {
        if (this.pollingInterval) {
            clearInterval(this.pollingInterval);
            this.pollingInterval = null;
            console.log('Stopped polling for chat messages');
        }
    }
    
    
    // Gửi message read status qua WebSocket
    sendMessageReadStatus() {
        if (window.chatWebSocketManager && window.chatWebSocketManager.isConnected() && this.currentRoomId) {
            window.chatWebSocketManager.sendMessageRead(this.currentRoomId);
        }
    }
}

// Initialize when page loads
document.addEventListener('DOMContentLoaded', function() {
    console.log('Initializing Simple Chat Manager...');
    window.chatManager = new SimpleChatManager();
    window.simpleChatManager = window.chatManager; // Alias for compatibility
});
