(() => {
    const notificationBadge = document.getElementById('notificationBadge');
    const notificationList = document.getElementById('notificationList');
    const markAllReadBtn = document.getElementById('markAllRead');
    
    if (!notificationBadge || !notificationList || !markAllReadBtn) {
        return;
    }

    let notifications = [];
    let unreadCount = 0;

    // Load notifications on page load
    loadNotifications();
    loadUnreadCount();

    // Auto-refresh every 30 seconds
    setInterval(() => {
        loadUnreadCount();
    }, 30000);

    async function loadNotifications() {
        try {
            const response = await authenticatedFetch('/api/notifications');
            if (!response || !response.ok) throw new Error('Failed to load notifications');
            
            notifications = await response.json();
            renderNotifications();
        } catch (error) {
            console.error('Error loading notifications:', error);
        }
    }

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

    function updateBadge() {
        if (unreadCount > 0) {
            notificationBadge.style.display = 'block';
            notificationBadge.textContent = unreadCount > 99 ? '99+' : unreadCount.toString();
        } else {
            notificationBadge.style.display = 'none';
        }
    }

    function renderNotifications() {
        const emptyState = `
            <div class="text-center py-3 text-muted">
                <i class="fa fa-bell-slash fa-2x mb-2"></i>
                <div>Không có thông báo nào</div>
            </div>
        `;

        if (!notifications.length) {
            notificationList.innerHTML = emptyState;
            return;
        }

        // Render header notifications
        notificationList.innerHTML = notifications.map(notification => `
            <div class="notification-item ${notification.isRead ? '' : 'unread'}" 
                 data-notification-id="${notification.id}">
                <div class="notification-content">
                    <img src="${notification.senderAvatar || 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMjQiIGhlaWdodD0iMjQiIHZpZXdCb3g9IjAgMCAyNCAyNCIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KPHJlY3Qgd2lkdGg9IjI0IiBoZWlnaHQ9IjI0IiBmaWxsPSIjRjVGNUY1Ii8+CjxwYXRoIGQ9Ik0xMiAxMkMxNC4yMDkxIDEyIDE2IDEwLjIwOTEgMTYgOEMxNiA1Ljc5MDg2IDE0LjIwOTEgNCAxMiA0QzkuNzkwODYgNCA4IDUuNzkwODYgOCA4QzggMTAuMjA5MSA5Ljc5MDg2IDEyIDEyIDEyWiIgZmlsbD0iI0Q5RDlEOSIvPgo8cGF0aCBkPSJNMTIgMTRDMTUuMzEzNyAxNCAxOCAxNi42ODYzIDE4IDIwSDFWMTZDMSAxNi42ODYzIDMuNjg2MyAxNCA3IDE0SDEyWiIgZmlsbD0iI0Q5RDlEOSIvPgo8L3N2Zz4K'}"
                         class="notification-avatar" alt="avatar"
                         onerror="this.src='data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMjQiIGhlaWdodD0iMjQiIHZpZXdCb3g9IjAgMCAyNCAyNCIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KPHJlY3Qgd2lkdGg9IjI0IiBoZWlnaHQ9IjI0IiBmaWxsPSIjRjVGNUY1Ii8+CjxwYXRoIGQ9Ik0xMiAxMkMxNC4yMDkxIDEyIDE2IDEwLjIwOTEgMTYgOEMxNiA1Ljc5MDg2IDE0LjIwOTEgNCAxMiA0QzkuNzkwODYgNCA4IDUuNzkwODkgOCA4QzggMTAuMjA5MSA5Ljc5MDg2IDEyIDEyIDEyWiIgZmlsbD0iI0Q5RDlEOSIvPgo8cGF0aCBkPSJNMTIgMTRDMTUuMzEzNyAxNCAxOCAxNi42ODYzIDE4IDIwSDFWMTZDMSAxNi42ODYzIDMuNjg2MyAxNCA3IDE0SDEyWiIgZmlsbD0iI0Q5RDlEOSIvPgo8L3N2Zz4K'">
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
        // Wire up header notification buttons
        notificationList.querySelectorAll('.mark-read-btn').forEach(btn => {
            btn.addEventListener('click', async (e) => {
                e.stopPropagation();
                const notificationId = btn.getAttribute('data-notification-id');
                await markAsRead(notificationId);
            });
        });

        // Wire up header notification item clicks
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

    function showToastNotification(notification) {
        // Create toast element
        const toast = document.createElement('div');
        toast.className = 'toast-notification';
        toast.innerHTML = `
            <div class="toast-content">
                <div class="toast-message">${escapeHtml(notification.message)}</div>
                <div class="toast-time">Vừa xong</div>
            </div>
        `;
        
        // Add to page
        document.body.appendChild(toast);
        
        // Show animation
        setTimeout(() => toast.classList.add('show'), 100);
        
        // Remove after 5 seconds
        setTimeout(() => {
            toast.classList.remove('show');
            setTimeout(() => {
                if (toast.parentNode) {
                    toast.parentNode.removeChild(toast);
                }
            }, 300);
        }, 5000);
    }

    function handleNewNotification(notification) {
        // Add new notification to the beginning of the list
        notifications.unshift(notification);
        
        // Update unread count
        if (!notification.isRead) {
            unreadCount++;
            updateBadge();
        }
        
        // Re-render notifications
        renderNotifications();
        
        // Add animation class to the first notification item
        const firstItem = notificationList.querySelector('.notification-item');
        if (firstItem) {
            firstItem.classList.add('new-notification');
            // Remove class after animation
            setTimeout(() => {
                firstItem.classList.remove('new-notification');
            }, 300);
        }
        
        // Show toast notification
        showToastNotification(notification);
    }

    // Event listeners
    markAllReadBtn.addEventListener('click', markAllAsRead);
    


    // Expose functions globally for onclick handlers
    window.markNotificationAsRead = markAsRead;
    window.loadNotifications = loadNotifications;
})();
