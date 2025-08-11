// Global post actions for compatibility with existing HTML
// These functions work with both dynamically created and server-rendered posts

// Global delete function (for onclick compatibility)
function deletePost(element) {
    const postId = element.getAttribute('data-id') || element.dataset.id;
    if (!postId) {
        console.error('Post ID not found');
        return;
    }

    if (window.postModal) {
        window.postModal.deletePost(postId);
    } else {
        // Fallback if PostModal not available
        deletePostDirect(postId);
    }
}

// Direct delete function
async function deletePostDirect(postId) {
    if (!confirm('Bạn có chắc chắn muốn xóa bài viết này?')) {
        return;
    }

    try {
        // Get CSRF token if available
        const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
        const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');

        const headers = {
            'Accept': 'application/json',
            'Content-Type': 'application/json'
        };

        // Add CSRF token if available
        if (csrfToken && csrfHeader) {
            headers[csrfHeader] = csrfToken;
        }

        console.log('Deleting post ID:', postId);
        const response = await fetch(`http://localhost:8080/api/posts/${postId}`, {
            method: 'DELETE',
            headers: headers,
            credentials: 'same-origin'
        });

        console.log('Delete response status:', response.status);

        if (response.ok) {
            showSuccessToast('Đã xóa bài viết thành công!');

            // Immediately refresh the feed to show updated list
            if (window.postModal && typeof window.postModal.refreshFeed === 'function') {
                // Refresh feed immediately
                window.postModal.refreshFeed(false);
            } else {
                // Fallback: reload page if PostModal not available
                window.location.reload();
            }

        } else {
            let errorMsg = 'Không thể xóa bài viết';
            try {
                const error = await response.json();
                errorMsg = error.message || errorMsg;
            } catch (e) {
                const errorText = await response.text();
                errorMsg = errorText || errorMsg;
            }
            console.error('Delete error:', errorMsg);
            showErrorToast(errorMsg);
        }
    } catch (error) {
        console.error('Error deleting post:', error);
        showErrorToast('Có lỗi xảy ra khi xóa bài viết: ' + error.message);
    }
}

// Global edit function - Open modal instead of redirect
function editPost(element) {
    const postId = element.getAttribute('data-id') || element.dataset.id || element.dataset.postId;
    if (!postId) {
        console.error('Post ID not found');
        return;
    }

    // Load post data and open edit modal
    loadPostForEdit(postId);
}

// Load post data and populate edit modal
async function loadPostForEdit(postId) {
    try {
        console.log('Loading post for edit:', postId);
        const response = await fetch(`http://localhost:8080/api/posts/${postId}`, {
            method: 'GET',
            headers: {
                'Accept': 'application/json'
            },
            credentials: 'same-origin'
        });

        if (response.ok) {
            const post = await response.json();
            openEditModal(post);
        } else {
            console.error('Failed to load post:', response.status);
            showErrorToast('Không thể tải bài viết để chỉnh sửa');
        }
    } catch (error) {
        console.error('Error loading post:', error);
        showErrorToast('Có lỗi xảy ra khi tải bài viết');
    }
}

// Open edit modal with post data
function openEditModal(post) {
    // Check if modal exists, if not create it
    let editModal = document.getElementById('editPostModal');
    if (!editModal) {
        createEditModal();
        editModal = document.getElementById('editPostModal');
    }

    // Populate modal with post data
    document.getElementById('editPostId').value = post.id;
    document.getElementById('editPostTitle').value = post.title || '';
    document.getElementById('editPostContent').value = post.content || '';
    document.getElementById('editPostTopic').value = post.topicId || '';

    // Set privacy
    if (window.editPostModal) {
        window.editPostModal.setPrivacy(post.privacy || 'PUBLIC');
    }

    // Show modal
    const modal = new bootstrap.Modal(editModal);
    modal.show();
}

