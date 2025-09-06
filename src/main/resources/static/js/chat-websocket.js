/**
 * Chat WebSocket Manager - Tương tự như notifications.js
 */
(() => {
    let chatWebSocket = null;
    let isConnected = false;
    let reconnectAttempts = 0;
    const maxReconnectAttempts = 5;
    let reconnectTimeout = null;
    
    // Khởi tạo WebSocket connection
    async function initializeChatWebSocket() {
        if (chatWebSocket && chatWebSocket.readyState === WebSocket.OPEN) {
            console.log('Chat WebSocket already connected');
            return;
        }
        
        try {
            const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
            
            // Kiểm tra cache trước
            let userId = null;
            if (window.currentUser && window.currentUser.id) {
                userId = window.currentUser.id;
            } else {
                userId = await getCurrentUserId();
            }
            
            if (!userId) {
                console.error('❌ Cannot get current user ID for WebSocket connection');
                fallbackToPolling();
                return;
            }
            
            const wsUrl = `${protocol}//${window.location.host}/ws/chat?userId=${userId}`;
            
            console.log('🔗 Connecting to Chat WebSocket:', wsUrl);
            
            chatWebSocket = new WebSocket(wsUrl);
            
            chatWebSocket.onopen = function(event) {
                console.log('✅ Chat WebSocket connected');
                isConnected = true;
                reconnectAttempts = 0;
            };
            
            chatWebSocket.onmessage = function(event) {
                try {
                    const data = JSON.parse(event.data);
                    handleChatWebSocketMessage(data);
                } catch (error) {
                    console.error('❌ Error parsing chat WebSocket message:', error);
                }
            };
            
            chatWebSocket.onclose = function(event) {
                console.log('🔌 Chat WebSocket disconnected:', event.code, event.reason);
                isConnected = false;
                chatWebSocket = null;
                
                // Tự động reconnect nếu chưa đạt max attempts
                if (reconnectAttempts < maxReconnectAttempts) {
                    reconnectAttempts++;
                    const delay = Math.min(1000 * Math.pow(2, reconnectAttempts), 30000); // Exponential backoff
                    console.log(`🔄 Reconnecting chat WebSocket in ${delay}ms (attempt ${reconnectAttempts}/${maxReconnectAttempts})`);
                    
                    reconnectTimeout = setTimeout(async () => {
                        await initializeChatWebSocket();
                    }, delay);
                } else {
                    console.log('❌ Max reconnection attempts reached for chat WebSocket');
                    fallbackToPolling();
                }
            };
            
            chatWebSocket.onerror = function(error) {
                console.error('❌ Chat WebSocket error:', error);
            };
            
        } catch (error) {
            console.error('❌ Error initializing chat WebSocket:', error);
            fallbackToPolling();
        }
    }
    
    // Gửi message qua WebSocket
    function sendChatMessage(message) {
        if (chatWebSocket && chatWebSocket.readyState === WebSocket.OPEN) {
            try {
                chatWebSocket.send(JSON.stringify(message));
                return true;
            } catch (error) {
                console.error('❌ Error sending chat WebSocket message:', error);
                return false;
            }
        } else {
            console.warn('⚠️ Chat WebSocket not connected, cannot send message');
            return false;
        }
    }
    
    // Xử lý message từ WebSocket
    function handleChatWebSocketMessage(data) {
        console.log('📨 Chat WebSocket message received:', data);
            
            switch (data.type) {
            case 'CONNECTION_ESTABLISHED':
                console.log('✅ Chat WebSocket connection confirmed');
                    break;
                
            case 'NEW_MESSAGE':
                handleNewMessage(data.message);
                    break;
                
            case 'MESSAGE_READ':
                handleMessageRead(data);
                    break;
                
                
            case 'USER_ONLINE':
                handleUserOnline(data);
                break;
                
            case 'USER_OFFLINE':
                handleUserOffline(data);
                break;
                
            default:
                console.log('❓ Unknown chat WebSocket message type:', data.type);
        }
    }
    
    // Xử lý tin nhắn mới
    function handleNewMessage(messageData) {
        console.log('💬 New message received:', messageData);
        console.log('💬 Current room ID:', window.chatManager ? window.chatManager.currentRoomId : 'No chatManager');
        console.log('💬 Message room ID:', messageData.roomId);
        
        // Nếu đang ở room này, thêm message vào UI
        if (window.chatManager && window.chatManager.currentRoomId == messageData.roomId) {
            console.log('💬 Adding message to UI');
            window.chatManager.addMessageToUI(messageData);
        } else {
            console.log('💬 Not in this room, skipping UI update');
            console.log('💬 Current room ID type:', typeof window.chatManager.currentRoomId);
            console.log('💬 Message room ID type:', typeof messageData.roomId);
        }
        
        // Cập nhật danh sách rooms để hiển thị tin nhắn mới nhất
        if (window.chatManager) {
            window.chatManager.updateRoomLastMessage(messageData.roomId, messageData);
        }
        
        // Hiển thị notification nếu không phải tin nhắn của mình
        const currentUserId = getCurrentUserIdSync();
        if (currentUserId && messageData.senderId !== currentUserId) {
            showChatNotification(messageData);
        }
    }
    
    // Xử lý message read status
    function handleMessageRead(data) {
        console.log('👁️ Message read status updated:', data);
        
        // Cập nhật UI để hiển thị "đã xem" cho tin nhắn cuối cùng
        if (window.chatManager && window.chatManager.currentRoomId === data.roomId) {
            window.chatManager.updateMessageReadStatus(data.roomId);
        }
    }
    
    
    // Xử lý user online
    function handleUserOnline(data) {
        console.log('🟢 User online:', data);
        
        if (window.chatManager) {
            window.chatManager.updateUserOnlineStatus(data.userId, true);
        }
    }
    
    // Xử lý user offline
    function handleUserOffline(data) {
        console.log('🔴 User offline:', data);
        
        if (window.chatManager) {
            window.chatManager.updateUserOnlineStatus(data.userId, false);
        }
    }
    
    // Hiển thị notification cho tin nhắn mới
    function showChatNotification(messageData) {
        if (window.toastManager) {
            const notificationText = `${messageData.senderName}: ${messageData.content}`;
            window.toastManager.info(notificationText);
        }
    }
    
    // Fallback về polling nếu WebSocket không hoạt động
    function fallbackToPolling() {
        console.log('🔄 Falling back to polling for chat');
        
        // Polling sẽ được thực hiện bởi chat-simple.js
        if (window.chatManager) {
            window.chatManager.startPolling();
        }
    }
    
    // Lấy current user ID (giống như websocket-manager.js)
    async function getCurrentUserId() {
        try {
            // Kiểm tra cache trước
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
    
    // Lấy current user ID từ cache (synchronous)
    function getCurrentUserIdSync() {
        return window.currentUser ? window.currentUser.id : null;
    }
    
    // Public API
    window.chatWebSocketManager = {
        // Kết nối WebSocket
        connect: initializeChatWebSocket,
        
        // Ngắt kết nối
        disconnect: function() {
            if (chatWebSocket) {
                chatWebSocket.close();
                chatWebSocket = null;
            }
            if (reconnectTimeout) {
                clearTimeout(reconnectTimeout);
                reconnectTimeout = null;
            }
        },
        
        // Gửi message
        send: sendChatMessage,
        
        // Kiểm tra trạng thái kết nối
        isConnected: function() {
            return isConnected && chatWebSocket && chatWebSocket.readyState === WebSocket.OPEN;
        },
        
        // Join room
        joinRoom: function(roomId) {
            sendChatMessage({
                type: 'JOIN_ROOM',
                roomId: roomId
            });
        },
        
        // Leave room
        leaveRoom: function(roomId) {
            sendChatMessage({
                type: 'LEAVE_ROOM',
                roomId: roomId
            });
        },
        
        
        // Gửi message read status
        sendMessageRead: function(roomId) {
            sendChatMessage({
                type: 'MESSAGE_READ',
                roomId: roomId
            });
        },
        
        // Lấy current user ID
        getCurrentUserId: getCurrentUserIdSync
    };
    
    // Tự động khởi tạo khi DOM ready
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', async () => {
            await initializeChatWebSocket();
        });
    } else {
        initializeChatWebSocket();
    }
    
})();
