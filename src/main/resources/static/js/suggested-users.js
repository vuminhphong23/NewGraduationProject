(() => {
    const suggestedUsersList = document.getElementById('suggestedUsersList');
    
    if (!suggestedUsersList) {
        return;
    }

    // Load suggested users on page load
    loadSuggestedUsers();

    async function loadSuggestedUsers() {
        try {
            const response = await authenticatedFetch('/api/users/suggested?limit=5');

            if (!response) {
                showToast('Không thể kết nối đến server', 'error');
                return;
            }
            
            if (!response.ok) {
                if (response.status === 401) {
                    showToast('Vui lòng đăng nhập để xem gợi ý', 'warning');
                } else if (response.status === 403) {
                    showToast('Không có quyền truy cập', 'error');
                } else {
                    showToast(`Lỗi server: ${response.status}`, 'error');
                }
                return;
            }
            
            const users = await response.json();
            
            if (users && Array.isArray(users) && users.length > 0) {
                renderSuggestedUsers(users);
            } else {
                renderSuggestedUsers([]);
            }
        } catch (error) {
            showToast('Lỗi khi tải dữ liệu từ database', 'error');
        }
    }

    function getMockSuggestedUsers() {
        return [
            {
                id: 1,
                username: 'hoa.nguyen',
                fullName: 'Hòa Nguyễn',
                avatar: null,
                mutualFriends: 3,
                department: 'Kỹ thuật phần mềm',
                friendshipStatus: 'NONE',
                requestedByMe: false
            },
            {
                id: 2,
                username: 'duc.le',
                fullName: 'Đức Lê',
                avatar: null,
                mutualFriends: 5,
                department: 'Khoa học máy tính',
                friendshipStatus: 'NONE',
                requestedByMe: false
            },
            {
                id: 3,
                username: 'mai.tran',
                fullName: 'Mai Trần',
                avatar: null,
                mutualFriends: 2,
                department: 'Công nghệ thông tin',
                friendshipStatus: 'NONE',
                requestedByMe: false
            },
            {
                id: 4,
                username: 'binh.pham',
                fullName: 'Bình Phạm',
                avatar: null,
                mutualFriends: 4,
                department: 'Kỹ thuật phần mềm',
                friendshipStatus: 'NONE',
                requestedByMe: false
            },
            {
                id: 5,
                username: 'lan.hoang',
                fullName: 'Lan Hoàng',
                avatar: null,
                mutualFriends: 1,
                department: 'Hệ thống thông tin',
                friendshipStatus: 'NONE',
                requestedByMe: false
            }
        ];
    }

    function renderSuggestedUsers(users) {
        if (!users || !users.length) {
            suggestedUsersList.innerHTML = `
                <div class="text-center py-4 text-muted">
                    <i class="fa fa-users fa-2x mb-3 text-secondary"></i>
                    <div class="fw-semibold mb-2">Không có gợi ý nào</div>
                    <div class="small">Có thể bạn đã kết bạn với tất cả mọi người rồi!</div>
                </div>
            `;
            return;
        }

        suggestedUsersList.innerHTML = users.map(user => {
            // Kiểm tra trạng thái kết bạn để hiển thị nút phù hợp
            const isAlreadyFriend = user.friendshipStatus === 'ACCEPTED';
            const isPendingRequest = user.friendshipStatus === 'PENDING';
            const isRequestedByMe = user.requestedByMe;
            
            let buttonContent = '';
            let buttonClass = 'add-friend-btn btn btn-sm ';
            let buttonDisabled = false;
            let buttonTitle = 'Thêm bạn';
            
            if (isAlreadyFriend) {
                buttonContent = '<i class="fa fa-check"></i>';
                buttonClass += 'btn-success';
                buttonTitle = 'Đã là bạn bè';
                buttonDisabled = true;
            } else              if (isPendingRequest) {
                 if (isRequestedByMe) {
                     buttonContent = '<i class="fa fa-clock me-1"></i>Đã gửi';
                     buttonClass += 'btn-secondary';
                     buttonTitle = 'Đã gửi lời mời kết bạn';
                     buttonDisabled = true;
                     
                     // Thêm style để button trông rõ ràng hơn
                     buttonClass += ' opacity-75';
                 } else {
                     buttonContent = '<i class="fa fa-user-check me-1"></i>Chấp nhận';
                     buttonClass += 'btn-info';
                     buttonTitle = 'Chấp nhận lời mời kết bạn';
                 }
             } else {
                 buttonContent = '<i class="fa fa-user-plus me-1"></i>Thêm bạn';
                 buttonClass += 'btn-primary';
             }
            
                         return `
                 <div class="list-group-item d-flex align-items-center gap-2 suggested-user-item px-4" 
                      data-user-id="${user.id}" 
                      data-username="${user.username}"
                      data-fullname="${escapeHtml(user.fullName || user.username)}"
                      data-department="${escapeHtml(user.department)}"
                      data-friendship-status="${user.friendshipStatus}"
                      data-requested-by-me="${isRequestedByMe}">
                                         <img src="${user.avatar || 'https://cdn.pixabay.com/photo/2023/02/18/11/00/icon-7797704_640.png'}"
                          class="rounded-circle" width="34" height="34" alt="avatar"
                          onerror="this.src='https://cdn.pixabay.com/photo/2023/02/18/11/00/icon-7797704_640.png'">
                    <div class="flex-grow-1">
                        <div class="fw-semibold suggested-user-name">${escapeHtml(user.fullName || user.username)}</div>
                        <div class="text-muted small suggested-user-info">
                            <span class="suggested-user-department">${escapeHtml(user.department)}</span>
                            ${user.mutualFriends > 0 ? `
                                <span class="text-primary">• ${user.mutualFriends} bạn chung</span>
                            ` : ''}
                        </div>
                    </div>
                    <div class="suggested-user-actions">
                                                 <button class="${buttonClass}" 
                                 data-user-id="${user.id}"
                                 data-friendship-status="${user.friendshipStatus}"
                                 data-requested-by-me="${isRequestedByMe}"
                                 ${buttonDisabled ? 'disabled' : ''}
                                 title="${buttonTitle}"
                                 >
                             ${buttonContent}
                         </button>
                    </div>
                </div>
            `;
        }).join('');

        // Wire up add friend buttons
        wireAddFriendButtons();
    }

    function wireAddFriendButtons() {
        suggestedUsersList.querySelectorAll('.add-friend-btn').forEach(btn => {
            btn.addEventListener('click', async (e) => {
                e.stopPropagation();
                const userId = btn.getAttribute('data-user-id');
                const friendshipStatus = (btn.getAttribute('data-friendship-status') || '').toUpperCase();
                const requestedByMe = btn.getAttribute('data-requested-by-me') === 'true';

                // Kiểm tra nếu button đã bị disable
                if (btn.disabled) {
                    return;
                }
                
                // Kiểm tra userId hợp lệ
                if (!userId || isNaN(parseInt(userId))) {
                    showToast('ID người dùng không hợp lệ', 'error');
                    return;
                }
                
                // Điều hướng hành vi theo trạng thái kết bạn hiện tại
                try {
                    if (friendshipStatus === 'PENDING' && !requestedByMe) {
                        // Có lời mời đến từ người kia -> chấp nhận
                        btn.disabled = true;
                        const originalContent = btn.innerHTML;
                        btn.innerHTML = '<i class="fa fa-spinner fa-spin"></i>';
                        await acceptFriend(parseInt(userId));
                        // cập nhật UI sang bạn bè
                        updateButtonState(parseInt(userId), 'ACCEPTED', false);
                        showToast('Đã chấp nhận lời mời kết bạn', 'success');
                        return;
                    }
                } catch (err) {
                    btn.disabled = false;
                    return;
                }

                // Thêm loading state cho case gửi lời mời mới
                const originalContent = btn.innerHTML;
                btn.innerHTML = '<i class="fa fa-spinner fa-spin"></i>';
                btn.disabled = true;
                
                try {
                    await addFriend(parseInt(userId));
                } catch (error) {
                    // Restore button state nếu có lỗi
                    btn.innerHTML = originalContent;
                    btn.disabled = false;
                }
            });
        });

        // Wire up user item clicks to view profile
        suggestedUsersList.querySelectorAll('.suggested-user-item').forEach(item => {
            item.addEventListener('click', (event) => {
                // Kiểm tra nếu click vào button thì không chuyển profile
                if (event.target.closest('.add-friend-btn')) {
                    return;
                }
                
                // Lấy thông tin user từ data attributes
                const userInfo = getUserInfoFromElement(item);
                
                if (userInfo.username && userInfo.username.trim()) {
                    window.location.href = `/profile/${userInfo.username}`;
                } else {
                    console.warn('No username found in data-username attribute');
                }
            });
        });
    }

    async function addFriend(userId) {

        // Kiểm tra userId hợp lệ
        if (!userId || isNaN(userId)) {
            showToast('ID người dùng không hợp lệ', 'error');
            return;
        }
        
        try {
            // Sửa API endpoint để khớp với FriendshipController
            const response = await authenticatedFetch(`/api/friends/request/${userId}`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                }
            });

            
            if (response && response.ok) {
                // Update button state sử dụng helper function
                updateButtonState(userId, 'PENDING', true);
                
                // Show success message
                showToast('Đã gửi lời mời kết bạn!', 'success');
                
                // Refresh suggested users để cập nhật trạng thái mới nhất
                setTimeout(() => {
                    loadSuggestedUsers();
                }, 1000); // Delay 1 giây để user thấy thông báo thành công
            } else {
                // Lấy thông tin lỗi từ response
                let errorMessage = 'Có lỗi xảy ra khi gửi lời mời kết bạn.';
                try {
                    const errorData = await response.json();
                    if (errorData.message) errorMessage = errorData.message;
                    // Nếu backend trả trạng thái đã PENDING thì không coi là lỗi gây gián đoạn
                    if (/PENDING/i.test(errorMessage)) {
                        showToast('Đã gửi lời mời trước đó', 'info');
                        updateButtonState(userId, 'PENDING', true);
                        return;
                    }
                } catch (e) {
                    // Nếu không parse được JSON, sử dụng status text
                    if (response.statusText) {
                        errorMessage = `${errorMessage} (${response.status}: ${response.statusText})`;
                    }
                }
                showToast(errorMessage, 'error');
                
                // Re-throw error để button có thể restore state
                throw new Error(errorMessage);
            }
        } catch (error) {
            showToast('Có lỗi xảy ra khi gửi lời mời kết bạn.', 'error');
            throw error; // Re-throw để button có thể restore state
        }
    }

    // Chấp nhận lời mời kết bạn từ người khác
    async function acceptFriend(userId) {
        try {
            const response = await authenticatedFetch(`/api/friends/accept/${userId}`, { method: 'POST' });
            if (!response || !response.ok) {
                let msg = 'Không thể chấp nhận lời mời kết bạn';
                try {
                    const data = await response.json();
                    if (data.message) msg = data.message;
                } catch {}
                throw new Error(msg);
            }
            return true;
        } catch (err) {
            showToast(err.message || 'Lỗi chấp nhận lời mời', 'error');
            throw err;
        }
    }

    // Sử dụng ToastManager thay vì showToast local
    function showToast(message, type = 'info') {
        if (window.toastManager) {
            return window.toastManager.show(message, type);
        } else {
            // Fallback nếu ToastManager chưa load
            const toast = document.createElement('div');
            toast.className = `toast show bg-${type === 'success' ? 'success' : type === 'error' ? 'danger' : 'info'} text-white`;
            toast.innerHTML = `
                <div class="toast-body d-flex align-items-center">
                    <i class="fa fa-${type === 'success' ? 'check-circle' : type === 'error' ? 'exclamation-circle' : 'info-circle'} me-2"></i>
                    ${message}
                </div>
            `;
            document.body.appendChild(toast);
            setTimeout(() => toast.remove(), 3000);
        }
    }

    function escapeHtml(str) {
        return String(str).replace(/[&<>"]+/g, s => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;'}[s]));
    }
    
    // Helper function để lấy thông tin user từ data attributes
    function getUserInfoFromElement(element) {
        return {
            id: element.getAttribute('data-user-id'),
            username: element.getAttribute('data-username'),
            fullName: element.getAttribute('data-fullname'),
            department: element.getAttribute('data-department'),
            friendshipStatus: element.getAttribute('data-friendship-status'),
            requestedByMe: element.getAttribute('data-requested-by-me') === 'true'
        };
    }
    
    // Function để update button state sau khi gửi lời mời
    function updateButtonState(userId, newStatus = 'PENDING', requestedByMe = true) {
        const userItem = document.querySelector(`[data-user-id="${userId}"]`);
        const btn = userItem?.querySelector('.add-friend-btn');
        
        if (userItem && btn) {
            // Update user item data attributes
            userItem.setAttribute('data-friendship-status', newStatus);
            userItem.setAttribute('data-requested-by-me', requestedByMe.toString());
            
            // Update button data attributes
            btn.setAttribute('data-friendship-status', newStatus);
            btn.setAttribute('data-requested-by-me', requestedByMe.toString());
            
            // Update button appearance
            if (newStatus === 'PENDING' && requestedByMe) {
                btn.innerHTML = '<i class="fa fa-clock me-1"></i>Đã gửi';
                btn.classList.remove('btn-primary', 'btn-info');
                btn.classList.add('btn-secondary', 'opacity-75');
                btn.disabled = true;
                btn.title = 'Đã gửi lời mời kết bạn';
            } else if (newStatus === 'ACCEPTED') {
                btn.innerHTML = '<i class="fa fa-check"></i>';
                btn.classList.remove('btn-primary', 'btn-secondary', 'btn-info', 'opacity-75');
                btn.classList.add('btn-success');
                btn.disabled = true;
                btn.title = 'Đã là bạn bè';
            }
        }
    }

    // Expose functions globally
    window.addFriend = addFriend;
    window.loadSuggestedUsers = loadSuggestedUsers;
    window.getUserInfoFromElement = getUserInfoFromElement;
    window.updateButtonState = updateButtonState;
    window.acceptFriend = acceptFriend;
})();