// Create edit modal HTML
function createEditModal() {
    const modalHtml = `
    <div class="modal fade" id="editPostModal" tabindex="-1" aria-labelledby="editPostModalLabel" aria-hidden="true">
        <div class="modal-dialog modal-dialog-centered modal-lg">
            <div class="modal-content rounded-4 shadow border-0">
                <div class="modal-header border-0 pb-0">
                    <h5 class="modal-title fw-bold mx-auto" id="editPostModalLabel">Chỉnh sửa bài viết</h5>
                </div>
                <div class="modal-body">
                    <form id="editPostForm">
                        <input type="hidden" id="editPostId">
                        
                        <div class="mb-3">
                            <input type="text" class="form-control border-0 fs-5 fw-semibold" 
                                   id="editPostTitle" placeholder="Tiêu đề bài viết..." maxlength="100">
                        </div>

                        <div class="mb-3">
                            <textarea class="form-control border-0 fs-6" id="editPostContent" 
                                      rows="6" placeholder="Nội dung bài viết..." 
                                      style="resize:none; background:transparent;" maxlength="2000"></textarea>
                        </div>

                        <div class="mb-3">
                            <label class="form-label fw-semibold">Chủ đề</label>
                            <select class="form-select" id="editPostTopic" required>
                                <option value="">Chọn chủ đề...</option>
                            </select>
                        </div>

                        <div class="mb-3">
                            <label class="form-label fw-semibold">Quyền riêng tư</label>
                            <div class="dropdown">
                                <button class="btn btn-light dropdown-toggle d-flex align-items-center gap-2" 
                                        type="button" id="editPrivacyDropdown" data-bs-toggle="dropdown">
                                    <i class="fa fa-globe-asia text-primary"></i>
                                    <span id="editPrivacyText">Công khai</span>
                                </button>
                                <ul class="dropdown-menu">
                                    <li><a class="dropdown-item d-flex align-items-center gap-2" href="#" data-privacy="PUBLIC">
                                        <i class="fa fa-globe-asia text-primary"></i>
                                        <span>Công khai</span>
                                    </a></li>
                                    <li><a class="dropdown-item d-flex align-items-center gap-2" href="#" data-privacy="FRIENDS">
                                        <i class="fa fa-users text-success"></i>
                                        <span>Bạn bè</span>
                                    </a></li>
                                    <li><a class="dropdown-item d-flex align-items-center gap-2" href="#" data-privacy="PRIVATE">
                                        <i class="fa fa-lock text-warning"></i>
                                        <span>Riêng tư</span>
                                    </a></li>
                                </ul>
                            </div>
                        </div>
                    </form>
                </div>
                <div class="modal-footer border-0">
                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Hủy</button>
                    <button type="button" class="btn btn-primary" id="saveEditBtn">
                        <span class="spinner-border spinner-border-sm me-2 d-none" id="editSpinner"></span>
                        <span id="editText">Lưu thay đổi</span>
                    </button>
                </div>
            </div>
        </div>
    </div>`;

    document.body.insertAdjacentHTML('beforeend', modalHtml);

    // Initialize edit modal functionality
    initializeEditModal();
}

// Initialize edit modal functionality
function initializeEditModal() {
    // Load topics for dropdown
    loadTopicsForEdit();

    // Bind events
    document.getElementById('saveEditBtn').addEventListener('click', saveEditPost);

    // Privacy dropdown events
    document.querySelectorAll('#editPostModal [data-privacy]').forEach(item => {
        item.addEventListener('click', (e) => {
            e.preventDefault();
            const privacy = e.currentTarget.dataset.privacy;
            setEditPrivacy(privacy);
        });
    });

    // Create edit modal object
    window.editPostModal = {
        currentPrivacy: 'PUBLIC',
        setPrivacy: setEditPrivacy
    };
}

