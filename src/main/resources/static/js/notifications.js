(() => {
    console.log('Notifications script loaded!');
    
    const notificationBadge = document.getElementById('notificationBadge');
    const notificationList = document.getElementById('notificationList');
    const markAllReadBtn = document.getElementById('markAllRead');

    console.log('Elements found:', { notificationBadge, notificationList, markAllReadBtn });

    if (!notificationBadge || !notificationList || !markAllReadBtn) {
        console.log('Some elements not found, exiting');
        return;
    }

    let notifications = [];
    let unreadCount = 0;

    // Load notifications on page load
    console.log('Loading notifications...');
    loadNotifications();
    loadUnreadCount();

    // Auto-refresh every 30 seconds
    setInterval(() => {
        loadUnreadCount();
    }, 30000);

    async function loadNotifications() {
        console.log('loadNotifications called');
        try {
            const response = await authenticatedFetch('/api/notifications');
            console.log('API response:', response);
            if (!response || !response.ok) throw new Error('Failed to load notifications');
            
            notifications = await response.json();
            console.log('Notifications loaded:', notifications);
            renderNotifications();
        } catch (error) {
            console.error('Error loading notifications:', error);
        }
    }

    async function loadUnreadCount() {
        console.log('loadUnreadCount called');
        try {
            const response = await authenticatedFetch('/api/notifications/unread-count');
            console.log('Unread count response:', response);
            if (!response || !response.ok) throw new Error('Failed to load unread count');
            
            const data = await response.json();
            unreadCount = data.count || 0;
            console.log('Unread count:', unreadCount);
            updateBadge();
        } catch (error) {
            console.error('Error loading unread count:', error);
        }
    }

    function updateBadge() {
        if (unreadCount > 0) {
            notificationBadge.style.display = 'block';
            notificationBadge.textContent = unreadCount > 99 ? '99+' : unreadCount.toString();
        } else {
            notificationBadge.style.display = 'none';
        }
    }

    function renderNotifications() {
        if (!notifications.length) {
            notificationList.innerHTML = `
                <div class="text-center py-3 text-muted">
                    <i class="fa fa-bell-slash fa-2x mb-2"></i>
                    <div>Không có thông báo nào</div>
                </div>
            `;
            return;
        }

        notificationList.innerHTML = notifications.map(notification => `
            <div class="notification-item ${notification.isRead ? '' : 'unread'}" 
                 data-notification-id="${notification.id}">
                <div class="notification-content">
                    <img src="${notification.senderAvatar || 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMjQiIGhlaWdodD0iMjQiIHZpZXdCb3g9IjAgMCAyNCAyNCIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KPHJlY3Qgd2lkdGg9IjI0IiBoZWlnaHQ9IjI0IiBmaWxsPSIjRjVGNUY1Ii8+CjxwYXRoIGQ9Ik0xMiAxMkMxNC4yMDkxIDEyIDE2IDEwLjIwOTEgMTYgOEMxNiA1Ljc5MDg2IDE0LjIwOTEgNCAxMiA0QzkuNzkwODYgNCA4IDUuNzkwODYgOCA4QzggMTAuMjA5MSA5Ljc5MDg2IDEyIDEyIDEyWiIgZmlsbD0iI0Q5RDlEOSIvPgo8cGF0aCBkPSJNMTIgMTRDMTUuMzEzNyAxNCAxOCAxNi42ODYzIDE4IDIwSDFWMTZDMSAxNi42ODYzIDMuNjg2MyAxNCA3IDE0SDEyWiIgZmlsbD0iI0Q5RDlEOSIvPgo8L3N2Zz4K'}"
                         class="notification-avatar" alt="avatar"
                         onerror="this.src='data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMjQiIGhlaWdodD0iMjQiIHZpZXdCb3g9IjAgMCAyNCAyNCIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KPHJlY3Qgd2lkdGg9IjI0IiBoZWlnaHQ9IjI0IiBmaWxsPSIjRjVGNUY1Ii8+CjxwYXRoIGQ9Ik0xMiAxMkMxNC4yMDkxIDEyIDE2IDEwLjIwOTEgMTYgOEMxNiA1Ljc5MDg2IDE0LjIwOTEgNCAxMiA0QzkuNzkwODYgNCA4IDUuNzkwODYgOCA4QzggMTAuMjA5MSA5Ljc5MDg2IDEyIDEyIDEyWiIgZmlsbD0iI0Q5RDlEOSIvPgo8cGF0aCBkPSJNMTIgMTRDMTUuMzEzNyAxNCAxOCAxNi42ODYzIDE4IDIwSDFWMTZDMSAxNi42ODYzIDMuNjg2MyAxNCA3IDE0SDEyWiIgZmlsbD0iI0Q5RDlEOSIvPgo8L3N2Zz4K'">
                    <div class="notification-text">
                        <div class="notification-message">${escapeHtml(notification.message)}</div>
                        <div class="notification-time">${formatTime(notification.createdAt)}</div>
                        ${notification.link ? `
                            <div class="notification-actions">
                                <button class="btn btn-sm btn-primary" onclick="window.location.href='${notification.link}'">
                                    Xem chi tiết
                                </button>
                                ${!notification.isRead ? `
                                    <button class="btn btn-sm btn-outline-secondary mark-read-btn" 
                                            data-notification-id="${notification.id}">
                                        Đánh dấu đã đọc
                                    </button>
                                ` : ''}
                            </div>
                        ` : ''}
                    </div>
                </div>
            </div>
        `).join('');

        // Wire up mark as read buttons
        wireMarkAsReadButtons();
    }

    function wireMarkAsReadButtons() {
        notificationList.querySelectorAll('.mark-read-btn').forEach(btn => {
            btn.addEventListener('click', async (e) => {
                e.stopPropagation();
                const notificationId = btn.getAttribute('data-notification-id');
                await markAsRead(notificationId);
            });
        });

        // Wire up notification item clicks
        notificationList.querySelectorAll('.notification-item').forEach(item => {
            item.addEventListener('click', async () => {
                const notificationId = item.getAttribute('data-notification-id');
                if (!item.classList.contains('unread')) return;
                
                await markAsRead(notificationId);
            });
        });
    }

    async function markAsRead(notificationId) {
        try {
            const response = await authenticatedFetch(`/api/notifications/${notificationId}/read`, {
                method: 'POST'
            });
            
            if (response && response.ok) {
                // Update local state
                const notification = notifications.find(n => n.id == notificationId);
                if (notification) {
                    notification.isRead = true;
                    unreadCount = Math.max(0, unreadCount - 1);
                    updateBadge();
                    renderNotifications();
                }
            }
        } catch (error) {
            console.error('Error marking notification as read:', error);
        }
    }

    async function markAllAsRead() {
        try {
            const response = await authenticatedFetch('/api/notifications/mark-all-read', {
                method: 'POST'
            });
            
            if (response && response.ok) {
                // Update local state
                notifications.forEach(n => n.isRead = true);
                unreadCount = 0;
                updateBadge();
                renderNotifications();
            }
        } catch (error) {
            console.error('Error marking all notifications as read:', error);
        }
    }

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

    function escapeHtml(str) {
        return String(str).replace(/[&<>"]+/g, s => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;'}[s]));
    }

    // Event listeners
    markAllReadBtn.addEventListener('click', markAllAsRead);

    // Expose functions globally for onclick handlers
    window.markNotificationAsRead = markAsRead;
    window.loadNotifications = loadNotifications;
})();
