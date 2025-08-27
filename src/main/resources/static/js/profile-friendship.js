(() => {
    const friendshipActions = document.querySelector('.friendship-actions');
    
    const targetUserId = friendshipActions?.getAttribute('data-user-id');
    if (!targetUserId) { /* own profile */ }
    
    // Load friendship status khi trang load (nếu có target user)
    if (targetUserId) loadFriendshipStatus();

    // ===== Tab switching: Bài viết (#feed) <-> Bạn bè =====
    try {
        const feed = document.getElementById('feed');
        const postsTab = document.getElementById('posts-tab');
        const friendsTab = document.getElementById('friends-tab') || document.getElementById('about-tab');
        const friendsContainer = document.getElementById('friendsTabContent') || document.getElementById('friendsList')?.closest('.card') || document.getElementById('friendsList')?.parentElement;

        // Hàm load danh sách bạn bè (lazy 1 lần)
        let friendsLoaded = false;
        let allFriends = [];
        async function loadFriendsOnce() {
            if (friendsLoaded) return;
            try {
                const response = await authenticatedFetch('/api/friends/list');
                if (!response || !response.ok) {
                    renderFriends([]);
                    return;
                }
                const users = await response.json();
                allFriends = Array.isArray(users) ? users : [];
                renderFriends(allFriends);
                friendsLoaded = true;
                // nếu user đã nhập sẵn từ khóa, lọc ngay
                wireSearch(true);
            } catch (e) {
                /* noop */
            }
        }

        function renderFriends(users) {
            const list = document.getElementById('friendsList');
            const empty = document.getElementById('friendsEmpty');
            const count = document.getElementById('friendsCount');
            if (!users || users.length === 0) {
                if (empty) empty.classList.remove('d-none');
                if (count) count.textContent = '';
                if (list) list.innerHTML = '';
                return;
            }
            if (empty) empty.classList.add('d-none');
            if (count) count.textContent = `${users.length} bạn`;
            if (!list) return;
            list.innerHTML = users.map(u => {
                const avatar = u.avatar && u.avatar.trim() !== '' ? u.avatar : 'https://cdn.pixabay.com/photo/2023/02/18/11/00/icon-7797704_640.png';
                const name = u.fullName && u.fullName.trim() !== '' ? u.fullName : (u.username || 'Người dùng');
                const username = u.username || '';
                return `
                    <div class="col-12 col-sm-6 col-md-4">
                        <div class="border rounded-3 p-3 h-100 d-flex align-items-center justify-content-between shadow-sm" style="transition:box-shadow .2s;">
                            <a class="d-flex align-items-center text-decoration-none flex-grow-1 me-2" href="/profile/${username}">
                                <img src="${avatar}" class="rounded-circle me-3" width="44" height="44" onerror="this.src='https://cdn.pixabay.com/photo/2023/02/18/11/00/icon-7797704_640.png'" alt="avatar">
                                <div class="overflow-hidden">
                                    <div class="fw-semibold text-dark text-truncate">${name}</div>
                                    <div class="small text-muted text-truncate">@${username}</div>
                                </div>
                            </a>
                            <button class="btn btn-danger fw-semibold p-2 unfriend-btn" data-user-id="${u.id}" style="font-size: 11px">
                                <i class="fa fa-user-minus me-1"></i>Hủy
                            </button>
                        </div>
                    </div>`;
            }).join('');

            // Wire buttons hủy kết bạn
            list.querySelectorAll('.unfriend-btn').forEach(btn => {
                btn.addEventListener('click', async (e) => {
                    e.preventDefault();
                    e.stopPropagation();
                    const id = btn.getAttribute('data-user-id');
                    if (!id) return;
                    const confirmText = 'Bạn có chắc chắn muốn hủy kết bạn?';
                    if (!confirm(confirmText)) return;
                    btn.disabled = true;
                    btn.innerHTML = '<i class="fa fa-spinner fa-spin"></i>';
                    try {
                        await unfriend(parseInt(id));
                        // remove card
                        const col = btn.closest('.col-12');
                        if (col) col.remove();
                        // update count
                        const current = friendsContainer.querySelectorAll('.col-12').length;
                        if (count) count.textContent = current > 0 ? `${current} bạn` : '';
                        if (current === 0 && empty) empty.classList.remove('d-none');
                    } catch (err) {
                        console.error('Unfriend error:', err);
                        btn.disabled = false;
                        btn.innerHTML = '<i class=\"fa fa-user-minus me-1\"></i>Hủy';
                    }
                });
            });
        }

        // Lọc danh sách bạn bè theo ô tìm kiếm trong card Bạn bè
        function wireSearch(runImmediate = false) {
            const input = document.getElementById('searchFriend') || document.getElementById('userSearchInput');
            const btn = document.getElementById('searchFriendBtn') || document.getElementById('userSearchBtn');
            if (!input || !btn) return;
            const doFilter = () => {
                const q = (input.value || '').toLowerCase().trim();
                if (!q) {
                    const empty = document.getElementById('friendsEmpty');
                    if (empty) empty.classList.add('d-none');
                    return renderFriends(allFriends);
                }
                const filtered = allFriends.filter(u => {
                    const name = (u.fullName || '').toLowerCase();
                    const username = (u.username || '').toLowerCase();
                    return name.includes(q) || username.includes(q);
                });
                renderFriends(filtered);
            };
            input.addEventListener('input', doFilter);
            input.addEventListener('keyup', (e) => { if (e.key === 'Enter') doFilter(); });
            btn.addEventListener('click', (e) => { e.preventDefault(); doFilter(); });
            if (runImmediate) doFilter();
        }

        function showFeed() {
            if (feed) feed.classList.remove('d-none');
            if (friendsContainer) friendsContainer.classList.add('d-none');
        }
        function showFriends() {
            if (feed) feed.classList.add('d-none');
            if (friendsContainer) friendsContainer.classList.remove('d-none');
        }

        // Gán click cho tab
        postsTab?.addEventListener('click', (e) => { e.preventDefault(); showFeed(); });
        friendsTab?.addEventListener('click', async (e) => { e.preventDefault(); await loadFriendsOnce(); showFriends(); });

        // Mặc định hiển thị feed
        showFeed();

        // Nếu phần danh sách bạn bè đã có sẵn trong DOM (đang ở tab Bạn bè)
        // thì tự động load dữ liệu và gắn tìm kiếm ngay không cần click tab
        if (document.getElementById('friendsList')) {
            loadFriendsOnce();
        }
    } catch (e) {
        /* noop */
    }

    // Tìm kiếm bạn bè: lọc trực tiếp danh sách bên dưới
    
    
    async function loadFriendshipStatus() {
        try {
            const response = await authenticatedFetch(`/api/friends/status/${targetUserId}`);
            if (!response || !response.ok) throw new Error('Failed to load friendship status');
            
            const data = await response.json();
            console.log('Friendship status:', data);
            renderFriendshipButtons(data);
        } catch (error) {
            // Hiển thị nút kết bạn mặc định nếu có lỗi
            renderFriendshipButtons({ status: 'NONE' });
        }
    }
    
    function renderFriendshipButtons(friendshipData) {
        const status = friendshipData.status;
        console.log('Rendering buttons for status:', status);
        
        let buttonHtml = '';
        
        switch (status) {
            case 'NONE':
                buttonHtml = `
                    <button class="btn btn-primary fw-semibold px-4 py-2" onclick="sendFriendRequest(${targetUserId})">
                        <i class="fa fa-user-plus me-2"></i>Kết bạn
                    </button>
                `;
                break;
                
            case 'PENDING':
                if (friendshipData.requestedByMe) {
                    // Hủy yêu cầu (màu đỏ)
                    buttonHtml = `
                        <button class="btn btn-danger fw-semibold px-2 py-2" onclick="cancelFriendRequest(${targetUserId})">
                            <i class="fa fa-times me-2"></i>Hủy yêu cầu
                        </button>
                    `;
                } else {
                    // Chấp nhận (xanh lá) / Từ chối (đỏ)
                    buttonHtml = `
                        <div class="btn-group" role="group">
                            <button class="btn btn-success fw-semibold px-2 py-2" onclick="acceptFriendRequest(${targetUserId})">
                                <i class="fa fa-check me-2"></i>Chấp nhận
                            </button>
                            <button class="btn btn-danger fw-semibold px-2 py-2" onclick="declineFriendRequest(${targetUserId})">
                                <i class="fa fa-times me-2"></i>Từ chối
                            </button>
                        </div>
                    `;
                }
                break;
                
            case 'ACCEPTED':
                // Hủy kết bạn (màu đỏ)
                buttonHtml = `
                    <button class="btn btn-danger fw-semibold px-2 py-2" onclick="unfriend(${targetUserId})">
                        <i class="fa fa-user-minus me-2"></i>Hủy kết bạn
                    </button>
                `;
                break;
                
            case 'BLOCKED':
                buttonHtml = `
                    <button class="btn btn-warning fw-semibold px-2 py-2" disabled>
                        <i class="fa fa-ban me-2"></i>Đã chặn
                    </button>
                `;
                break;
                
            default:
                buttonHtml = `
                    <button class="btn btn-primary fw-semibold px-2 py-2" onclick="sendFriendRequest(${targetUserId})">
                        <i class="fa fa-user-plus me-2"></i>Kết bạn
                    </button>
                `;
        }
        
        friendshipActions.innerHTML = buttonHtml;
    }
    
    // Các hàm xử lý kết bạn
    async function sendFriendRequest(targetUserId) {
        try {
            const response = await authenticatedFetch(`/api/friends/request/${targetUserId}`, {
                method: 'POST'
            });
            
            if (response && response.ok) {
                showToast('Đã gửi yêu cầu kết bạn!', 'success');
                // Reload trạng thái
                loadFriendshipStatus();
            } else {
                const error = await response.json();
                showToast(error.message || 'Có lỗi xảy ra', 'error');
            }
        } catch (error) {
            console.error('Error sending friend request:', error);
            showToast('Có lỗi xảy ra khi gửi yêu cầu kết bạn', 'error');
        }
    }
    
    async function acceptFriendRequest(requesterId) {
        try {
            const response = await authenticatedFetch(`/api/friends/accept/${requesterId}`, {
                method: 'POST'
            });
            
            if (response && response.ok) {
                showToast('Đã chấp nhận yêu cầu kết bạn!', 'success');
                loadFriendshipStatus();
            } else {
                const error = await response.json();
                showToast(error.message || 'Có lỗi xảy ra', 'error');
            }
        } catch (error) {
            console.error('Error accepting friend request:', error);
            showToast('Có lỗi xảy ra khi chấp nhận yêu cầu kết bạn', 'error');
        }
    }
    
    async function declineFriendRequest(requesterId) {
        try {
            const response = await authenticatedFetch(`/api/friends/decline/${requesterId}`, {
                method: 'POST'
            });
            
            if (response && response.ok) {
                showToast('Đã từ chối yêu cầu kết bạn', 'info');
                loadFriendshipStatus();
            } else {
                const error = await response.json();
                showToast(error.message || 'Có lỗi xảy ra', 'error');
            }
        } catch (error) {
            console.error('Error declining friend request:', error);
            showToast('Có lỗi xảy ra khi từ chối yêu cầu kết bạn', 'error');
        }
    }
    
    async function cancelFriendRequest(targetUserId) {
        try {
            const response = await authenticatedFetch(`/api/friends/cancel/${targetUserId}`, {
                method: 'POST'
            });
            
            if (response && response.ok) {
                showToast('Đã hủy yêu cầu kết bạn', 'info');
                loadFriendshipStatus();
            } else {
                const error = await response.json();
                showToast(error.message || 'Có lỗi xảy ra', 'error');
            }
        } catch (error) {
            console.error('Error canceling friend request:', error);
            showToast('Có lỗi xảy ra khi hủy yêu cầu kết bạn', 'error');
        }
    }
    
    async function unfriend(friendUserId) {
        if (!confirm('Bạn có chắc chắn muốn hủy kết bạn với người này?')) {
            return;
        }
        
        try {
            const response = await authenticatedFetch(`/api/friends/${friendUserId}`, {
                method: 'DELETE'
            });
            
            if (response && response.ok) {
                showToast('Đã hủy kết bạn', 'info');
                loadFriendshipStatus();
            } else {
                const error = await response.json();
                showToast(error.message || 'Có lỗi xảy ra', 'error');
            }
        } catch (error) {
            console.error('Error unfriending:', error);
            showToast('Có lỗi xảy ra khi hủy kết bạn', 'error');
        }
    }
    
    // Hàm hiển thị toast
    function showToast(message, type = 'info') {
        let toastId;
        let toastBody;
        
        switch (type) {
            case 'success':
                toastId = 'successToast';
                break;
            case 'error':
                toastId = 'errorToast';
                break;
            default:
                toastId = 'friendshipToast';
                break;
        }
        
        const toast = document.getElementById(toastId);
        
        if (toast) {
            // Cập nhật nội dung toast
            if (type === 'info') {
                toastBody = toast.querySelector('#friendshipMessage');
            } else {
                toastBody = toast.querySelector('.toast-body');
            }
            
            if (toastBody) {
                toastBody.textContent = message;
            }
            
            // Hiển thị toast
            const bsToast = new bootstrap.Toast(toast);
            bsToast.show();
        } else {
            // Fallback: alert nếu không có toast
            alert(message);
        }
    }
    
    // Expose functions globally
    window.sendFriendRequest = sendFriendRequest;
    window.acceptFriendRequest = acceptFriendRequest;
    window.declineFriendRequest = declineFriendRequest;
    window.cancelFriendRequest = cancelFriendRequest;
    window.unfriend = unfriend;
    
})();
