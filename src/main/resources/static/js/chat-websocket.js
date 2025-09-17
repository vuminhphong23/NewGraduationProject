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
                fallbackToPolling();
                return;
            }
            
            const wsUrl = `${protocol}//${window.location.host}/ws/chat?userId=${userId}`;
            
            chatWebSocket = new WebSocket(wsUrl);
            
            chatWebSocket.onopen = function(event) {
                isConnected = true;
                reconnectAttempts = 0;
            };
            
            chatWebSocket.onmessage = function(event) {
                try {
                    const data = JSON.parse(event.data);
                    handleChatWebSocketMessage(data);
                } catch (error) {
                    // Error parsing message
                }
            };
            
            chatWebSocket.onclose = function(event) {
                isConnected = false;
                chatWebSocket = null;
                
                // Tự động reconnect nếu chưa đạt max attempts
                if (reconnectAttempts < maxReconnectAttempts) {
                    reconnectAttempts++;
                    const delay = Math.min(1000 * Math.pow(2, reconnectAttempts), 30000); // Exponential backoff
                    
                    reconnectTimeout = setTimeout(async () => {
                        await initializeChatWebSocket();
                    }, delay);
                } else {
                    fallbackToPolling();
                }
            };
            
            chatWebSocket.onerror = function(error) {
                // WebSocket error
            };
            
        } catch (error) {
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
                return false;
            }
        } else {
            return false;
        }
    }
    
    // Xử lý message từ WebSocket
    function handleChatWebSocketMessage(data) {
            
            switch (data.type) {
            case 'CONNECTION_ESTABLISHED':
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
                
            case 'FILE_UPLOADED':
                handleFileUploaded(data);
                break;
                
            case 'FILES_UPLOADED':
                handleFilesUploaded(data);
                break;
                
            default:
                // Unknown message type
                break;
        }
    }
    
    // Xử lý tin nhắn mới
    function handleNewMessage(messageData) {
        // Nếu đang ở room này, thêm message vào UI
        if (window.chatManager && window.chatManager.currentRoomId == messageData.roomId) {
            window.chatManager.addMessageToUI(messageData);
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
        // Cập nhật UI để hiển thị "đã xem" cho tin nhắn cuối cùng
        if (window.chatManager && window.chatManager.currentRoomId === data.roomId) {
            window.chatManager.updateMessageReadStatus(data.roomId);
        }
    }
    
    
    // Xử lý user online
    function handleUserOnline(data) {
        if (window.chatManager) {
            window.chatManager.updateUserOnlineStatus(data.userId, true);
        }
    }
    
    // Xử lý user offline
    function handleUserOffline(data) {
        if (window.chatManager) {
            window.chatManager.updateUserOnlineStatus(data.userId, false);
        }
    }
    
    // Xử lý file upload
    function handleFileUploaded(data) {
        // Nếu đang ở room này, hiển thị file trong chat
        if (window.chatManager && window.chatManager.currentRoomId == data.roomId) {
            // Tạo message giả để hiển thị file
            const fileMessage = {
                id: 'temp_' + Date.now(),
                content: `📎 ${data.file.originalName}`, // Content text cho sidebar
                senderId: data.userId,
                senderName: 'Bạn',
                createdAt: new Date().toISOString(),
                isRead: true,
                attachments: [data.file] // Sử dụng attachments thay vì file
            };
            
            window.chatManager.addMessageToUI(fileMessage);
        }
        
        // Cập nhật files sidebar nếu đang mở
        if (window.chatFilesManager && window.chatFilesManager.currentRoomId == data.roomId) {
            window.chatFilesManager.addFile(data.file);
        }
        
        // Hiển thị notification nếu không phải file của mình
        const currentUserId = getCurrentUserIdSync();
        if (currentUserId && data.userId !== currentUserId) {
            showFileNotification(data.file, data.userId);
        }
    }
    
    // Xử lý multiple files upload
    function handleFilesUploaded(data) {
        // Nếu đang ở room này, hiển thị files trong chat
        if (window.chatManager && window.chatManager.currentRoomId == data.roomId) {
            // Tạo message giả để hiển thị files
            const filesMessage = {
                id: 'temp_' + Date.now(),
                content: `📎 Đã gửi ${data.files.length} file`, // Content text cho sidebar
                senderId: data.userId,
                senderName: 'Bạn',
                createdAt: new Date().toISOString(),
                isRead: true,
                attachments: data.files // Sử dụng attachments thay vì files
            };
            
            window.chatManager.addMessageToUI(filesMessage);
        }
        
        // Cập nhật files sidebar nếu đang mở
        if (window.chatFilesManager && window.chatFilesManager.currentRoomId == data.roomId) {
            data.files.forEach(file => {
                window.chatFilesManager.addFile(file);
            });
        }
        
        // Hiển thị notification nếu không phải files của mình
        const currentUserId = getCurrentUserIdSync();
        if (currentUserId && data.userId !== currentUserId) {
            showFilesNotification(data.files, data.userId);
        }
    }
    
    // Hiển thị notification cho tin nhắn mới
    function showChatNotification(messageData) {
        if (window.toastManager) {
            const notificationText = `${messageData.senderName}: ${messageData.content}`;
            window.toastManager.info(notificationText);
        }
    }
    
    // Hiển thị notification cho file upload
    function showFileNotification(file, userId) {
        if (window.toastManager) {
            const notificationText = `📎 ${file.originalName}`;
            window.toastManager.info(notificationText);
        }
    }
    
    // Hiển thị notification cho multiple files upload
    function showFilesNotification(files, userId) {
        if (window.toastManager) {
            const notificationText = `📎 Đã gửi ${files.length} file`;
            window.toastManager.info(notificationText);
        }
    }
    
    // Fallback về polling nếu WebSocket không hoạt động
    function fallbackToPolling() {
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