// Load topics for edit modal
async function loadTopicsForEdit() {
    try {
        const response = await fetch('http://localhost:8080/api/topics', {
            method: 'GET',
            headers: { 'Accept': 'application/json' },
            credentials: 'same-origin'
        });

        if (response.ok) {
            const topics = await response.json();
            const select = document.getElementById('editPostTopic');
            select.innerHTML = '<option value="">Chọn chủ đề...</option>';

            topics.forEach(topic => {
                const option = document.createElement('option');
                option.value = topic.id;
                option.textContent = topic.name;
                select.appendChild(option);
            });
        }
    } catch (error) {
        console.error('Error loading topics:', error);
    }
}

// Set privacy for edit modal
function setEditPrivacy(privacy) {
    if (!window.editPostModal) return;

    window.editPostModal.currentPrivacy = privacy;
    const privacyMap = {
        'PUBLIC': { text: 'Công khai', icon: 'fa-globe-asia', color: 'text-primary' },
        'FRIENDS': { text: 'Bạn bè', icon: 'fa-users', color: 'text-success' },
        'PRIVATE': { text: 'Riêng tư', icon: 'fa-lock', color: 'text-warning' }
    };

    const config = privacyMap[privacy];
    document.getElementById('editPrivacyText').textContent = config.text;

    // Update dropdown button
    const icon = document.querySelector('#editPrivacyDropdown i');
    if (icon) {
        icon.className = `fa ${config.icon} ${config.color}`;
    }
}

// Save edited post
async function saveEditPost() {
    const postId = document.getElementById('editPostId').value;
    const title = document.getElementById('editPostTitle').value.trim();
    const content = document.getElementById('editPostContent').value.trim();
    const topicId = document.getElementById('editPostTopic').value;

    if (!title || !content || !topicId) {
        showErrorToast('Vui lòng điền đầy đủ thông tin');
        return;
    }

    const saveBtn = document.getElementById('saveEditBtn');
    const spinner = document.getElementById('editSpinner');
    const text = document.getElementById('editText');

    // Show loading
    saveBtn.disabled = true;
    spinner.classList.remove('d-none');
    text.textContent = 'Đang lưu...';

    try {
        const updateData = {
            title: title,
            content: content,
            topicId: parseInt(topicId),
            privacy: window.editPostModal.currentPrivacy
        };

        const response = await fetch(`http://localhost:8080/api/posts/${postId}`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            },
            body: JSON.stringify(updateData),
            credentials: 'same-origin'
        });

        if (response.ok) {
            showSuccessToast('Đã cập nhật bài viết thành công!');

            // Close modal
            const modal = bootstrap.Modal.getInstance(document.getElementById('editPostModal'));
            modal.hide();

            // Refresh feed
            setTimeout(() => {
                if (window.postModal && typeof window.postModal.refreshFeed === 'function') {
                    window.postModal.refreshFeed(false);
                } else {
                    window.location.reload();
                }
            }, 500);

        } else {
            const error = await response.json();
            showErrorToast(error.message || 'Không thể cập nhật bài viết');
        }
    } catch (error) {
        console.error('Error updating post:', error);
        showErrorToast('Có lỗi xảy ra khi cập nhật bài viết');
    } finally {
        // Hide loading
        saveBtn.disabled = false;
        spinner.classList.add('d-none');
        text.textContent = 'Lưu thay đổi';
    }
}

// Like function
async function toggleLike(element) {
    const postId = element.getAttribute('data-id') || element.dataset.id || element.dataset.postId;
    if (!postId) {
        console.error('Post ID not found');
        return;
    }

    try {
        const response = await fetch(`http://localhost:8080/api/posts/${postId}/like`, {
            method: 'POST',
            headers: {
                'Accept': 'application/json'
            },
            credentials: 'same-origin'
        });

        if (response.ok) {
            // Update like button UI
            const likeBtn = element.closest('.like-btn') || element;
            const icon = likeBtn.querySelector('i');
            const isLiked = icon.classList.contains('fa-solid');

            if (isLiked) {
                icon.classList.remove('fa-solid');
                icon.classList.add('fa-regular');
                likeBtn.classList.remove('text-primary');
            } else {
                icon.classList.remove('fa-regular');
                icon.classList.add('fa-solid');
                likeBtn.classList.add('text-primary');
            }
        }
    } catch (error) {
        console.error('Error toggling like:', error);
    }
}

