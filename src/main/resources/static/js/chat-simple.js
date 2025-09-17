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
        this.markAsReadTimeout = null;
        this.selectedUsers = []; // Danh sách người được chọn cho group chat
        this.recentSearches = []; // Lưu lịch sử tìm kiếm gần đây
        this.lastSearchResults = []; // Lưu kết quả tìm kiếm cuối cùng
        
        this.init();
    }

    async init() {
        try {
            // Lấy user ID
            this.currentUserId = await this.getCurrentUserId();
            if (!this.currentUserId) {
                return;
            }
            
            // Load chat rooms
            await this.loadChatRooms();
            
            // Setup event listeners
            this.setupEventListeners();
            
            // Khởi tạo WebSocket connection
            this.initializeWebSocket();
            
        } catch (error) {
            // Error initializing
        }
    }

    async getCurrentUserId() {
        try {
            // Kiểm tra cache trước (giống như websocket-manager.js)
            if (window.currentUser && window.currentUser.id) {
                return window.currentUser.id;
            }
            
            // Gọi API /me để lấy thông tin user
            const response = await authenticatedFetch('/api/auth/me', {
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
            return null;
        }
    }

    async loadChatRooms() {
        try {
            const response = await authenticatedFetch('/api/chat/rooms', {
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
            // Error loading chat rooms
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
            roomDiv.setAttribute('data-room-type', room.isGroup ? 'group' : 'private');
            
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
            
            // Xác định tên hiển thị và avatar
            let displayName, displayAvatar, displayOnlineStatus;
            
            // Check if this is a group chat
            const isGroupChat = room.isGroup === true || (room.members && room.members.length > 2);
            
            if (isGroupChat) {
                // Group chat: hiển thị tên room
                displayName = room.roomName || 'Group Chat';
                displayAvatar = room.roomAvatar || 'https://cdn.pixabay.com/photo/2023/02/18/11/00/icon-7797704_640.png';
                displayOnlineStatus = false; // Group không có online status
            } else {
                // Private chat: hiển thị tên và avatar của người đối diện
                const otherMember = room.members && room.members.find(member => member.userId !== this.currentUserId);
                if (otherMember) {
                    displayName = otherMember.fullName || otherMember.username || 'Unknown User';
                    displayAvatar = otherMember.avatar || 'https://cdn.pixabay.com/photo/2023/02/18/11/00/icon-7797704_640.png';
                    displayOnlineStatus = otherMember.isOnline || false;
                } else {
                    displayName = room.roomName || room.roomname || 'Private Chat';
                    displayAvatar = room.roomAvatar || room.roomavatar || 'https://cdn.pixabay.com/photo/2023/02/18/11/00/icon-7797704_640.png';
                    displayOnlineStatus = false;
                }
            }
            
            // Highlight search results
            const highlightedName = this.searchQuery ? this.highlightText(displayName, this.searchQuery) : displayName;
            const highlightedMessage = this.searchQuery ? this.highlightText(displayMessage, this.searchQuery) : displayMessage;
            
            roomDiv.innerHTML = `
                <img class="room-avatar" src="${displayAvatar}" alt="Avatar" onerror="this.src='https://cdn.pixabay.com/photo/2023/02/18/11/00/icon-7797704_640.png'">
                <div class="room-info">
                    <div class="room-name">${highlightedName}</div>
                    <div class="${messageClass}">${highlightedMessage}</div>
                </div>
                <div class="room-meta">
                    <div class="room-time">${this.getRoomDisplayTime(room)}</div>
                    ${room.unreadCount > 0 ? `<div class="unread-badge">${room.unreadCount}</div>` : ''}
                </div>
                <div class="room-actions">
                    <button class="room-menu-btn" data-room-id="${room.id}">
                        <i class="fas fa-ellipsis-v"></i>
                    </button>
                </div>
                <div class="online-status ${displayOnlineStatus ? 'online' : 'offline'}"></div>
            `;

            // Click event cho room (không trigger khi click menu)
            roomDiv.addEventListener('click', (e) => {
                if (!e.target.closest('.room-menu-btn')) {
                    this.selectRoom(room.id);
                }
            });

            // Event listener cho menu button
            const menuBtn = roomDiv.querySelector('.room-menu-btn');
            menuBtn.addEventListener('click', (e) => {
                e.stopPropagation();
                this.showRoomMenu(room, e.target.closest('.room-menu-btn'));
            });

            chatRoomsList.appendChild(roomDiv);
        });
    }
    
    // Hiển thị menu cho room
    showRoomMenu(room, button) {
        // Đóng menu cũ nếu có
        this.hideRoomMenu();
        
        // Tạo menu HTML
        const menuHTML = `
            <div class="room-menu" id="roomMenu">
                <div class="menu-item" data-action="delete" data-room-id="${room.id}">
                    <i class="fas fa-trash"></i>
                    <span>Xóa cuộc trò chuyện</span>
                </div>
                ${room.isGroup ? `
                    <div class="menu-item" data-action="add-member" data-room-id="${room.id}">
                        <i class="fas fa-user-plus"></i>
                        <span>Thêm thành viên</span>
                    </div>
                    <div class="menu-item" data-action="group-info" data-room-id="${room.id}">
                        <i class="fas fa-info-circle"></i>
                        <span>Thông tin nhóm</span>
                    </div>
                ` : `
                    <div class="menu-item" data-action="user-info" data-room-id="${room.id}">
                        <i class="fas fa-user"></i>
                        <span>Thông tin người dùng</span>
                    </div>
                `}
            </div>
        `;
        
        // Thêm menu vào body
        document.body.insertAdjacentHTML('beforeend', menuHTML);
        
        const menu = document.getElementById('roomMenu');
        
        // Tính toán vị trí menu
        const buttonRect = button.getBoundingClientRect();
        
        let top = buttonRect.bottom + 5;
        let left = buttonRect.right - 180; // Menu width là 180px
        
        // Điều chỉnh vị trí nếu menu bị tràn màn hình
        if (left < 10) {
            left = 10;
        }
        if (top + 200 > window.innerHeight - 10) { // Ước tính menu height
            top = buttonRect.top - 200 - 5;
        }
        
        menu.style.position = 'fixed';
        menu.style.top = top + 'px';
        menu.style.left = left + 'px';
        menu.style.zIndex = '1000';
        
        // Event listeners cho menu items
        menu.addEventListener('click', (e) => {
            const menuItem = e.target.closest('.menu-item');
            if (menuItem) {
                const action = menuItem.dataset.action;
                const roomId = parseInt(menuItem.dataset.roomId);
                
                switch (action) {
                    case 'delete':
                        this.deleteChatRoom(roomId);
                        break;
                    case 'add-member':
                        this.addMemberToGroup(roomId);
                        break;
                    case 'group-info':
                        this.showGroupInfo(roomId);
                        break;
                    case 'user-info':
                        this.showUserInfo(roomId);
                        break;
                }
                
                this.hideRoomMenu();
            }
        });
        
        // Đóng menu khi click bên ngoài
        setTimeout(() => {
            document.addEventListener('click', this.hideRoomMenu.bind(this), { once: true });
        }, 100);
    }
    
    // Ẩn menu room
    hideRoomMenu() {
        const menu = document.getElementById('roomMenu');
        if (menu) {
            menu.remove();
        }
    }
    
    // Xóa cuộc trò chuyện
    async deleteChatRoom(roomId) {
        const room = this.chatRooms.find(r => r.id === roomId);
        if (!room) return;
        
        const isGroupChat = room.isGroup === true || (room.members && room.members.length > 2);
        const roomName = isGroupChat ? room.roomName : 
                         (room.members && room.members.find(m => m.userId !== this.currentUserId)?.fullName) || 'cuộc trò chuyện';
        
        const confirmMessage = isGroupChat ? 
            `Bạn có chắc chắn muốn xóa nhóm "${roomName}"?` :
            `Bạn có chắc chắn muốn xóa cuộc trò chuyện với "${roomName}"?`;
        
        if (!confirm(confirmMessage + '\n\nHành động này không thể hoàn tác.')) {
            return;
        }
        
        try {
            const response = await authenticatedFetch(`/api/chat/rooms/${roomId}`, {
                method: 'DELETE',
                credentials: 'include'
            });
            
            if (response.ok) {
                // Xóa room khỏi danh sách
                this.chatRooms = this.chatRooms.filter(r => r.id !== roomId);
                this.filteredChatRooms = this.filteredChatRooms.filter(r => r.id !== roomId);
                
                // Nếu đang ở room bị xóa, đóng chat
                if (this.currentRoomId === roomId) {
                    this.currentRoomId = null;
                    this.messages = [];
                    this.renderMessages();
                    this.updateChatHeader(null);
                    
                    // Ẩn chat window
                    const chatWindow = document.getElementById('chatWindow');
                    const chatPlaceholder = document.getElementById('chatPlaceholder');
                    if (chatWindow) chatWindow.style.display = 'none';
                    if (chatPlaceholder) chatPlaceholder.style.display = 'block';
                }
                
                // Re-render danh sách rooms
                this.renderChatRooms();
                
            } else {
                const errorData = await response.json();
                alert('Không thể xóa cuộc trò chuyện: ' + (errorData.message || 'Lỗi không xác định'));
            }
        } catch (error) {
            alert('Lỗi khi xóa cuộc trò chuyện: ' + error.message);
        }
    }
    
    // Thêm thành viên vào nhóm (placeholder)
    addMemberToGroup(roomId) {
        alert('Tính năng thêm thành viên sẽ được phát triển sau');
    }
    
    // Hiển thị thông tin nhóm (placeholder)
    showGroupInfo(roomId) {
        alert('Tính năng thông tin nhóm sẽ được phát triển sau');
    }
    
    // Hiển thị thông tin người dùng (placeholder)
    showUserInfo(roomId) {
        alert('Tính năng thông tin người dùng sẽ được phát triển sau');
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
            
            // Đánh dấu đã đọc khi chọn room
            this.markRoomAsReadIfNeeded();
            
            // Auto open file management
            this.autoOpenFileManagement();
            
        } catch (error) {
            // Error selecting room
        }
    }

    async markRoomAsRead(roomId) {
        try {
            const response = await authenticatedFetch(`/api/chat/rooms/${roomId}/read`, {
                method: 'POST',
                credentials: 'include'
            });

            if (response.ok) {
                
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
            // Error marking room as read
        }
    }

    markRoomAsReadIfNeeded() {
        if (!this.currentRoomId) return;
        
        // Clear timeout cũ nếu có
        if (this.markAsReadTimeout) {
            clearTimeout(this.markAsReadTimeout);
        }
        
        // Debounce để tránh gọi API quá nhiều lần
        this.markAsReadTimeout = setTimeout(() => {
            // Kiểm tra xem có tin nhắn chưa đọc từ người khác không
            const hasUnreadReceivedMessages = this.messages.some(msg => 
                msg.senderId !== this.currentUserId && !msg.isRead
            );
            
            if (hasUnreadReceivedMessages) {
                // Gọi API để đánh dấu đã đọc nhưng không reload messages
                this.markRoomAsReadSilently(this.currentRoomId);
            }
        }, 500); // Debounce 500ms
    }
    
    // Đánh dấu đã đọc mà không reload messages
    async markRoomAsReadSilently(roomId) {
        try {
            const response = await authenticatedFetch(`/api/chat/rooms/${roomId}/read`, {
                method: 'POST',
                credentials: 'include'
            });

            if (response.ok) {
                
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
            // Error marking room as read silently
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
        
        // Xác định tên hiển thị và avatar cho header
        let headerDisplayName, headerDisplayAvatar;
        
        const isGroupChat = room.isGroup === true || (room.members && room.members.length > 2);
        
        if (isGroupChat) {
            // Group chat: hiển thị tên room
            headerDisplayName = room.roomName || room.roomname || 'Group Chat';
            headerDisplayAvatar = room.roomAvatar || room.roomavatar || 'https://cdn.pixabay.com/photo/2023/02/18/11/00/icon-7797704_640.png';
        } else {
            // Private chat: hiển thị tên và avatar của người đối diện
            const otherMember = room.members && room.members.find(member => member.userId !== this.currentUserId);
            if (otherMember) {
                headerDisplayName = otherMember.fullName || otherMember.username || 'Unknown User';
                headerDisplayAvatar = otherMember.avatar || 'https://cdn.pixabay.com/photo/2023/02/18/11/00/icon-7797704_640.png';
            } else {
                headerDisplayName = room.roomName || room.roomname || 'Private Chat';
                headerDisplayAvatar = room.roomAvatar || room.roomavatar || 'https://cdn.pixabay.com/photo/2023/02/18/11/00/icon-7797704_640.png';
            }
        }
        
        if (chatUserName) {
            chatUserName.textContent = headerDisplayName;
        }
        
        if (chatAvatar) {
            chatAvatar.src = headerDisplayAvatar;
            chatAvatar.onerror = function() {
                this.src = 'https://cdn.pixabay.com/photo/2023/02/18/11/00/icon-7797704_640.png';
            };
        }
        
        // Chỉ hiển thị online status cho private chat
        if (!room.isGroup) {
            const otherMember = room.members && room.members.find(member => member.userId !== this.currentUserId);
            const isOtherMemberOnline = otherMember && otherMember.isOnline;
            
            if (onlineStatus) {
                onlineStatus.textContent = isOtherMemberOnline ? 'Trực tuyến' : 'Ngoại tuyến';
            }
            
            if (onlineIndicator) {
                onlineIndicator.style.display = 'block';
                onlineIndicator.className = `online-indicator ${isOtherMemberOnline ? 'online' : 'offline'}`;
            }
        } else {
            // Group chat: hiển thị "Nhóm"
            if (onlineStatus) {
                onlineStatus.textContent = 'Nhóm';
            }
            
            if (onlineIndicator) {
                onlineIndicator.style.display = 'none';
            }
        }
    }

    async loadRoomMessages(roomId) {
        try {
            const response = await authenticatedFetch(`/api/chat/rooms/${roomId}/messages`, {
                method: 'GET',
                credentials: 'include'
            });

            if (response.ok) {
                const data = await response.json();
                if (data.success) {
                    this.messages = data.data || [];
                    this.renderMessages();
                }
            }
        } catch (error) {
            // Error loading room messages
        }
    }

    renderMessages() {
        const chatMessages = document.getElementById('chatMessages');
        if (!chatMessages) {
            return;
        }

        chatMessages.innerHTML = '';

        if (this.messages.length === 0) {
            chatMessages.innerHTML = '<div class="no-messages">Chưa có tin nhắn nào</div>';
            return;
        }

        // Tìm tin nhắn cuối cùng của người gửi hiện tại
        const lastSentMessageIndex = this.findLastSentMessageIndex();
        
        this.messages.forEach((message, index) => {
            const messageDiv = document.createElement('div');
            messageDiv.className = `message ${message.senderId === this.currentUserId ? 'sent' : 'received'}`;
            messageDiv.setAttribute('data-message-id', message.id);
            
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
            
            // Xử lý file messages
            let messageContent = message.content || '';
            if (message.attachments && message.attachments.length > 0) {
                // Single attachment message - hiển thị ảnh trực tiếp
                if (message.attachments.length === 1) {
                    messageContent = this.createFileMessageHTML(message.attachments[0]);
                } else {
                    // Multiple attachments message - hiển thị ảnh trực tiếp
                    messageContent = this.createFilesMessageHTML(message.attachments);
                }
            } else if (message.file) {
                // Backward compatibility - Single file message
                messageContent = this.createFileMessageHTML(message.file);
            } else if (message.files && message.files.length > 0) {
                // Backward compatibility - Multiple files message
                messageContent = this.createFilesMessageHTML(message.files);
            }
            
            messageDiv.innerHTML = `
                <div class="message-content">${messageContent}</div>
                <div class="message-time">${this.formatTime(message.createdAt)}</div>
                ${readStatus}
            `;

            chatMessages.appendChild(messageDiv);
        });

        // Scroll to bottom
        this.scrollToBottom();
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
            const response = await authenticatedFetch(`/api/chat/rooms/${this.currentRoomId}/messages`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                credentials: 'include',
                body: JSON.stringify({ content: content })
            });

            if (response.ok) {
                const data = await response.json();
                if (data.success) {
                    // Xóa tin nhắn tạm và thêm tin nhắn thật từ server
                    this.removeSendingMessage();
                    this.addMessageToUI(data.data);
                }
            } else {
                // Nếu gửi thất bại, xóa tin nhắn tạm
                this.removeSendingMessage();
            }
        } catch (error) {
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
            
            // Modal functionality
            this.setupModalEventListeners();
            
        }

        // Click event để đánh dấu đã đọc khi click vào chat area
        const chatMessages = document.getElementById('chatMessages');
        if (chatMessages) {
            chatMessages.addEventListener('click', () => {
                this.markRoomAsReadIfNeeded();
            });
        }
        
        // Focus event để đánh dấu đã đọc khi focus vào input
        const messageInputFocus = document.getElementById('messageInput');
        if (messageInputFocus) {
            messageInputFocus.addEventListener('focus', () => {
                this.markRoomAsReadIfNeeded();
            });
        }

        // Toggle files button
        const toggleFilesBtn = document.getElementById('toggleFilesBtn');
        if (toggleFilesBtn) {
            toggleFilesBtn.addEventListener('click', () => {
                this.toggleFileManagement();
            });
        }

        // Search messages button
        const searchMessagesBtn = document.getElementById('searchMessagesBtn');
        if (searchMessagesBtn) {
            searchMessagesBtn.addEventListener('click', () => {
                this.toggleSearchBar();
            });
        }

        // Close search button
        const closeSearchBtn = document.getElementById('closeSearchBtn');
        if (closeSearchBtn) {
            closeSearchBtn.addEventListener('click', () => {
                this.closeSearchBar();
            });
        }

        // Search input
        const messageSearchInput = document.getElementById('messageSearchInput');
        if (messageSearchInput) {
            messageSearchInput.addEventListener('input', (e) => {
                this.searchMessages(e.target.value);
            });
        }
    }

    autoOpenFileManagement() {
        if (!this.currentRoomId) {
            return;
        }

        if (window.chatFileManagement) {
            const chatMain = document.querySelector('.chat-main');
            const filesIcon = document.getElementById('filesIcon');
            
            // Auto open file management
            window.chatFileManagement.openSidebar(this.currentRoomId);
            if (chatMain) chatMain.classList.add('with-files-sidebar');
            if (filesIcon) filesIcon.className = 'fas fa-folder-open';
        }
    }

    toggleFileManagement() {
        if (!this.currentRoomId) {
            console.log('No room selected');
            return;
        }

        if (window.chatFileManagement) {
            const sidebar = document.getElementById('chatFilesSidebar');
            const chatMain = document.querySelector('.chat-main');
            const filesIcon = document.getElementById('filesIcon');
            
            if (sidebar && sidebar.classList.contains('show')) {
                // Close sidebar
                window.chatFileManagement.closeSidebar();
                if (chatMain) chatMain.classList.remove('with-files-sidebar');
                if (filesIcon) filesIcon.className = 'fas fa-folder';
            } else {
                // Open sidebar
                window.chatFileManagement.openSidebar(this.currentRoomId);
                if (chatMain) chatMain.classList.add('with-files-sidebar');
                if (filesIcon) filesIcon.className = 'fas fa-folder-open';
            }
        } else {
            console.error('ChatFileManagement not initialized');
        }
    }

    toggleSearchBar() {
        const searchBar = document.getElementById('chatSearchBar');
        const searchInput = document.getElementById('messageSearchInput');
        
        if (searchBar && searchInput) {
            if (searchBar.style.display === 'none') {
                searchBar.style.display = 'flex';
                searchInput.focus();
            } else {
                this.closeSearchBar();
            }
        }
    }

    closeSearchBar() {
        const searchBar = document.getElementById('chatSearchBar');
        const searchInput = document.getElementById('messageSearchInput');
        const searchResults = document.getElementById('searchResults');
        
        if (searchBar) {
            searchBar.style.display = 'none';
        }
        if (searchInput) {
            searchInput.value = '';
        }
        if (searchResults) {
            searchResults.innerHTML = '';
        }
    }

    searchMessages(query) {
        if (!query || query.trim() === '') {
            this.clearSearchResults();
            return;
        }

        const results = this.messages.filter(message => {
            const content = message.content || '';
            return content.toLowerCase().includes(query.toLowerCase());
        });

        console.log('Search results:', results);
        console.log('Current messages:', this.messages);

        this.displaySearchResults(results, query);
    }

    displaySearchResults(results, query) {
        const searchResults = document.getElementById('searchResults');
        if (!searchResults) return;

        if (results.length === 0) {
            searchResults.innerHTML = '<div class="no-search-results">Không tìm thấy tin nhắn nào</div>';
            return;
        }

        const html = results.map(message => {
            const highlightedContent = this.highlightSearchTerm(message.content || '', query);
            const time = this.formatTime(message.createdAt);
            const messageId = message.id || message.messageId; // Fallback for different ID fields
            
            return `
                <div class="search-result-item" data-message-id="${messageId}">
                    <div class="search-result-content">${highlightedContent}</div>
                    <div class="search-result-meta">
                        <span>${time}</span>
                        <span>${message.senderName || 'Unknown'}</span>
                    </div>
                </div>
            `;
        }).join('');

        searchResults.innerHTML = html;

        // Add click handlers for search results
        searchResults.querySelectorAll('.search-result-item').forEach(item => {
            item.addEventListener('click', (e) => {
                e.preventDefault();
                const messageId = item.dataset.messageId;
                console.log('Clicked search result for message ID:', messageId);
                
                // Close search bar first
                this.closeSearchBar();
                
                // Then scroll to message
                setTimeout(() => {
                    this.scrollToMessage(messageId);
                }, 100);
            });
        });
    }

    highlightSearchTerm(text, query) {
        if (!text || !query) return text;
        
        const regex = new RegExp(`(${query})`, 'gi');
        return text.replace(regex, '<span class="search-highlight">$1</span>');
    }

    clearSearchResults() {
        const searchResults = document.getElementById('searchResults');
        if (searchResults) {
            searchResults.innerHTML = '';
        }
    }

    scrollToMessage(messageId) {
        console.log('Scrolling to message ID:', messageId);
        
        // Tìm message element trong chat messages
        const messageElement = document.querySelector(`.chat-messages [data-message-id="${messageId}"]`);
        
        if (messageElement) {
            console.log('Found message element:', messageElement);
            
            // Scroll đến message
            messageElement.scrollIntoView({ 
                behavior: 'smooth', 
                block: 'center',
                inline: 'nearest'
            });
            
            // Add highlight effect using CSS class
            messageElement.classList.add('highlighted');
            
            // Remove highlight after 3 seconds
            setTimeout(() => {
                messageElement.classList.remove('highlighted');
            }, 3000);
        } else {
            console.log('Message element not found for ID:', messageId);
            
            // Fallback: scroll to bottom and try again
            this.scrollToBottom();
            setTimeout(() => {
                const retryElement = document.querySelector(`.chat-messages [data-message-id="${messageId}"]`);
                if (retryElement) {
                    retryElement.scrollIntoView({ 
                        behavior: 'smooth', 
                        block: 'center',
                        inline: 'nearest'
                    });
                    
                    // Add highlight effect using CSS class
                    retryElement.classList.add('highlighted');
                    
                    setTimeout(() => {
                        retryElement.classList.remove('highlighted');
                    }, 3000);
                } else {
                    console.log('Message still not found after retry');
                }
            }, 500);
        }
    }

    formatTime(dateString) {
        const date = new Date(dateString);
        return date.toLocaleTimeString('vi-VN', { 
            hour: '2-digit', 
            minute: '2-digit' 
        });
    }
    
    // Lấy thời gian hiển thị cho room (ưu tiên thời gian tin nhắn cuối)
    getRoomDisplayTime(room) {
        let dateToShow;
        
        // Ưu tiên thời gian tin nhắn cuối
        if (room.lastMessage && room.lastMessage.createdAt) {
            dateToShow = new Date(room.lastMessage.createdAt);
        } else if (room.updatedAt) {
            dateToShow = new Date(room.updatedAt);
        } else {
            dateToShow = new Date();
        }
        
        return this.formatRelativeTime(dateToShow);
    }
    
    // Format thời gian tương đối
    formatRelativeTime(date) {
        const now = new Date();
        const diffMs = now - date;
        const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));
        
        if (diffDays === 0) {
            // Hôm nay: chỉ hiển thị giờ:phút
            return date.toLocaleTimeString('vi-VN', { 
                hour: '2-digit', 
                minute: '2-digit' 
            });
        } else if (diffDays === 1) {
            // Hôm qua
            return 'Hôm qua';
        } else if (diffDays < 7) {
            // Trong tuần: hiển thị thứ
            return date.toLocaleDateString('vi-VN', { weekday: 'short' });
        } else {
            // Cũ hơn: hiển thị ngày/tháng
            return date.toLocaleDateString('vi-VN', { 
                day: '2-digit', 
                month: '2-digit' 
            });
        }
    }
    
    // Khởi tạo WebSocket connection
    initializeWebSocket() {
        if (window.chatWebSocketManager) {
            window.chatWebSocketManager.connect();
            
            // Dừng polling nếu WebSocket hoạt động
            this.stopPolling();
        } else {
            this.startPolling();
        }
    }
    
    // Thêm message vào UI (được gọi từ WebSocket)
    addMessageToUI(messageData) {
        // Kiểm tra xem message đã tồn tại chưa
        const existingMessage = this.messages.find(m => m.id === messageData.id);
        if (existingMessage) {
            return; // Message đã tồn tại, không thêm lại
        }
        
        // Thêm message mới
        this.messages.push(messageData);
        
        // Render lại messages
        this.renderMessages();
        
        // Scroll xuống cuối
        this.scrollToBottom();
    }
    
    // Scroll xuống cuối chat messages
    scrollToBottom() {
        const chatMessages = document.getElementById('chatMessages');
        if (chatMessages) {
            // Sử dụng setTimeout để đảm bảo DOM đã được cập nhật
            setTimeout(() => {
                // Kiểm tra nếu có tin nhắn thì mới scroll
                if (chatMessages.children.length > 0) {
                    chatMessages.scrollTop = chatMessages.scrollHeight;
                }
            }, 100);
        }
    }
    
    // Tạo HTML cho single file message
    createFileMessageHTML(file) {
        // Nếu là ảnh, hiển thị ảnh trực tiếp như Facebook
        if (file.fileType === 'image' || file.attachmentType === 'IMAGE') {
            return `
                <div class="image-message">
                    <img src="${file.previewUrl || file.downloadUrl}" 
                         alt="${file.originalName}" 
                         class="chat-image"
                         onclick="window.open('${file.previewUrl || file.downloadUrl}', '_blank')"
                         loading="lazy">
                    <div class="image-overlay">
                        <div class="image-actions">
                            <a href="${file.downloadUrl}" target="_blank" class="image-download" title="Tải về">
                                <i class="fas fa-download"></i>
                            </a>
                            <a href="${file.previewUrl || file.downloadUrl}" target="_blank" class="image-preview" title="Xem to">
                                <i class="fas fa-expand"></i>
                            </a>
                        </div>
                    </div>
                </div>
            `;
        }
        
        // Nếu là video, hiển thị video player
        if (file.fileType === 'video' || file.attachmentType === 'VIDEO') {
            return `
                <div class="video-message">
                    <video controls class="chat-video" preload="metadata">
                        <source src="${file.previewUrl || file.downloadUrl}" type="${file.mimeType}">
                        Trình duyệt không hỗ trợ video.
                    </video>
                    <div class="video-info">
                        <div class="file-name">${file.originalName}</div>
                        <div class="file-size">${this.formatFileSize(file.fileSize)}</div>
                    </div>
                </div>
            `;
        }
        
        // Nếu là audio, hiển thị audio player
        if (file.fileType === 'audio' || file.attachmentType === 'AUDIO') {
            return `
                <div class="audio-message">
                    <audio controls class="chat-audio">
                        <source src="${file.previewUrl || file.downloadUrl}" type="${file.mimeType}">
                        Trình duyệt không hỗ trợ audio.
                    </audio>
                    <div class="audio-info">
                        <div class="file-name">${file.originalName}</div>
                        <div class="file-size">${this.formatFileSize(file.fileSize)}</div>
                    </div>
                </div>
            `;
        }
        
        // Các file khác (document, etc.) - click để tải về
        const fileIcon = this.getFileIcon(file.fileType || file.attachmentType);
        const fileSize = this.formatFileSize(file.fileSize);
        
        return `
            <div class="file-message clickable" onclick="window.open('${file.downloadUrl}', '_blank')">
                <div class="file-icon">${fileIcon}</div>
                <div class="file-info">
                    <div class="file-name">${file.originalName}</div>
                    <div class="file-size">${fileSize}</div>
                </div>
                <div class="file-actions">
                    <a href="${file.downloadUrl}" target="_blank" class="file-download" title="Tải về" onclick="event.stopPropagation()">
                        <i class="fas fa-download"></i>
                    </a>
                    <a href="${file.previewUrl}" target="_blank" class="file-preview" title="Xem" onclick="event.stopPropagation()">
                        <i class="fas fa-eye"></i>
                    </a>
                </div>
            </div>
        `;
    }
    
    // Tạo HTML cho multiple files message
    createFilesMessageHTML(files) {
        // Phân loại files theo type
        const images = files.filter(file => file.fileType === 'image' || file.attachmentType === 'IMAGE');
        const videos = files.filter(file => file.fileType === 'video' || file.attachmentType === 'VIDEO');
        const audios = files.filter(file => file.fileType === 'audio' || file.attachmentType === 'AUDIO');
        const documents = files.filter(file => 
            file.fileType === 'document' || 
            file.attachmentType === 'DOCUMENT' || 
            file.attachmentType === 'OTHER'
        );
        
        let content = '';
        
        // Hiển thị ảnh như gallery
        if (images.length > 0) {
            const imagesHTML = images.map(file => `
                <div class="gallery-item">
                    <img src="${file.previewUrl || file.downloadUrl}" 
                         alt="${file.originalName}" 
                         class="gallery-image"
                         onclick="window.open('${file.previewUrl || file.downloadUrl}', '_blank')"
                         loading="lazy">
                </div>
            `).join('');
            
            content += `
                <div class="images-gallery">
                    ${imagesHTML}
                </div>
            `;
        }
        
        // Hiển thị video
        if (videos.length > 0) {
            const videosHTML = videos.map(file => `
                <div class="video-item">
                    <video controls class="gallery-video" preload="metadata">
                        <source src="${file.previewUrl || file.downloadUrl}" type="${file.mimeType}">
                        Trình duyệt không hỗ trợ video.
                    </video>
                    <div class="video-name">${file.originalName}</div>
                </div>
            `).join('');
            
            content += `
                <div class="videos-gallery">
                    ${videosHTML}
                </div>
            `;
        }
        
        // Hiển thị audio
        if (audios.length > 0) {
            const audiosHTML = audios.map(file => `
                <div class="audio-item">
                    <audio controls class="gallery-audio">
                        <source src="${file.previewUrl || file.downloadUrl}" type="${file.mimeType}">
                        Trình duyệt không hỗ trợ audio.
                    </audio>
                    <div class="audio-name">${file.originalName}</div>
                </div>
            `).join('');
            
            content += `
                <div class="audios-gallery">
                    ${audiosHTML}
                </div>
            `;
        }
        
        // Hiển thị documents như cũ
        if (documents.length > 0) {
            const documentsHTML = documents.map(file => {
                const fileIcon = this.getFileIcon(file.fileType || file.attachmentType);
                const fileSize = this.formatFileSize(file.fileSize);
                
                return `
                    <div class="file-item">
                        <div class="file-icon">${fileIcon}</div>
                        <div class="file-info">
                            <div class="file-name">${file.originalName}</div>
                            <div class="file-size">${fileSize}</div>
                        </div>
                        <div class="file-actions">
                            <a href="${file.downloadUrl}" target="_blank" class="file-download" title="Tải về">
                                <i class="fas fa-download"></i>
                            </a>
                            <a href="${file.previewUrl}" target="_blank" class="file-preview" title="Xem">
                                <i class="fas fa-eye"></i>
                            </a>
                        </div>
                    </div>
                `;
            }).join('');
            
            content += `
                <div class="documents-list">
                    <div class="files-header">📄 Tài liệu (${documents.length}):</div>
                    <div class="files-list">${documentsHTML}</div>
                </div>
            `;
        }
        
        return `
            <div class="files-message">
                ${content}
            </div>
        `;
    }
    
    // Lấy icon cho file type
    getFileIcon(fileType) {
        switch (fileType) {
            case 'image': return '🖼️';
            case 'video': return '🎥';
            case 'document': return '📄';
            default: return '📎';
        }
    }
    
    // Format file size
    formatFileSize(bytes) {
        if (bytes === 0) return '0 Bytes';
        const k = 1024;
        const sizes = ['Bytes', 'KB', 'MB', 'GB'];
        const i = Math.floor(Math.log(bytes) / Math.log(k));
        return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
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

    // Tìm kiếm chat rooms - Tối ưu không phân biệt hoa thường
    searchChatRooms(query) {
        this.searchQuery = query.toLowerCase().trim();
        
        if (!this.searchQuery) {
            this.filteredChatRooms = [...this.chatRooms];
        } else {
            // Tách từ khóa thành các từ riêng biệt để tìm kiếm linh hoạt hơn
            const searchTerms = this.searchQuery.split(/\s+/).filter(term => term.length > 0);
            
            this.filteredChatRooms = this.chatRooms.filter(room => {
                // Tìm kiếm theo tên room
                const roomName = (room.roomName || room.roomname || '').toLowerCase();
                const roomNameMatch = searchTerms.every(term => roomName.includes(term));
                
                // Tìm kiếm theo tên thành viên (fullName và username)
                const memberMatch = room.members && room.members.some(member => {
                    const fullName = (member.fullName || '').toLowerCase();
                    const username = (member.username || '').toLowerCase();
                    return searchTerms.every(term => 
                        fullName.includes(term) || username.includes(term)
                    );
                });
                
                // Tìm kiếm theo tin nhắn cuối cùng
                const lastMessageContent = (room.lastMessage && room.lastMessage.content ? 
                    room.lastMessage.content.toLowerCase() : '');
                const lastUnreadContent = (room.lastUnreadMessage ? 
                    room.lastUnreadMessage.toLowerCase() : '');
                const lastMessageMatch = searchTerms.every(term => 
                    lastMessageContent.includes(term) || lastUnreadContent.includes(term)
                );
                
                // Tìm kiếm theo email (nếu có)
                const emailMatch = room.members && room.members.some(member => {
                    const email = (member.email || '').toLowerCase();
                    return searchTerms.every(term => email.includes(term));
                });
                
                return roomNameMatch || memberMatch || lastMessageMatch || emailMatch;
            });
        }
        
        this.renderChatRooms();
    }
    
    // Hàm loại bỏ dấu tiếng Việt
    removeVietnameseDiacritics(str) {
        if (!str) return '';
        
        const diacriticsMap = {
            'à': 'a', 'á': 'a', 'ạ': 'a', 'ả': 'a', 'ã': 'a', 'â': 'a', 'ầ': 'a', 'ấ': 'a', 'ậ': 'a', 'ẩ': 'a', 'ẫ': 'a', 'ă': 'a', 'ằ': 'a', 'ắ': 'a', 'ặ': 'a', 'ẳ': 'a', 'ẵ': 'a',
            'è': 'e', 'é': 'e', 'ẹ': 'e', 'ẻ': 'e', 'ẽ': 'e', 'ê': 'e', 'ề': 'e', 'ế': 'e', 'ệ': 'e', 'ể': 'e', 'ễ': 'e',
            'ì': 'i', 'í': 'i', 'ị': 'i', 'ỉ': 'i', 'ĩ': 'i',
            'ò': 'o', 'ó': 'o', 'ọ': 'o', 'ỏ': 'o', 'õ': 'o', 'ô': 'o', 'ồ': 'o', 'ố': 'o', 'ộ': 'o', 'ổ': 'o', 'ỗ': 'o', 'ơ': 'o', 'ờ': 'o', 'ớ': 'o', 'ợ': 'o', 'ở': 'o', 'ỡ': 'o',
            'ù': 'u', 'ú': 'u', 'ụ': 'u', 'ủ': 'u', 'ũ': 'u', 'ư': 'u', 'ừ': 'u', 'ứ': 'u', 'ự': 'u', 'ử': 'u', 'ữ': 'u',
            'ỳ': 'y', 'ý': 'y', 'ỵ': 'y', 'ỷ': 'y', 'ỹ': 'y',
            'đ': 'd',
            'À': 'A', 'Á': 'A', 'Ạ': 'A', 'Ả': 'A', 'Ã': 'A', 'Â': 'A', 'Ầ': 'A', 'Ấ': 'A', 'Ậ': 'A', 'Ẩ': 'A', 'Ẫ': 'A', 'Ă': 'A', 'Ằ': 'A', 'Ắ': 'A', 'Ặ': 'A', 'Ẳ': 'A', 'Ẵ': 'A',
            'È': 'E', 'É': 'E', 'Ẹ': 'E', 'Ẻ': 'E', 'Ẽ': 'E', 'Ê': 'E', 'Ề': 'E', 'Ế': 'E', 'Ệ': 'E', 'Ể': 'E', 'Ễ': 'E',
            'Ì': 'I', 'Í': 'I', 'Ị': 'I', 'Ỉ': 'I', 'Ĩ': 'I',
            'Ò': 'O', 'Ó': 'O', 'Ọ': 'O', 'Ỏ': 'O', 'Õ': 'O', 'Ô': 'O', 'Ồ': 'O', 'Ố': 'O', 'Ộ': 'O', 'Ổ': 'O', 'Ỗ': 'O', 'Ơ': 'O', 'Ờ': 'O', 'Ớ': 'O', 'Ợ': 'O', 'Ở': 'O', 'Ỡ': 'O',
            'Ù': 'U', 'Ú': 'U', 'Ụ': 'U', 'Ủ': 'U', 'Ũ': 'U', 'Ư': 'U', 'Ừ': 'U', 'Ứ': 'U', 'Ự': 'U', 'Ử': 'U', 'Ữ': 'U',
            'Ỳ': 'Y', 'Ý': 'Y', 'Ỵ': 'Y', 'Ỷ': 'Y', 'Ỹ': 'Y',
            'Đ': 'D'
        };
        
        return str.replace(/[àáạảãâầấậẩẫăằắặẳẵèéẹẻẽêềếệểễìíịỉĩòóọỏõôồốộổỗơờớợởỡùúụủũưừứựửữỳýỵỷỹđÀÁẠẢÃÂẦẤẬẨẪĂẰẮẶẲẴÈÉẸẺẼÊỀẾỆỂỄÌÍỊỈĨÒÓỌỎÕÔỒỐỘỔỖƠỜỚỢỞỠÙÚỤỦŨƯỪỨỰỬỮỲÝỴỶỸĐ]/g, function(match) {
            return diacriticsMap[match] || match;
        });
    }
    
    // Hàm tìm kiếm nâng cao với fuzzy matching cho tiếng Việt (không phân biệt hoa thường)
    fuzzySearch(text, query) {
        if (!text || !query) return false;
        
        // Chuẩn hóa về lowercase
        const textLower = text.toLowerCase().trim();
        const queryLower = query.toLowerCase().trim();
        
        // Tìm kiếm chính xác trước
        if (textLower.includes(queryLower)) return true;
        
        // Tìm kiếm không dấu
        const textNoDiacritics = this.removeVietnameseDiacritics(textLower);
        const queryNoDiacritics = this.removeVietnameseDiacritics(queryLower);
        
        if (textNoDiacritics.includes(queryNoDiacritics)) return true;
        
        // Tìm kiếm fuzzy (bỏ qua dấu, khoảng trắng)
        const normalizeText = textNoDiacritics.replace(/[^\w]/g, '');
        const normalizeQuery = queryNoDiacritics.replace(/[^\w]/g, '');
        
        if (normalizeText.includes(normalizeQuery)) return true;
        
        // Tìm kiếm theo từng ký tự (cho phép thiếu một vài ký tự)
        let textIndex = 0;
        let queryIndex = 0;
        
        while (textIndex < normalizeText.length && queryIndex < normalizeQuery.length) {
            if (normalizeText[textIndex] === normalizeQuery[queryIndex]) {
                queryIndex++;
            }
            textIndex++;
        }
        
        return queryIndex === normalizeQuery.length;
    }
    
    
    // Hàm highlight text tìm kiếm
    highlightText(text, query) {
        if (!text || !query) return text;
        
        const regex = new RegExp(`(${query.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')})`, 'gi');
        return text.replace(regex, '<mark class="search-highlight">$1</mark>');
    }
    
    // Hàm highlight đơn giản và chính xác
    highlightMultipleFields(text, query) {
        if (!text || !query) return text;
        
        const searchTerms = query.toLowerCase().split(/\s+/).filter(term => term.length > 0);
        let highlightedText = text;
        
        searchTerms.forEach(term => {
            // Highlight chính xác (case insensitive)
            const exactRegex = new RegExp(`(${term.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')})`, 'gi');
            highlightedText = highlightedText.replace(exactRegex, '<mark class="search-highlight">$1</mark>');
        });
        
        return highlightedText;
    }
    
    // Hàm highlight đơn giản
    highlightCombinedFields(user, query) {
        const firstName = user.firstName || '';
        const lastName = user.lastName || '';
        const username = user.username || '';
        const fullName = `${firstName} ${lastName}`.trim();
        
        return {
            fullName: this.highlightMultipleFields(fullName, query),
            username: this.highlightMultipleFields(username, query),
            combined: this.highlightMultipleFields(fullName, query)
        };
    }
    
    // Hàm tìm vị trí gốc trong text có dấu từ vị trí trong text không dấu
    findOriginalPosition(originalText, noDiacriticsText, position) {
        let originalPos = 0;
        let noDiacriticsPos = 0;
        
        while (originalPos < originalText.length && noDiacriticsPos < position) {
            const originalChar = originalText[originalPos];
            const noDiacriticsChar = this.removeVietnameseDiacritics(originalChar);
            
            if (noDiacriticsChar.length > 0) {
                noDiacriticsPos++;
            }
            
            if (noDiacriticsPos <= position) {
                originalPos++;
            }
        }
        
        return originalPos;
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
            
            // Thêm event listener cho real-time search
            searchInput.addEventListener('input', (e) => {
                this.searchChatRooms(e.target.value);
            });
            
            // Thêm event listener cho Enter key
            searchInput.addEventListener('keydown', (e) => {
                if (e.key === 'Enter') {
                    e.preventDefault();
                    this.searchChatRooms(e.target.value);
                }
            });
            
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

    // Setup event listeners cho modal
    setupModalEventListeners() {
        // Close modal buttons
        const closeModalBtn = document.getElementById('closeModalBtn');
        const modalOverlay = document.getElementById('modalOverlay');
        const modal = document.getElementById('newChatModal');
        
        if (closeModalBtn) {
            closeModalBtn.addEventListener('click', () => {
                this.hideNewChatModal();
            });
        }
        
        if (modalOverlay) {
            modalOverlay.addEventListener('click', () => {
                this.hideNewChatModal();
            });
        }
        
        // User search input với tính năng nâng cao
        const userSearchInput = document.getElementById('userSearchInput');
        if (userSearchInput) {
            let searchTimeout;
            
            userSearchInput.addEventListener('input', (e) => {
                clearTimeout(searchTimeout);
                const query = e.target.value.trim();
                this.searchQuery = query;
                
                if (query.length >= 2) {
                    searchTimeout = setTimeout(() => {
                        this.searchUsers(query);
                        this.addToRecentSearches(query);
                    }, 300);
                } else if (query.length === 1) {
                    // Hiển thị gợi ý khi gõ 1 ký tự
                    this.showSearchSuggestions(query);
                } else if (query.length === 0) {
                    this.clearSearchResults();
                }
            });
            
            // Enter key to search
            userSearchInput.addEventListener('keypress', (e) => {
                if (e.key === 'Enter') {
                    e.preventDefault();
                    const query = e.target.value.trim();
                    if (query.length >= 2) {
                        this.searchUsers(query);
                        this.addToRecentSearches(query);
                    }
                }
            });
            
            // Focus event - hiển thị lịch sử tìm kiếm
            userSearchInput.addEventListener('focus', (e) => {
                if (e.target.value.length === 0) {
                    this.showRecentSearches();
                }
            });
        }
    }

    // Hiển thị modal tạo chat mới
    showNewChatModal() {
        const modal = document.getElementById('newChatModal');
        if (modal) {
            modal.style.display = 'flex';
            const searchInput = document.getElementById('userSearchInput');
            if (searchInput) {
                searchInput.value = '';
                searchInput.focus();
            }
            this.clearSearchResults();
            this.clearSelectedUsers();
        }
    }
    
    // Ẩn modal tạo chat mới
    hideNewChatModal() {
        const modal = document.getElementById('newChatModal');
        if (modal) {
            modal.style.display = 'none';
        }
    }
    
    // Tìm kiếm user - Tối ưu không phân biệt hoa thường với toán tử nâng cao
    async searchUsers(query) {
        const searchResults = document.getElementById('userSearchResults');
        if (!searchResults) return;
        
        // Normalize query - loại bỏ khoảng trắng thừa và chuyển về lowercase
        const normalizedQuery = query.toLowerCase().trim();
        
        if (normalizedQuery.length < 2) {
            this.clearSearchResults();
            return;
        }
        
        searchResults.innerHTML = '<div class="text-center py-3 text-muted"><i class="fas fa-spinner fa-spin"></i> Đang tìm kiếm...</div>';
        
        try {
            // Phân tích query để tìm toán tử
            const searchParams = this.parseSearchQuery(normalizedQuery);
            
            // Tìm kiếm tất cả kết quả (không phân trang)
            const apiUrl = `/api/users/search?q=${encodeURIComponent(searchParams.query)}`;
            
            const response = await authenticatedFetch(apiUrl, {
                method: 'GET',
                credentials: 'include'
            });
            
            if (response.ok) {
                const data = await response.json();
                this.lastSearchResults = data.items || [];
                
                // Lọc kết quả phía client với toán tử nâng cao
                const filteredResults = this.filterUserSearchResultsAdvanced(this.lastSearchResults, searchParams);
                this.renderSearchResults(filteredResults);
            } else {
                const errorText = await response.text();
                searchResults.innerHTML = '<div class="text-center py-3 text-danger">Không thể tìm kiếm người dùng (Status: ' + response.status + ')</div>';
            }
        } catch (error) {
            searchResults.innerHTML = '<div class="text-center py-3 text-danger">Lỗi khi tìm kiếm: ' + error.message + '</div>';
        }
    }
    
    // Phân tích query tìm kiếm để tìm toán tử
    parseSearchQuery(query) {
        const params = {
            query: query,
            fields: [],
            exact: false,
            startsWith: false,
            contains: true
        };
        
        // Kiểm tra toán tử
        if (query.startsWith('"') && query.endsWith('"')) {
            // Tìm kiếm chính xác: "nguyen van"
            params.exact = true;
            params.query = query.slice(1, -1);
        } else if (query.startsWith('^')) {
            // Tìm kiếm bắt đầu: ^nguyen
            params.startsWith = true;
            params.query = query.slice(1);
        } else if (query.includes(':')) {
            // Tìm kiếm theo trường: name:nguyen, email:gmail
            const parts = query.split(':');
            if (parts.length === 2) {
                const field = parts[0].trim();
                const value = parts[1].trim();
                
                switch (field) {
                    case 'name':
                    case 'n':
                        params.fields = ['firstName', 'lastName', 'fullName'];
                        break;
                    case 'username':
                    case 'u':
                        params.fields = ['username'];
                        break;
                    case 'email':
                    case 'e':
                        params.fields = ['email'];
                        break;
                    default:
                        params.fields = ['firstName', 'lastName', 'username', 'email'];
                }
                
                params.query = value;
            }
        }
        
        return params;
    }
    
    // Lọc kết quả đơn giản - chỉ cần có dấu hiệu khớp
    filterUserSearchResultsAdvanced(users, searchParams) {
        if (!searchParams.query) return users;
        
        const query = searchParams.query.toLowerCase().trim();
        
        const filtered = users.filter(user => {
            // Chuẩn hóa dữ liệu
            const firstName = (user.firstName || '').toLowerCase().trim();
            const lastName = (user.lastName || '').toLowerCase().trim();
            const username = (user.username || '').toLowerCase().trim();
            const email = (user.email || '').toLowerCase().trim();
            const fullName = `${firstName} ${lastName}`.trim();
            
            // Tạo chuỗi tìm kiếm tổng hợp
            const searchText = `${firstName} ${lastName} ${username} ${email}`.trim();
            
            // Tạo phiên bản không dấu
            const searchTextNoDiacritics = this.removeVietnameseDiacritics(searchText);
            const queryNoDiacritics = this.removeVietnameseDiacritics(query);
            
            // Tách từ khóa tìm kiếm
            const searchTerms = query.split(/\s+/).filter(term => term.length > 0);
            
            // Kiểm tra từng từ khóa - chỉ cần 1 từ khớp là được
            const matches = searchTerms.some(term => {
                const termNoDiacritics = this.removeVietnameseDiacritics(term);
                
                // Kiểm tra có dấu
                if (searchText.includes(term)) return true;
                
                // Kiểm tra không dấu
                if (searchTextNoDiacritics.includes(termNoDiacritics)) return true;
                
                return false;
            });
            
            return matches;
        });
        
        return filtered;
    }
    
    // Lọc kết quả tìm kiếm user phía client - Tối ưu đa trường
    filterUserSearchResults(users, query) {
        if (!query) return users;
        
        const searchTerms = query.split(/\s+/).filter(term => term.length > 0);
        
        return users.filter(user => {
            // Chuẩn hóa dữ liệu
            const firstName = (user.firstName || '').toLowerCase().trim();
            const lastName = (user.lastName || '').toLowerCase().trim();
            const username = (user.username || '').toLowerCase().trim();
            const email = (user.email || '').toLowerCase().trim();
            
            // Tạo các biến thể tên
            const fullName = `${firstName} ${lastName}`.trim();
            const reverseFullName = `${lastName} ${firstName}`.trim();
            const displayName = fullName || username;
            
            // Tạo mảng tất cả các trường để tìm kiếm
            const searchFields = [
                firstName,
                lastName,
                username,
                email,
                fullName,
                reverseFullName,
                displayName
            ].filter(field => field.length > 0);
            
            // Tìm kiếm theo từng từ khóa
            return searchTerms.some(term => {
                // Tìm kiếm chính xác trước
                const exactMatch = searchFields.some(field => field === term);
                if (exactMatch) return true;
                
                // Tìm kiếm chứa từ khóa
                const containsMatch = searchFields.some(field => field.includes(term));
                if (containsMatch) return true;
                
                // Tìm kiếm fuzzy (bỏ qua dấu, khoảng trắng)
                const fuzzyMatch = searchFields.some(field => {
                    const normalizedField = field.replace(/[^\w]/g, '');
                    const normalizedTerm = term.replace(/[^\w]/g, '');
                    return normalizedField.includes(normalizedTerm);
                });
                
                return fuzzyMatch;
            });
        });
    }
    
    
    // Hiển thị kết quả tìm kiếm
    renderSearchResults(users) {
        const searchResults = document.getElementById('userSearchResults');
        if (!searchResults) {
            return;
        }
        
        if (!Array.isArray(users) || users.length === 0) {
            searchResults.innerHTML = '<div class="text-center py-3 text-muted">Không tìm thấy người dùng nào</div>';
            return;
        }
        
        const resultsHTML = users.map((user, index) => {
            const fullName = `${user.firstName || ''} ${user.lastName || ''}`.trim() || user.username;
            const avatar = user.avatar || 'https://cdn.pixabay.com/photo/2023/02/18/11/00/icon-7797704_640.png';
            const isSelected = this.selectedUsers.some(selected => selected.id === user.id);
            
            // Highlight search terms với nhiều trường và kết hợp
            const highlightedFields = this.searchQuery ? this.highlightCombinedFields(user, this.searchQuery) : {
                fullName: fullName,
                username: user.username,
                combined: fullName
            };
            const highlightedName = highlightedFields.fullName;
            const highlightedUsername = highlightedFields.username;
            
            // Thêm thông tin bổ sung với highlight
            const userInfo = [];
            if (user.email) {
                const highlightedEmail = this.searchQuery ? this.highlightMultipleFields(user.email, this.searchQuery) : user.email;
                userInfo.push(`📧 ${highlightedEmail}`);
            }
            if (user.isOnline !== undefined) userInfo.push(user.isOnline ? '🟢 Online' : '⚫ Offline');
            
            
            return `
                <div class="user-result-item ${isSelected ? 'selected' : ''}" data-user-id="${user.id}">
                    <div class="user-avatar">
                        <img src="${avatar}" alt="${fullName}" onerror="this.src='https://cdn.pixabay.com/photo/2023/02/18/11/00/icon-7797704_640.png'">
                        ${user.isOnline ? '<div class="online-indicator"></div>' : ''}
                    </div>
                    <div class="user-info">
                        <div class="user-name">${highlightedName}</div>
                        <div class="user-username">@${highlightedUsername}</div>
                        ${userInfo.length > 0 ? `<div class="user-details">${userInfo.join(' • ')}</div>` : ''}
                    </div>
                    <div class="user-actions">
                        <button class="btn btn-outline-primary btn-sm select-user-btn" data-user-id="${user.id}">
                            <i class="fas fa-${isSelected ? 'check' : 'plus'}"></i> ${isSelected ? 'Đã chọn' : 'Chọn'}
                        </button>
                        <button class="btn btn-primary btn-sm start-chat-btn" data-user-id="${user.id}">
                            <i class="fas fa-comment"></i> Nhắn tin
                        </button>
                    </div>
                </div>
            `;
        }).join('');
        
        searchResults.innerHTML = resultsHTML;
        
        // Add event listeners for select user buttons
        searchResults.querySelectorAll('.select-user-btn').forEach(btn => {
            btn.addEventListener('click', (e) => {
                const userId = parseInt(e.target.closest('.select-user-btn').dataset.userId);
                this.toggleUserSelection(userId);
            });
        });
        
        // Add event listeners for start chat buttons
        searchResults.querySelectorAll('.start-chat-btn').forEach(btn => {
            btn.addEventListener('click', (e) => {
                const userId = e.target.closest('.start-chat-btn').dataset.userId;
                this.startChatWithUser(parseInt(userId));
            });
        });
    }
    
    // Xóa kết quả tìm kiếm
    clearSearchResults() {
        const searchResults = document.getElementById('searchResults');
        if (searchResults) {
            searchResults.innerHTML = '';
        }
    }
    
    // Xóa danh sách người được chọn
    clearSelectedUsers() {
        this.selectedUsers = [];
        this.updateSelectedUsersDisplay();
        
        // Clear input tên nhóm
        const groupNameInput = document.getElementById('groupNameInput');
        if (groupNameInput) {
            groupNameInput.value = '';
        }
    }
    
    // Toggle chọn/bỏ chọn người dùng
    toggleUserSelection(userId) {
        const userIndex = this.selectedUsers.findIndex(user => user.id === userId);
        
        if (userIndex > -1) {
            // Bỏ chọn
            this.selectedUsers.splice(userIndex, 1);
        } else {
            // Chọn thêm
            // Tìm user từ kết quả tìm kiếm hiện tại
            const searchResults = document.getElementById('userSearchResults');
            const userElement = searchResults.querySelector(`[data-user-id="${userId}"]`);
            if (userElement) {
                const userName = userElement.querySelector('.user-name').textContent;
                const userUsername = userElement.querySelector('.user-username').textContent;
                const userAvatar = userElement.querySelector('.user-avatar img').src;
                
                this.selectedUsers.push({
                    id: userId,
                    name: userName,
                    username: userUsername,
                    avatar: userAvatar
                });
            }
        }
        
        this.updateSelectedUsersDisplay();
        this.renderSearchResults(this.lastSearchResults || []); // Re-render để cập nhật UI
    }
    
    // Cập nhật hiển thị danh sách người được chọn
    updateSelectedUsersDisplay() {
        const selectedUsersContainer = document.getElementById('selectedUsersContainer');
        const groupNameInputContainer = document.getElementById('groupNameInputContainer');
        const groupNameInput = document.getElementById('groupNameInput');
        
        if (!selectedUsersContainer) return;
        
        if (this.selectedUsers.length === 0) {
            selectedUsersContainer.innerHTML = '';
            if (groupNameInputContainer) {
                groupNameInputContainer.style.display = 'none';
            }
            return;
        }
        
        // Hiển thị input tên nhóm khi có người được chọn
        if (groupNameInputContainer) {
            groupNameInputContainer.style.display = 'block';
        }
        
        // Set default tên nhóm
        if (groupNameInput && !groupNameInput.value) {
            groupNameInput.value = `Nhóm ${this.selectedUsers.length + 1} người`;
        }
        
        const selectedHTML = `
            <div class="selected-users-header">
                <span>Đã chọn ${this.selectedUsers.length} người:</span>
            </div>
            <div class="selected-users-list">
                ${this.selectedUsers.map(user => `
                    <div class="selected-user-item">
                        <img src="${user.avatar}" alt="${user.name}" class="selected-user-avatar">
                        <span class="selected-user-name">${user.name}</span>
                        <button class="btn btn-sm btn-outline-danger remove-user-btn" data-user-id="${user.id}">
                            <i class="fas fa-times"></i>
                        </button>
                    </div>
                `).join('')}
            </div>
            <div class="selected-users-actions">
                <button class="btn btn-success" id="createGroupChatBtn">
                    <i class="fas fa-users"></i> Tạo nhóm chat
                </button>
            </div>
        `;
        
        selectedUsersContainer.innerHTML = selectedHTML;
        
        // Add event listeners for remove buttons
        selectedUsersContainer.querySelectorAll('.remove-user-btn').forEach(btn => {
            btn.addEventListener('click', (e) => {
                const userId = parseInt(e.target.closest('.remove-user-btn').dataset.userId);
                this.toggleUserSelection(userId);
            });
        });
        
        // Add event listener for create group button
        const createGroupBtn = document.getElementById('createGroupChatBtn');
        if (createGroupBtn) {
            createGroupBtn.addEventListener('click', () => {
                this.createGroupChat();
            });
        }
    }
    
    // Tạo nhóm chat
    async createGroupChat() {
        if (this.selectedUsers.length < 1) {
            alert('Vui lòng chọn ít nhất 1 người để tạo nhóm chat');
            return;
        }
        
        // Lấy tên nhóm từ input
        const groupNameInput = document.getElementById('groupNameInput');
        const groupName = groupNameInput ? groupNameInput.value.trim() : '';
        
        if (!groupName) {
            alert('Vui lòng nhập tên nhóm chat');
            if (groupNameInput) {
                groupNameInput.focus();
            }
            return;
        }
        
        try {
            const userIds = this.selectedUsers.map(user => user.id);
            
            const response = await authenticatedFetch('/api/chat/group-chat', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                credentials: 'include',
                body: JSON.stringify({
                    groupName: groupName.trim(),
                    userIds: userIds
                })
            });
            
            if (response.ok) {
                const data = await response.json();
                if (data.success) {
                    // Đóng modal
                    this.hideNewChatModal();
                    
                    // Load lại danh sách chat rooms
                    await this.loadChatRooms();
                    
                    // Mở chat room mới
                    this.selectRoom(data.data.id);
                } else {
                    alert('Không thể tạo nhóm chat: ' + data.message);
                }
            } else {
                alert('Không thể tạo nhóm chat');
            }
        } catch (error) {
            alert('Lỗi khi tạo nhóm chat');
        }
    }
    
    
    // Bắt đầu chat với user
    async startChatWithUser(userId) {
        try {
            const response = await authenticatedFetch('/api/chat/private-chat', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                credentials: 'include',
                body: JSON.stringify({ userId: userId })
            });
            
            if (response.ok) {
                const data = await response.json();
                
                if (data.success) {
                    // Đóng modal
                    this.hideNewChatModal();
                    
                    // Load lại danh sách chat rooms
                    await this.loadChatRooms();
                    
                    // Mở chat room mới
                    this.selectRoom(data.data.id);
                } else {
                    alert('Không thể bắt đầu cuộc trò chuyện: ' + data.message);
                }
            } else {
                const errorText = await response.text();
                alert('Không thể bắt đầu cuộc trò chuyện (Status: ' + response.status + ')');
            }
        } catch (error) {
            alert('Lỗi khi bắt đầu cuộc trò chuyện: ' + error.message);
        }
    }

    // Cập nhật online status của user
    updateUserOnlineStatus(userId, isOnline) {
        
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
        }
    }
    
    // Dừng polling
    stopPolling() {
        if (this.pollingInterval) {
            clearInterval(this.pollingInterval);
            this.pollingInterval = null;
        }
    }
    
    
    // Gửi message read status qua WebSocket
    sendMessageReadStatus() {
        if (window.chatWebSocketManager && window.chatWebSocketManager.isConnected() && this.currentRoomId) {
            window.chatWebSocketManager.sendMessageRead(this.currentRoomId);
        }
    }
    
    // Thêm vào lịch sử tìm kiếm gần đây
    addToRecentSearches(query) {
        if (!query || query.length < 2) return;
        
        // Loại bỏ query cũ nếu đã tồn tại
        this.recentSearches = this.recentSearches.filter(search => search !== query);
        
        // Thêm query mới vào đầu danh sách
        this.recentSearches.unshift(query);
        
        // Giới hạn tối đa 10 lịch sử
        if (this.recentSearches.length > 10) {
            this.recentSearches = this.recentSearches.slice(0, 10);
        }
        
        // Lưu vào localStorage
        localStorage.setItem('chatRecentSearches', JSON.stringify(this.recentSearches));
    }
    
    // Hiển thị lịch sử tìm kiếm gần đây
    showRecentSearches() {
        const searchResults = document.getElementById('searchResults');
        if (!searchResults) return;
        
        // Load từ localStorage
        const savedSearches = localStorage.getItem('chatRecentSearches');
        if (savedSearches) {
            this.recentSearches = JSON.parse(savedSearches);
        }
        
        if (this.recentSearches.length === 0) {
            searchResults.innerHTML = '<div class="text-center py-3 text-muted">Gõ tên để tìm kiếm người dùng</div>';
            return;
        }
        
        const recentHTML = `
            <div class="recent-searches">
                <div class="recent-searches-header">
                    <i class="fas fa-clock"></i> Tìm kiếm gần đây
                </div>
                ${this.recentSearches.map(search => `
                    <div class="recent-search-item" onclick="window.chatManager.searchUserFromRecent('${search}')">
                        <i class="fas fa-search"></i>
                        <span>${search}</span>
                        <button class="btn-remove-recent" onclick="event.stopPropagation(); window.chatManager.removeRecentSearch('${search}')">
                            <i class="fas fa-times"></i>
                        </button>
                    </div>
                `).join('')}
            </div>
        `;
        
        searchResults.innerHTML = recentHTML;
    }
    
    // Tìm kiếm từ lịch sử
    searchUserFromRecent(query) {
        const userSearchInput = document.getElementById('userSearchInput');
        if (userSearchInput) {
            userSearchInput.value = query;
            this.searchQuery = query;
            this.searchUsers(query);
        }
    }
    
    // Xóa lịch sử tìm kiếm
    removeRecentSearch(query) {
        this.recentSearches = this.recentSearches.filter(search => search !== query);
        localStorage.setItem('chatRecentSearches', JSON.stringify(this.recentSearches));
        this.showRecentSearches();
    }
    
    // Hiển thị gợi ý tìm kiếm
    showSearchSuggestions(query) {
        const searchResults = document.getElementById('searchResults');
        if (!searchResults) return;
        
        // Tạo gợi ý từ lịch sử tìm kiếm
        const suggestions = this.recentSearches.filter(search => 
            search.toLowerCase().includes(query.toLowerCase())
        ).slice(0, 5);
        
        if (suggestions.length === 0) {
            searchResults.innerHTML = '<div class="text-center py-3 text-muted">Gõ thêm để tìm kiếm...</div>';
            return;
        }
        
        const suggestionsHTML = `
            <div class="search-suggestions">
                <div class="suggestions-header">
                    <i class="fas fa-lightbulb"></i> Gợi ý
                </div>
                ${suggestions.map(suggestion => `
                    <div class="suggestion-item" onclick="window.chatManager.searchUserFromRecent('${suggestion}')">
                        <i class="fas fa-search"></i>
                        <span>${suggestion}</span>
                    </div>
                `).join('')}
            </div>
        `;
        
        searchResults.innerHTML = suggestionsHTML;
    }
}

// Initialize when page loads
document.addEventListener('DOMContentLoaded', function() {
    window.chatManager = new SimpleChatManager();
    window.simpleChatManager = window.chatManager; // Alias for compatibility
});
