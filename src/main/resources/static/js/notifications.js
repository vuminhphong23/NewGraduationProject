(() => {
    const notificationBadge = document.getElementById('notificationBadge');
    const notificationList = document.getElementById('notificationList');
    const markAllReadBtn = document.getElementById('markAllRead');
    
    if (!notificationBadge || !notificationList || !markAllReadBtn) {
        return;
    }

    let notifications = [];
    let unreadCount = 0;
    const pageLoadTime = new Date(); // Thời điểm load trang

    // Khởi tạo WebSocket handlers
    function initializeWebSocketHandlers() {
        if (!window.wsManager) {
            // Thêm giới hạn retry để tránh vòng lặp vô hạn
            if (!window.wsManagerRetryCount) {
                window.wsManagerRetryCount = 0;
            }
            
            if (window.wsManagerRetryCount < 10) { // Giới hạn 10 lần retry
                window.wsManagerRetryCount++;
                setTimeout(initializeWebSocketHandlers, 1000); // Tăng timeout lên 1 giây
            } else {
                fallbackToPolling();
            }
            return;
        }

        // Reset retry count khi thành công
        window.wsManagerRetryCount = 0;
        
        // Đăng ký message handler cho notifications
        wsManager.addMessageHandler('notifications', handleWebSocketMessage);
        
        // Đăng ký connection handler
        wsManager.addConnectionHandler('notifications', handleConnectionChange);
    }

    // Xử lý message từ WebSocket
    function handleWebSocketMessage(data) {
        switch (data.type) {
            case 'NOTIFICATION':
                // Thông báo mới từ WebSocket (chỉ hiển thị toast cho loại này)
                const notification = data.notification;
                handleNewNotification(notification, true); // true = từ WebSocket
                break;
                
            case 'NOTIFICATION_UPDATE':
                // Cập nhật thông báo
                const updatedNotification = data.notification;
                updateNotificationInList(updatedNotification);
                break;
                
            case 'NOTIFICATION_DELETE':
                // Xóa thông báo
                const notificationId = data.notificationId;
                removeNotificationFromList(notificationId);
                break;
                
            case 'UNREAD_COUNT_UPDATE':
                // Cập nhật số lượng chưa đọc
                unreadCount = data.count || 0;
                updateBadge();
                break;
                
            case 'FRIENDSHIP_ACCEPTED':
                // Kết bạn được chấp nhận
                handleFriendshipAccepted(data);
                break;
                
            case 'FRIENDSHIP_REJECTED':
                // Kết bạn bị từ chối
                handleFriendshipRejected(data);
                break;
                
            case 'FRIENDSHIP_CANCELLED':
                // Hủy kết bạn
                handleFriendshipCancelled(data);
                break;
                
            case 'FRIENDSHIP_REQUEST_SENT':
                // Đã gửi lời mời kết bạn
                handleFriendshipRequestSent(data);
                break;
                
            case 'FRIENDSHIP_STATUS_UPDATE':
                // Cập nhật trạng thái friendship từ server
                handleFriendshipStatusUpdate(data);
                break;
                
            default:
                // Loại message không xác định
                console.log('Unknown message type:', data.type);
        }
    }

    // Xử lý thay đổi kết nối
    function handleConnectionChange(isConnected, reason) {
        if (isConnected) {
            // WebSocket đã kết nối, notifications sẽ nhận realtime updates
            console.log('WebSocket connected for notifications');
        } else {
            // WebSocket đã ngắt kết nối, chuyển về polling mode
            console.log('WebSocket disconnected for notifications:', reason);
            if (reason === 'max_retries') {
                fallbackToPolling();
            }
        }
    }

    // Fallback về polling nếu WebSocket không hoạt động
    function fallbackToPolling() {
        console.log('Falling back to polling for notifications');
        setInterval(() => {
            loadUnreadCount();
        }, 30000);
    }

    // Load notifications ban đầu
    async function loadNotifications() {
        try {
            const response = await authenticatedFetch('/api/notifications');
            if (!response || !response.ok) throw new Error('Failed to load notifications');
            
            notifications = await response.json();
            renderNotifications();
            
            // Đánh dấu rằng đã load xong thông báo ban đầu
            window.notificationsLoaded = true;
        } catch (error) {
            console.error('Error loading notifications:', error);
        }
    }

    // Load unread count ban đầu
    async function loadUnreadCount() {
        try {
            const response = await authenticatedFetch('/api/notifications/unread-count');
            if (!response || !response.ok) throw new Error('Failed to load unread count');
            
            const data = await response.json();
            unreadCount = data.count || 0;
            updateBadge();
        } catch (error) {
            console.error('Error loading unread count:', error);
        }
    }

    // Cập nhật badge
    function updateBadge() {
        if (unreadCount > 0) {
            notificationBadge.style.display = 'block';
            notificationBadge.textContent = unreadCount > 99 ? '99+' : unreadCount.toString();
        } else {
            notificationBadge.style.display = 'none';
        }
    }

    // Render notifications
    function renderNotifications() {
        const emptyState = `
            <div class="text-center py-3 text-muted">
                <div>Không có thông báo nào</div>
            </div>
        `;

        if (!notifications.length) {
            notificationList.innerHTML = emptyState;
            return;
        }

        notificationList.innerHTML = notifications.map(notification => `
            <div class="notification-item ${notification.isRead ? '' : 'unread'}" 
                 data-notification-id="${notification.id}">
                <div class="notification-content">
                    <img src="${notification.senderAvatar || 'https://cdn.pixabay.com/photo/2023/02/18/11/00/icon-7797704_640.png'}"
                         class="notification-avatar" alt="avatar"
                         onerror="this.src='https://cdn.pixabay.com/photo/2023/02/18/11/00/icon-7797704_640.png'">
                    <div class="notification-text">
                        <div class="notification-message">${escapeHtml(notification.message)}</div>
                        <div class="notification-time">${formatTime(notification.createdAt)}</div>
                        ${renderNotificationActions(notification)}
                    </div>
                </div>
            </div>
        `).join('');

        wireNotificationEvents();
    }

    // Render các action cho từng loại notification
    function renderNotificationActions(notification) {
        const type = notification.notificationType || notification.type;
        
        switch (type) {
            case 'FRIENDSHIP_REQUEST':
                return `
                    <div class="notification-actions mb-2">
                        <button class="btn btn-sm btn-success mr-2" onclick="handleAcceptFriend('${notification.senderId}', '${notification.id}')">
                            <i class="fa fa-check"></i> Chấp nhận
                        </button>
                        <button class="btn btn-sm btn-danger" onclick="handleRejectFriend('${notification.senderId}', '${notification.id}')">
                            <i class="fa fa-times"></i> Từ chối
                        </button>
                    </div>
                `;
                
            case 'FRIENDSHIP_ACCEPTED':
                return `
                    <div class="notification-actions mb-2">
                        <span class="badge badge-success">Đã kết bạn</span>
                        <button class="btn btn-sm btn-danger ml-2" onclick="handleUnfriend('${notification.senderId}')">
                            <i class="fa fa-user-minus"></i> Hủy kết bạn
                        </button>
                    </div>
                `;
                
            case 'FRIENDSHIP_REJECTED':
                return `
                    <div class="notification-actions mb-2">
                        <span class="badge badge-warning">Đã từ chối</span>
                    </div>
                `;
                
            case 'FRIENDSHIP_CANCELLED':
                return `
                    <div class="notification-actions mb-2">
                        <span class="badge badge-secondary">Đã hủy kết bạn</span>
                    </div>
                `;
                
            case 'POST_LIKE':
            case 'POST_COMMENT':
            case 'COMMENT_LIKE':
            case 'COMMENT_REPLY':
            case 'MENTION':
                return `
                    <div class="notification-actions">
                        <button class="btn btn-sm btn-primary view-detail-btn" 
                                data-notification-id="${notification.id}"
                                data-link="${notification.link}">
                            Xem chi tiết
                        </button>
                        ${!notification.isRead ? `
                            <button class="btn btn-sm btn-outline-secondary mark-read-btn" 
                                    data-notification-id="${notification.id}">
                                Đánh dấu đã đọc
                            </button>
                        ` : ''}
                    </div>
                `;
                
            default:
                // Các loại notification khác
                if (notification.link) {
                    return `
                        <div class="notification-actions">
                            <button class="btn btn-sm btn-primary view-detail-btn" 
                                    data-notification-id="${notification.id}"
                                    data-link="${notification.link}">
                                Xem chi tiết
                            </button>
                            ${!notification.isRead ? `
                                <button class="btn btn-sm btn-outline-secondary mark-read-btn" 
                                        data-notification-id="${notification.id}">
                                    Đánh dấu đã đọc
                                </button>
                            ` : ''}
                        </div>
                    `;
                }
                return '';
        }
    }

    // Wire up notification events
    function wireNotificationEvents() {
        // Mark as read buttons
        notificationList.querySelectorAll('.mark-read-btn').forEach(btn => {
            btn.addEventListener('click', async (e) => {
                e.stopPropagation();
                const notificationId = btn.getAttribute('data-notification-id');
                await markAsRead(notificationId);
            });
        });

        // View detail buttons
        notificationList.querySelectorAll('.view-detail-btn').forEach(btn => {
            btn.addEventListener('click', async (e) => {
                e.stopPropagation();
                const notificationId = btn.getAttribute('data-notification-id');
                const link = btn.getAttribute('data-link');
                
                // Mark as read trước khi chuyển trang
                await markAsRead(notificationId);
                
                // Chuyển trang
                window.location.href = link;
            });
        });

        // Click on notification items
        notificationList.querySelectorAll('.notification-item').forEach(item => {
            item.addEventListener('click', async (e) => {
                // Không xử lý click nếu click vào button action
                if (e.target.closest('.notification-actions')) {
                    return;
                }
                
                const notificationId = item.getAttribute('data-notification-id');
                const notification = notifications.find(n => n.id == notificationId);
                
                // Nếu có link, đánh dấu đã đọc ngay lập tức rồi chuyển trang
                if (notification && notification.link) {
                    // Đánh dấu đã đọc ngay lập tức (không đợi API)
                    if (item.classList.contains('unread')) {
                        markAsReadImmediately(notificationId);
                    }
                    // Chuyển trang ngay lập tức
                    window.location.href = notification.link;
                    return;
                }
                
                // Nếu không có link và chưa đọc, đánh dấu đã đọc
                if (item.classList.contains('unread')) {
                    await markAsRead(notificationId);
                }
            });
        });
    }

    // Mark as read ngay lập tức (không đợi API)
    function markAsReadImmediately(notificationId) {
        const notification = notifications.find(n => n.id == notificationId);
        if (notification && !notification.isRead) {
            // Cập nhật local state ngay lập tức
            notification.isRead = true;
            unreadCount = Math.max(0, unreadCount - 1);
            updateBadge();
            
            // Gửi API call trong background (không đợi)
            markAsReadInBackground(notificationId);
        }
    }

    // Mark as read
    async function markAsRead(notificationId) {
        try {
            const response = await authenticatedFetch(`/api/notifications/${notificationId}/read`, {
                method: 'POST'
            });
            
            if (response && response.ok) {
                // Cập nhật local state
                const notification = notifications.find(n => n.id == notificationId);
                if (notification) {
                    notification.isRead = true;
                    unreadCount = Math.max(0, unreadCount - 1);
                    updateBadge();
                    
                    // Gửi thông báo qua WebSocket nếu có
                    if (wsManager && wsManager.isConnected()) {
                        wsManager.send({
                            type: 'MARK_AS_READ',
                            notificationId: notificationId
                        });
                    }
                    
                    renderNotifications();
                }
            }
        } catch (error) {
            console.error('Error marking notification as read:', error);
        }
    }

    // Mark as read trong background (không đợi response)
    async function markAsReadInBackground(notificationId) {
        try {
            const response = await authenticatedFetch(`/api/notifications/${notificationId}/read`, {
                method: 'POST'
            });
            
            if (response && response.ok) {
                // Gửi thông báo qua WebSocket nếu có
                if (wsManager && wsManager.isConnected()) {
                    wsManager.send({
                        type: 'MARK_AS_READ',
                        notificationId: notificationId
                    });
                }
            }
        } catch (error) {
            console.error('Error marking notification as read in background:', error);
        }
    }

    // Mark all as read
    async function markAllAsRead() {
        try {
            const response = await authenticatedFetch('/api/notifications/mark-all-read', {
                method: 'POST'
            });
            
            if (response && response.ok) {
                // Cập nhật local state
                notifications.forEach(n => n.isRead = true);
                unreadCount = 0;
                updateBadge();
                
                // Gửi thông báo qua WebSocket nếu có
                if (wsManager && wsManager.isConnected()) {
                    wsManager.send({
                        type: 'MARK_ALL_AS_READ'
                    });
                }
                
                renderNotifications();
            }
        } catch (error) {
            console.error('Error marking all notifications as read:', error);
        }
    }

    // Xử lý thông báo mới
    // fromWebSocket: true = từ WebSocket (hiển thị toast), false = từ load ban đầu (không hiển thị toast)
    function handleNewNotification(notification, fromWebSocket = false) {
        // Kiểm tra xem thông báo này đã tồn tại chưa (tránh duplicate)
        const existingNotification = notifications.find(n => n.id === notification.id);
        if (existingNotification) {
            return; // Thông báo đã tồn tại, không làm gì
        }
        
        // Thêm vào đầu danh sách
        notifications.unshift(notification);
        
        // Cập nhật số lượng chưa đọc
        if (!notification.isRead) {
            unreadCount++;
            updateBadge();
        }
        
        // Re-render notifications
        renderNotifications();
        
        // Chỉ hiển thị toast notification cho thông báo mới thực sự từ WebSocket
        if (window.toastManager && fromWebSocket && window.notificationsLoaded) {
            window.toastManager.info(notification.message);
        }
    }

    // Xử lý khi kết bạn được chấp nhận
    function handleFriendshipAccepted(data) {
        const { notificationId, friendshipStatus } = data;
        updateNotificationStatus(notificationId, 'accepted', friendshipStatus);
    }

    // Xử lý khi kết bạn bị từ chối
    function handleFriendshipRejected(data) {
        const { notificationId, friendshipStatus } = data;
        updateNotificationStatus(notificationId, 'rejected', friendshipStatus);
    }

    // Xử lý khi hủy kết bạn
    function handleFriendshipCancelled(data) {
        const { notificationId, friendshipStatus } = data;
        updateNotificationStatus(notificationId, 'cancelled', friendshipStatus);
    }

    // Xử lý khi đã gửi lời mời kết bạn
    function handleFriendshipRequestSent(data) {
        const { notificationId, friendshipStatus } = data;
        updateNotificationStatus(notificationId, 'sent', friendshipStatus);
    }

    // Cập nhật trạng thái notification và button
    function updateNotificationStatus(notificationId, action, friendshipStatus) {
        const notification = notifications.find(n => n.id == notificationId);
        if (!notification) return;

        console.log('✅ Cập nhật notification:', { notificationId, action, friendshipStatus });

        // Cập nhật trạng thái friendship
        notification.friendshipStatus = friendshipStatus;
        notification.action = action;
        
        // Cập nhật notification type để hiển thị đúng UI
        if (action === 'accepted') {
            notification.notificationType = 'FRIENDSHIP_ACCEPTED';
            notification.type = 'FRIENDSHIP_ACCEPTED';
        } else if (action === 'rejected') {
            notification.notificationType = 'FRIENDSHIP_REJECTED';
            notification.type = 'FRIENDSHIP_REJECTED';
        } else if (action === 'cancelled') {
            notification.notificationType = 'FRIENDSHIP_CANCELLED';
            notification.type = 'FRIENDSHIP_CANCELLED';
        }

        // Re-render để cập nhật button
        renderNotifications();
    }

    // Xử lý cập nhật trạng thái friendship từ server
    function handleFriendshipStatusUpdate(data) {
        const { notificationId, action, friendshipStatus, senderId } = data;
        
        console.log('🔄 Cập nhật trạng thái friendship:', { notificationId, action, friendshipStatus, senderId });
        
        if (notificationId) {
            // Cập nhật theo notificationId
            updateNotificationStatus(notificationId, action, friendshipStatus);
        } else if (senderId) {
            // Cập nhật theo senderId (cho trường hợp unfriend)
            const notification = notifications.find(n => n.senderId === senderId && n.type && n.type.includes('FRIENDSHIP'));
            if (notification) {
                updateNotificationStatus(notification.id, action, friendshipStatus);
            }
        }
    }

    // Cập nhật notification trong danh sách
    function updateNotificationInList(updatedNotification) {
        const index = notifications.findIndex(n => n.id === updatedNotification.id);
        if (index !== -1) {
            notifications[index] = updatedNotification;
            renderNotifications();
        }
    }

    // Xóa notification khỏi danh sách
    function removeNotificationFromList(notificationId) {
        notifications = notifications.filter(n => n.id !== notificationId);
        renderNotifications();
    }

    // Format thời gian
    function formatTime(timestamp) {
        if (!timestamp) return '';
        
        const date = new Date(timestamp);
        const now = new Date();
        const diffMs = now - date;
        const diffMins = Math.floor(diffMs / 60000);
        const diffHours = Math.floor(diffMs / 3600000);
        const diffDays = Math.floor(diffMs / 86400000);

        if (diffMins < 1) return 'Vừa xong';
        if (diffMins < 60) return `${diffMins} phút trước`;
        if (diffHours < 24) return `${diffHours} giờ trước`;
        if (diffDays < 7) return `${diffDays} ngày trước`;
        
        return date.toLocaleDateString('vi-VN');
    }

    // Escape HTML
    function escapeHtml(str) {
        return String(str).replace(/[&<>"]+/g, s => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;'}[s]));
    }

    // Event listeners
    markAllReadBtn.addEventListener('click', markAllAsRead);

    // Khởi tạo
    loadNotifications();
    loadUnreadCount();
    
    // Khởi tạo WebSocket handlers
    initializeWebSocketHandlers();

    // Xử lý chấp nhận kết bạn
    async function handleAcceptFriend(senderId, notificationId) {
        try {
            const response = await authenticatedFetch(`/api/friends/accept/${senderId}`, {
                method: 'POST'
            });
            
            if (response && response.ok) {
                // Đánh dấu notification là đã đọc
                await markAsRead(notificationId);
                
                // Cập nhật ngay lập tức để UI responsive
                updateNotificationStatus(notificationId, 'accepted', 'ACCEPTED');
                
                // Hiển thị thông báo thành công
                if (window.toastManager) {
                    window.toastManager.success('Đã chấp nhận yêu cầu kết bạn!');
                }
                
                // Gửi thông báo qua WebSocket để đồng bộ với server
                if (wsManager && wsManager.isConnected()) {
                    wsManager.send({
                        type: 'FRIENDSHIP_ACCEPTED',
                        senderId: senderId,
                        notificationId: notificationId
                    });
                }
            } else {
                // Hiển thị thông báo lỗi
                if (window.toastManager) {
                    window.toastManager.error('Có lỗi xảy ra khi chấp nhận yêu cầu kết bạn');
                }
            }
        } catch (error) {
            console.error('Error accepting friend:', error);
            // Hiển thị thông báo lỗi
            if (window.toastManager) {
                window.toastManager.error('Có lỗi xảy ra khi chấp nhận yêu cầu kết bạn');
            }
        }
    }

    // Xử lý từ chối kết bạn
    async function handleRejectFriend(senderId, notificationId) {
        try {
            const response = await authenticatedFetch(`/api/friends/decline/${senderId}`, {
                method: 'POST'
            });
            
            if (response && response.ok) {
                // Đánh dấu notification là đã đọc
                await markAsRead(notificationId);
                
                // Cập nhật ngay lập tức để UI responsive
                updateNotificationStatus(notificationId, 'rejected', 'REJECTED');
                
                // Hiển thị thông báo thành công
                if (window.toastManager) {
                    window.toastManager.info('Đã từ chối yêu cầu kết bạn');
                }
                
                // Gửi thông báo qua WebSocket để đồng bộ với server
                if (wsManager && wsManager.isConnected()) {
                    wsManager.send({
                        type: 'FRIENDSHIP_REJECTED',
                        senderId: senderId,
                        notificationId: notificationId
                    });
                }
            } else {
                // Hiển thị thông báo lỗi
                if (window.toastManager) {
                    window.toastManager.error('Có lỗi xảy ra khi từ chối yêu cầu kết bạn');
                }
            }
        } catch (error) {
            console.error('Error rejecting friend:', error);
            // Hiển thị thông báo lỗi
            if (window.toastManager) {
                window.toastManager.error('Có lỗi xảy ra khi từ chối yêu cầu kết bạn');
            }
        }
    }

    // Xử lý hủy kết bạn
    async function handleUnfriend(senderId) {
        try {
            const response = await authenticatedFetch(`/api/friends/${senderId}`, {
                method: 'DELETE'
            });
            
            if (response && response.ok) {
                // Tìm notification có senderId này và cập nhật
                const notification = notifications.find(n => n.senderId === senderId && n.type && n.type.includes('FRIENDSHIP'));
                if (notification) {
                    // Đánh dấu notification là đã đọc
                    await markAsRead(notification.id);
                    
                    // Cập nhật ngay lập tức để UI responsive
                    updateNotificationStatus(notification.id, 'cancelled', 'CANCELLED');
                }
                
                // Hiển thị thông báo thành công
                if (window.toastManager) {
                    window.toastManager.info('Đã hủy kết bạn');
                }
                
                // Gửi thông báo qua WebSocket để đồng bộ với server
                if (wsManager && wsManager.isConnected()) {
                    wsManager.send({
                        type: 'FRIENDSHIP_CANCELLED',
                        senderId: senderId
                    });
                }
            } else {
                // Hiển thị thông báo lỗi
                if (window.toastManager) {
                    window.toastManager.error('Có lỗi xảy ra khi hủy kết bạn');
                }
            }
        } catch (error) {
            console.error('Error unfriending:', error);
            // Hiển thị thông báo lỗi
            if (window.toastManager) {
                window.toastManager.error('Có lỗi xảy ra khi hủy kết bạn');
            }
        }
    }

    // Expose functions globally
    window.markNotificationAsRead = markAsRead;
    window.loadNotifications = loadNotifications;
    window.handleAcceptFriend = handleAcceptFriend;
    window.handleRejectFriend = handleRejectFriend;
    window.handleUnfriend = handleUnfriend;
})();
