(() => {
    console.log('Profile friendship script loaded!');
    
    const friendshipActions = document.querySelector('.friendship-actions');
    if (!friendshipActions) {
        console.log('Friendship actions container not found');
        return;
    }
    
    const targetUserId = friendshipActions.getAttribute('data-user-id');
    if (!targetUserId) {
        console.log('Target user ID not found');
        return;
    }
    
    console.log('Target user ID:', targetUserId);
    
    // Load friendship status khi trang load
    loadFriendshipStatus();
    
    async function loadFriendshipStatus() {
        try {
            const response = await authenticatedFetch(`/api/friends/status/${targetUserId}`);
            if (!response || !response.ok) throw new Error('Failed to load friendship status');
            
            const data = await response.json();
            console.log('Friendship status:', data);
            renderFriendshipButtons(data);
        } catch (error) {
            console.error('Error loading friendship status:', error);
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
                        <button class="btn btn-danger fw-semibold px-4 py-2" onclick="cancelFriendRequest(${targetUserId})">
                            <i class="fa fa-times me-2"></i>Hủy yêu cầu
                        </button>
                    `;
                } else {
                    // Chấp nhận (xanh lá) / Từ chối (đỏ)
                    buttonHtml = `
                        <div class="btn-group" role="group">
                            <button class="btn btn-success fw-semibold px-3 py-2" onclick="acceptFriendRequest(${targetUserId})">
                                <i class="fa fa-check me-2"></i>Chấp nhận
                            </button>
                            <button class="btn btn-danger fw-semibold px-3 py-2" onclick="declineFriendRequest(${targetUserId})">
                                <i class="fa fa-times me-2"></i>Từ chối
                            </button>
                        </div>
                    `;
                }
                break;
                
            case 'ACCEPTED':
                // Hủy kết bạn (màu đỏ)
                buttonHtml = `
                    <button class="btn btn-danger fw-semibold px-4 py-2" onclick="unfriend(${targetUserId})">
                        <i class="fa fa-user-minus me-2"></i>Hủy kết bạn
                    </button>
                `;
                break;
                
            case 'BLOCKED':
                buttonHtml = `
                    <button class="btn btn-warning fw-semibold px-4 py-2" disabled>
                        <i class="fa fa-ban me-2"></i>Đã chặn
                    </button>
                `;
                break;
                
            default:
                buttonHtml = `
                    <button class="btn btn-primary fw-semibold px-4 py-2" onclick="sendFriendRequest(${targetUserId})">
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
