// Post Modal JavaScript
class PostModal {
    constructor() {
        this.currentPrivacy = 'PUBLIC';
        this.autoRefreshInterval = null;
        this.initializeElements();
        this.bindEvents();
        this.startAutoRefresh();
    }

    initializeElements() {
        this.modal = document.getElementById('postModal');
        this.form = document.getElementById('postForm');
        this.titleInput = document.getElementById('postTitle');
        this.contentInput = document.getElementById('postContent');
        this.topicSelect = document.getElementById('postTopic');
        this.privacyDropdown = document.getElementById('privacyDropdown');
        this.privacyText = document.getElementById('privacyText');
        this.publishBtn = document.getElementById('publishBtn');
        this.publishSpinner = document.getElementById('publishSpinner');
        this.publishText = document.getElementById('publishText');
        this.titleCount = document.getElementById('titleCount');
        this.contentCount = document.getElementById('contentCount');
        this.previewArea = document.getElementById('postPreview');
        this.previewContent = document.getElementById('previewContent');

        // Toast elements
        this.successToast = new bootstrap.Toast(document.getElementById('successToast'));
        this.errorToast = new bootstrap.Toast(document.getElementById('errorToast'));
        this.errorMessage = document.getElementById('errorMessage');
    }

    bindEvents() {
        // Privacy dropdown
        document.querySelectorAll('[data-privacy]').forEach(item => {
            item.addEventListener('click', (e) => {
                e.preventDefault();
                const privacy = e.currentTarget.dataset.privacy;
                this.setPrivacy(privacy);
            });
        });

        // Character counters
        this.titleInput.addEventListener('input', () => {
            this.updateCharacterCount(this.titleInput, this.titleCount, 100);
            this.validateForm();
        });

        this.contentInput.addEventListener('input', () => {
            this.updateCharacterCount(this.contentInput, this.contentCount, 2000);
            this.validateForm();
            this.updatePreview();
        });

        // Topic selection
        this.topicSelect.addEventListener('change', () => {
            this.validateForm();
        });

        // Publish button
        this.publishBtn.addEventListener('click', () => {
            this.publishPost();
        });


        // Modal events
        this.modal.addEventListener('hidden.bs.modal', () => {
            this.resetForm();
        });

        // Preview toggle
        this.contentInput.addEventListener('focus', () => {
            if (this.contentInput.value.trim()) {
                this.showPreview();
            }
        });
    }

    setPrivacy(privacy) {
        this.currentPrivacy = privacy;
        const privacyMap = {
            'PUBLIC': { text: 'Công khai', icon: 'fa-globe-asia', color: 'text-primary' },
            'FRIENDS': { text: 'Bạn bè', icon: 'fa-users', color: 'text-success' },
            'PRIVATE': { text: 'Riêng tư', icon: 'fa-lock', color: 'text-warning' }
        };

        const config = privacyMap[privacy];
        this.privacyText.textContent = config.text;

        // Update dropdown button
        const icon = this.privacyDropdown.querySelector('i');
        icon.className = `fa ${config.icon} ${config.color}`;
    }

    updateCharacterCount(input, counter, maxLength) {
        const count = input.value.length;
        counter.textContent = count;

        if (count > maxLength * 0.9) {
            counter.classList.add('text-warning');
        } else {
            counter.classList.remove('text-warning');
        }

        if (count > maxLength) {
            counter.classList.add('text-danger');
        } else {
            counter.classList.remove('text-danger');
        }
    }

    validateForm() {
        const title = this.titleInput.value.trim();
        const content = this.contentInput.value.trim();
        const topic = this.topicSelect.value;

        const isValid = title.length > 0 && content.length > 0 && topic !== '';
        this.publishBtn.disabled = !isValid;
    }

    updatePreview() {
        const content = this.contentInput.value.trim();
        if (content) {
            this.previewContent.innerHTML = this.formatContent(content);
            this.showPreview();
        } else {
            this.hidePreview();
        }
    }