// Share function
function sharePost(element) {
    const postId = element.getAttribute('data-id') || element.dataset.id || element.dataset.postId;
    if (!postId) {
        console.error('Post ID not found');
        return;
    }

    if (navigator.share) {
        navigator.share({
            title: 'Bài viết từ Forumikaa',
            url: window.location.origin + `http://localhost:8080/posts/${postId}`
        });
    } else {
        // Fallback: Copy to clipboard
        const url = window.location.origin + `/posts/${postId}`;
        navigator.clipboard.writeText(url).then(() => {
            showSuccessToast('Đã copy link bài viết!');
        }).catch(() => {
            // Fallback for older browsers
            const textArea = document.createElement('textarea');
            textArea.value = url;
            document.body.appendChild(textArea);
            textArea.select();
            document.execCommand('copy');
            document.body.removeChild(textArea);
            showSuccessToast('Đã copy link bài viết!');
        });
    }
}

// Utility functions for toasts
function showSuccessToast(message) {
    showToast(message, 'success');
}

function showErrorToast(message) {
    showToast(message, 'danger');
}

function showToast(message, type = 'success') {
    const toast = document.createElement('div');
    toast.className = `toast align-items-center text-white bg-${type} border-0 position-fixed top-0 end-0 m-3`;
    toast.style.zIndex = '9999';
    toast.innerHTML = `
        <div class="d-flex">
            <div class="toast-body">${message}</div>
            <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button>
        </div>
    `;

    document.body.appendChild(toast);
    const bsToast = new bootstrap.Toast(toast);
    bsToast.show();

    // Remove after 3 seconds
    setTimeout(() => {
        if (toast.parentNode) {
            toast.remove();
        }
    }, 3000);
}

// Initialize event listeners for existing posts on page load
document.addEventListener('DOMContentLoaded', function() {
    // Bind events to existing server-rendered posts
    bindExistingPostEvents();
});

function bindExistingPostEvents() {
    // Delete buttons
    document.querySelectorAll('[onclick*="deletePost"]').forEach(btn => {
        btn.removeAttribute('onclick');
        btn.addEventListener('click', function(e) {
            e.preventDefault();
            deletePost(this);
        });
    });

    // Edit buttons
    document.querySelectorAll('a[href*="/posts/edit/"]').forEach(btn => {
        btn.addEventListener('click', function(e) {
            if (this.classList.contains('edit-post-btn')) {
                e.preventDefault();
                editPost(this);
            }
        });
    });

    // Like buttons
    document.querySelectorAll('.like-btn, [data-action="like"]').forEach(btn => {
        btn.addEventListener('click', function(e) {
            e.preventDefault();
            toggleLike(this);
        });
    });

    // Share buttons
    document.querySelectorAll('.share-btn, [data-action="share"]').forEach(btn => {
        btn.addEventListener('click', function(e) {
            e.preventDefault();
            sharePost(this);
        });
    });
}

// Refresh posts function (can be called from anywhere)
async function refreshPosts() {
    if (window.postModal && typeof window.postModal.refreshFeed === 'function') {
        await window.postModal.refreshFeed(true);
    } else {
        // Fallback: reload page
        window.location.reload();
    }
}

// Export functions for global use
window.deletePost = deletePost;
window.editPost = editPost;
window.toggleLike = toggleLike;
window.sharePost = sharePost;
window.refreshPosts = refreshPosts;
window.showSuccessToast = showSuccessToast;
window.showErrorToast = showErrorToast;
