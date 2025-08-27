/**
 * WebSocket Manager - Quản lý kết nối WebSocket cho toàn bộ ứng dụng
 */
class WebSocketManager {
    constructor() {
        this.websocket = null;
        this.reconnectAttempts = 0;
        this.maxReconnectAttempts = 2; // Giảm số lần retry
        this.reconnectDelay = 10000; // Tăng delay lên 10 giây
        this.isConnecting = false;
        this.messageHandlers = new Map();
        this.connectionHandlers = new Map();
        this.currentUserId = null;
        this.retryTimeout = null;
        this.isDestroyed = false;
        this.hasConnectedOnce = false; // Flag để kiểm tra đã kết nối thành công chưa
        
        // Bind methods
        this.connect = this.connect.bind(this);
        this.disconnect = this.disconnect.bind(this);
        this.send = this.send.bind(this);
        this.onMessage = this.onMessage.bind(this);
        this.onConnectionChange = this.onConnectionChange.bind(this);
    }

    /**
     * Khởi tạo WebSocket connection
     */
    async connect() {
        if (this.isDestroyed || this.isConnecting || (this.websocket && this.websocket.readyState === WebSocket.OPEN)) {
            return;
        }

        this.isConnecting = true;
        
        try {
            // Lấy userId từ API /me
            this.currentUserId = await this.getCurrentUserId();
            if (!this.currentUserId) {
                this.isConnecting = false;
                return;
            }

            const wsUrl = `ws://${window.location.host}/ws/notifications?recipientId=${this.currentUserId}`;
            
            this.websocket = new WebSocket(wsUrl);
            
            this.websocket.onopen = (event) => {
                this.isConnecting = false;
                this.reconnectAttempts = 0;
                this.hasConnectedOnce = true;
                this.onConnectionChange(true);
                
                // Gửi message xác nhận kết nối
                this.send({
                    type: 'CONNECT',
                    userId: this.currentUserId
                });
            };
            
            this.websocket.onmessage = (event) => {
                this.onMessage(event);
            };
            
            this.websocket.onclose = (event) => {
                this.isConnecting = false;
                this.onConnectionChange(false);
                
                // Chỉ retry nếu chưa từng kết nối thành công hoặc không phải do user đóng
                if (!this.isDestroyed && event.code !== 1000 && !this.hasConnectedOnce && this.reconnectAttempts < this.maxReconnectAttempts) {
                    this.scheduleReconnect();
                } else if (this.reconnectAttempts >= this.maxReconnectAttempts) {
                    this.onConnectionChange(false, 'max_retries');
                }
            };
            
            this.websocket.onerror = (error) => {
                this.isConnecting = false;
                this.onConnectionChange(false, 'error');
            };
            
        } catch (error) {
            this.isConnecting = false;
            this.onConnectionChange(false, 'init_error');
        }
    }

    /**
     * Lên lịch retry với delay
     */
    scheduleReconnect() {
        if (this.retryTimeout) {
            clearTimeout(this.retryTimeout);
        }
        
        this.reconnectAttempts++;
        const delay = this.reconnectDelay * this.reconnectAttempts;
        
        this.retryTimeout = setTimeout(() => {
            if (!this.isDestroyed) {
                this.connect();
            }
        }, delay);
    }

    /**
     * Ngắt kết nối WebSocket
     */
    disconnect() {
        if (this.retryTimeout) {
            clearTimeout(this.retryTimeout);
            this.retryTimeout = null;
        }
        
        if (this.websocket) {
            this.websocket.close(1000, 'User disconnect');
            this.websocket = null;
        }
        this.isConnecting = false;
        this.onConnectionChange(false);
    }

    /**
     * Gửi message qua WebSocket
     */
    send(data) {
        if (this.websocket && this.websocket.readyState === WebSocket.OPEN) {
            try {
                const message = JSON.stringify(data);
                this.websocket.send(message);
            } catch (error) {
                // Silent error
            }
        }
    }

    /**
     * Xử lý message nhận được từ WebSocket
     */
    onMessage(event) {
        try {
            const data = JSON.parse(event.data);
            
            // Gọi tất cả message handlers
            this.messageHandlers.forEach((handler, key) => {
                try {
                    handler(data);
                } catch (error) {
                    // Silent error
                }
            });
            
        } catch (error) {
            // Silent error
        }
    }

    /**
     * Xử lý thay đổi trạng thái kết nối
     */
    onConnectionChange(isConnected, reason = null) {
        // Gọi tất cả connection handlers
        this.connectionHandlers.forEach((handler, key) => {
            try {
                handler(isConnected, reason);
            } catch (error) {
                // Silent error
            }
        });
    }

    /**
     * Đăng ký message handler
     */
    addMessageHandler(key, handler) {
        this.messageHandlers.set(key, handler);
    }

    /**
     * Hủy đăng ký message handler
     */
    removeMessageHandler(key) {
        this.messageHandlers.delete(key);
    }

    /**
     * Đăng ký connection handler
     */
    addConnectionHandler(key, handler) {
        this.connectionHandlers.set(key, handler);
    }

    /**
     * Hủy đăng ký connection handler
     */
    removeConnectionHandler(key) {
        this.connectionHandlers.delete(key);
    }

    /**
     * Lấy trạng thái kết nối
     */
    isConnected() {
        return this.websocket && this.websocket.readyState === WebSocket.OPEN;
    }

    /**
     * Lấy userId hiện tại từ API /me
     */
    async getCurrentUserId() {
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
            return null;
        }
    }

    /**
     * Kiểm tra và kết nối lại nếu cần
     */
    ensureConnection() {
        if (!this.isDestroyed && !this.isConnected() && !this.isConnecting) {
            this.connect();
        }
    }

    /**
     * Cleanup khi trang đóng
     */
    cleanup() {
        this.isDestroyed = true;
        this.disconnect();
        this.messageHandlers.clear();
        this.connectionHandlers.clear();
    }
}

// Tạo instance global
const wsManager = new WebSocketManager();

// Auto-connect khi trang load
document.addEventListener('DOMContentLoaded', () => {
    // Chỉ kết nối một lần khi trang load
    wsManager.connect();
});

// Cleanup khi trang đóng
window.addEventListener('beforeunload', () => {
    wsManager.cleanup();
});

// Export để sử dụng ở các file khác
window.wsManager = wsManager;