    formatContent(content) {
        // Convert line breaks to <br> tags
        return content.replace(/\n/g, '<br>');
    }

    showPreview() {
        this.previewArea.classList.remove('d-none');
    }

    hidePreview() {
        this.previewArea.classList.add('d-none');
    }


    async publishPost() {
        if (this.publishBtn.disabled) return;

        const postData = {
            title: this.titleInput.value.trim(),
            content: this.contentInput.value.trim(),
            topicId: parseInt(this.topicSelect.value),
            privacy: this.currentPrivacy
        };

        this.setLoading(true);

        try {
            let response;

            if (this.editingPostId) {
                // Chế độ chỉnh sửa - PUT request
                response = await fetch(`/api/posts/${this.editingPostId}`, {
                    method: 'PUT',
                    headers: {
                        'Content-Type': 'application/json',
                        'Accept': 'application/json'
                    },
                    body: JSON.stringify(postData)
                });
            } else {
                // Chế độ tạo mới - POST request
                response = await fetch('/api/posts', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Accept': 'application/json'
                    },
                    body: JSON.stringify(postData)
                });
            }

            if (response.ok) {
                const result = await response.json();
                const message = this.editingPostId ? 'Cập nhật bài viết thành công!' : 'Đăng bài thành công!';
                this.showSuccessMessage(message);
                this.closeModal();
                this.refreshFeed();
            } else {
                const errorData = await response.json().catch(() => ({ message: 'Có lỗi xảy ra' }));
                this.showError(errorData.message || 'Có lỗi xảy ra khi xử lý bài viết');
            }
        } catch (error) {
            console.error('Error publishing post:', error);
            this.showError('Không thể kết nối đến máy chủ');
        } finally {
            this.setLoading(false);
        }
    }

    setLoading(loading) {
        this.publishBtn.disabled = loading;
        this.publishSpinner.classList.toggle('d-none', !loading);
        this.publishText.textContent = loading ? 'Đang đăng...' : 'Đăng';
    }

    showSuccess() {
        this.successToast.show();
    }

    showError(message) {
        this.errorMessage.textContent = message;
        this.errorToast.show();
    }

    closeModal() {
        const modal = bootstrap.Modal.getInstance(this.modal);
        modal.hide();
    }

    resetForm() {
        this.form.reset();
        this.currentPrivacy = 'PUBLIC';
        this.setPrivacy('PUBLIC');
        this.publishBtn.disabled = true;
        this.titleCount.textContent = '0';
        this.contentCount.textContent = '0';
        this.hidePreview();
        this.titleCount.classList.remove('text-warning', 'text-danger');
        this.contentCount.classList.remove('text-warning', 'text-danger');

        // Reset edit mode
        this.editingPostId = null;
        this.publishBtn.textContent = 'Đăng';
    }

    getPrivacyIcon(privacy) {
        const privacyMap = {
            'PUBLIC': '<i class="fa fa-globe-asia" title="Công khai"></i>',
            'FRIENDS': '<i class="fa fa-users" title="Bạn bè"></i>',
            'PRIVATE': '<i class="fa fa-lock" title="Riêng tư"></i>'
        };
        return privacyMap[privacy] || '<i class="fa fa-question" title="Không xác định"></i>';
    }

    escapeHtml(unsafe) {
        if (!unsafe) return '';
        return unsafe
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#039;");
    }

    async refreshFeed(showLogs = true) {
        try {
            if (showLogs) console.log('Refreshing feed...');

            // Sử dụng endpoint feed đã có
            let apiUrl = '/api/posts/feed';
            if (window.location.pathname.includes('/profile')) {
                apiUrl = '/api/posts/my-posts';
            }

            console.log('Making request to:', apiUrl);

            const response = await fetch(apiUrl, {
                method: 'GET',
                headers: {
                    'Accept': 'application/json'
                },
                credentials: 'same-origin'
            });

            console.log('API Response status:', response.status);

            if (response.ok) {
                const posts = await response.json();
                console.log('Received posts:', posts.length);
                this.updateFeedDisplay(posts);
                if (showLogs) console.log('Feed refreshed successfully');
            } else {
                console.error('Failed to refresh feed:', response.status);
            }
        } catch (error) {
            console.error('Error refreshing feed:', error);
        }
    }

    updateFeedDisplay(posts) {
        // Tìm feed container cho trang index hoặc profile
        let feedContainer = document.getElementById('feed') || document.querySelector('main.col-lg-8');

        if (!feedContainer) {
            console.warn('Feed container not found');
            return;
        }

        console.log('Updating feed display with', posts.length, 'posts');

        // Xóa posts cũ - khác nhau cho từng trang
        if (feedContainer.id === 'feed') {
            // Trang index: xóa hết nội dung
            feedContainer.innerHTML = '';
        } else {
            // Trang profile: chỉ xóa các post cards cũ, giữ lại form đăng bài
            const existingPosts = feedContainer.querySelectorAll('.card.mb-4.border-0.shadow-sm');
            existingPosts.forEach(post => post.remove());
        }

        if (!posts || posts.length === 0) {
            const emptyMessage = `
                <div class="text-center text-muted my-4">
                    Chưa có bài viết nào. Hãy là người đầu tiên đăng bài nhé!
                </div>
            `;
            feedContainer.insertAdjacentHTML('beforeend', emptyMessage);
            return;
        }

        // Render each post
        posts.forEach(post => {
            const postElement = this.createPostElement(post);
            feedContainer.appendChild(postElement);
        });
    }

    createPostElement(post) {
        const postDiv = document.createElement('div');
        postDiv.className = 'card mb-4 border-0 shadow-sm';
        postDiv.setAttribute('data-post-id', post.id);

        const createdAt = new Date(post.createdAt).toLocaleString('vi-VN', {
            day: '2-digit',
            month: '2-digit',
            year: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        });

        const privacyIcon = this.getPrivacyIcon(post.privacy);
        const titleHtml = post.title ? `<h5 class="card-title mb-2">${this.escapeHtml(post.title)}</h5>` : '';
        const topicBadge = post.topicName ? `<span class="badge bg-primary">${this.escapeHtml(post.topicName)}</span>` : '';

        postDiv.innerHTML = `
            <div class="card-header bg-white border-0 d-flex align-items-center justify-content-between">
                <div class="d-flex align-items-center gap-2">
                    <img src="https://randomuser.me/api/portraits/men/32.jpg"
                         class="rounded-circle" width="40" height="40" alt="avatar">
                    <div>
                        <div class="fw-semibold">${this.escapeHtml(post.userName || 'Unknown User')}</div>
                        <div class="text-muted small">
                            <span>${createdAt}</span>
                            • ${privacyIcon}
                        </div>
                    </div>
                </div>
                <div class="d-flex align-items-center gap-2">
                    ${topicBadge}
                    <div class="dropdown">
                        <button class="btn btn-sm btn-light" data-bs-toggle="dropdown">
                            <i class="fa fa-ellipsis-h"></i>
                        </button>
                        <ul class="dropdown-menu dropdown-menu-end">
                            <li><a class="dropdown-item edit-post-btn" href="#" data-post-id="${post.id}">Chỉnh sửa</a></li>
                            <li><a class="dropdown-item text-danger delete-post-btn" href="#" data-post-id="${post.id}">Xoá</a></li>
                        </ul>
                    </div>
                </div>
            </div>
            <div class="card-body pt-2">
                ${titleHtml}
                <p class="card-text" style="white-space: pre-wrap;">${this.escapeHtml(post.content)}</p>
            </div>
            <div class="card-footer bg-white border-0 pt-0">
                <hr class="my-2" style="color: #c3d2e0;">
                <div class="d-flex justify-content-around">
                    <button class="btn btn-light d-flex align-items-center gap-2 like-btn" data-post-id="${post.id}">
                        <i class="fa-regular fa-thumbs-up"></i> Thích
                    </button>
                    <button class="btn btn-light d-flex align-items-center gap-2 comment-btn" data-post-id="${post.id}">
                        <i class="fa-regular fa-comment"></i> Bình luận
                    </button>
                    <button class="btn btn-light d-flex align-items-center gap-2 share-btn" data-post-id="${post.id}">
                        <i class="fa-regular fa-share-from-square"></i> Chia sẻ
                    </button>
                </div>
            </div>
        `;

        this.bindPostEventHandlers(postDiv, post);
        return postDiv;
    }

    bindPostEventHandlers(postElement, post) {
        // Chỉnh sửa button
        const editBtn = postElement.querySelector('.edit-post-btn');
        if (editBtn) {
            editBtn.addEventListener('click', (e) => {
                e.preventDefault();
                this.editPost(post.id);
            });
        }

        // Xóa button
        const deleteBtn = postElement.querySelector('.delete-post-btn');
        if (deleteBtn) {
            deleteBtn.addEventListener('click', (e) => {
                e.preventDefault();
                this.deletePost(post.id);
            });
        }
    }

    // Chỉnh sửa post - mở modal với data cũ
    async editPost(postId) {
        try {
            // Lấy thông tin post hiện tại
            const response = await fetch(`/api/posts/${postId}`, {
                method: 'GET',
                headers: { 'Accept': 'application/json' },
                credentials: 'same-origin'
            });

            if (response.ok) {
                const post = await response.json();

                // Điền data vào modal
                this.titleInput.value = post.title || '';
                this.contentInput.value = post.content || '';
                this.topicSelect.value = post.topicId || '';
                this.setPrivacy(post.privacy || 'PUBLIC');

                // Set edit mode
                this.editingPostId = postId;
                this.publishBtn.textContent = 'Cập nhật';

                // Mở modal
                const modal = new bootstrap.Modal(this.modal);
                modal.show();

                this.validateForm();
            }
        } catch (error) {
            console.error('Error loading post:', error);
            this.showError('Không thể tải thông tin bài viết');
        }
    }

    // Xóa post
    async deletePost(postId) {
        if (!confirm('Bạn có chắc chắn muốn xóa bài viết này?')) {
            return;
        }

        try {
            const response = await fetch(`/api/posts/${postId}`, {
                method: 'DELETE',
                headers: { 'Accept': 'application/json' },
                credentials: 'same-origin'
            });

            if (response.ok) {
                this.showSuccessMessage('Đã xóa bài viết thành công!');
                this.refreshFeed(false);
            } else {
                const error = await response.json().catch(() => ({ message: 'Không thể xóa bài viết' }));
                this.showError(error.message);
            }
        } catch (error) {
            console.error('Error deleting post:', error);
            this.showError('Có lỗi xảy ra khi xóa bài viết');
        }
    }

    showSuccessMessage(message) {
        const toast = document.createElement('div');
        toast.className = 'toast align-items-center text-white bg-success border-0 position-fixed top-0 end-0 m-3';
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

        setTimeout(() => toast.remove(), 3000);
    }

    startAutoRefresh() {
        this.autoRefreshInterval = setInterval(() => {
            this.refreshFeed(false);
        }, 30000);
    }
}

// Initialize when DOM is loaded
document.addEventListener('DOMContentLoaded', function() {
    window.postModal = new PostModal();
});

// Global function để sử dụng trong HTML onclick
window.deletePost = function(element) {
    const postId = element.getAttribute('data-id');
    if (window.postModal && window.postModal.deletePost) {
        window.postModal.deletePost(postId);
    } else {
        console.error('PostModal not initialized');
    }
};

// Export for use in other scripts
if (typeof module !== 'undefined' && module.exports) {
    module.exports = PostModal;
}
