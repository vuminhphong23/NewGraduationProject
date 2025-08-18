/**
 * Post Modal Manager - Production Version
 * Quản lý modal đăng bài và chỉnh sửa bài viết
 */

class PostModal {
    constructor() {
        this.modal = document.getElementById('postModal');
        this.form = document.getElementById('postForm');
        this.titleInput = document.getElementById('postTitle');
        this.contentInput = document.getElementById('postContent');
        this.publishBtn = document.getElementById('publishBtn');
        this.currentPrivacy = 'PUBLIC';
        this.editingPostId = null;
        
        // Initialize if modal exists
        if (this.modal) {
            this.init();
        }
    }
    
    init() {
        this.bindEvents();
    }
    
    bindEvents() {
        // Form submission
        if (this.publishBtn) {
            this.publishBtn.addEventListener('click', (e) => {
                e.preventDefault();
                this.submitPost();
            });
        }
        
        // Modal events
        if (this.modal) {
            this.modal.addEventListener('hidden.bs.modal', () => {
                this.resetForm();
            });
        }
        
        // Form validation
        if (this.titleInput) {
            this.titleInput.addEventListener('input', () => this.validateForm());
        }
        if (this.contentInput) {
            this.contentInput.addEventListener('input', () => this.validateForm());
        }
        
        // Privacy dropdown
        this.bindPrivacyEvents();
        
        // Bind edit/delete buttons for existing posts
        this.bindPostActions();
    }
    
    bindPrivacyEvents() {
        const privacyItems = document.querySelectorAll('.privacy-option');
        privacyItems.forEach(item => {
            item.addEventListener('click', (e) => {
                e.preventDefault();
                const privacy = item.dataset.privacy;
                this.setPrivacy(privacy);
            });
        });
    }
    
    bindPostActions() {
        // Bind edit buttons
        document.addEventListener('click', (e) => {
            if (e.target.closest('.edit-post-btn')) {
                e.preventDefault();
                const postId = e.target.closest('.edit-post-btn').dataset.postId;
                this.editPost(postId);
            }
            
            if (e.target.closest('.delete-post-btn')) {
                e.preventDefault();
                const postId = e.target.closest('.delete-post-btn').dataset.postId;
                this.deletePost(postId);
            }
        });
    }
    
    validateForm() {
        if (!this.titleInput || !this.contentInput || !this.publishBtn) return;
        
        const title = this.titleInput.value.trim();
        const content = this.contentInput.value.trim();
        
        const isValid = title.length > 0 && content.length > 0;
        this.publishBtn.disabled = !isValid;
    }
    
    setPrivacy(privacy) {
        this.currentPrivacy = privacy;
        const privacyBtn = document.querySelector('.privacy-btn');
        if (privacyBtn) {
            const privacyConfig = {
                'PUBLIC': { text: 'Công khai', icon: 'fa-globe-asia' },
                'FRIENDS': { text: 'Bạn bè', icon: 'fa-users' },
                'PRIVATE': { text: 'Riêng tư', icon: 'fa-lock' }
            };
            
            const config = privacyConfig[privacy];
            if (config) {
                privacyBtn.innerHTML = `<i class="fa ${config.icon}"></i> ${config.text}`;
            }
        }
    }
    
    async submitPost() {
        if (this.publishBtn.disabled) return;
        
        // Get topic names from hashtag manager
        const topicNames = window.HashtagManager ? window.HashtagManager.getSelected() : [];
        
        const postData = {
            title: this.titleInput.value.trim(),
            content: this.contentInput.value.trim(),
            topicNames: topicNames,
            privacy: this.currentPrivacy
        };
        
        try {
            this.publishBtn.disabled = true;
            this.publishBtn.textContent = 'Đang xử lý...';
            
            let response;
            
            if (this.editingPostId) {
                // Update existing post
                response = await fetch(`/api/posts/${this.editingPostId}`, {
                    method: 'PUT',
                    headers: {
                        'Content-Type': 'application/json',
                        'Accept': 'application/json'
                    },
                    credentials: 'same-origin',
                    body: JSON.stringify(postData)
                });
            } else {
                // Create new post
                response = await fetch('/api/posts', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Accept': 'application/json'
                    },
                    credentials: 'same-origin',
                    body: JSON.stringify(postData)
                });
            }
            
            if (response.ok) {
                const result = await response.json();
                const message = this.editingPostId ? 'Cập nhật bài viết thành công!' : 'Đăng bài thành công!';
                this.showToast(message, 'success');
                
                this.closeModal();
                
                // Simple page reload after a delay to show toast
                setTimeout(() => {
                    window.location.reload();
                }, 1500);
            } else {
                const error = await response.json().catch(() => ({ message: 'Có lỗi xảy ra' }));
                this.showToast(error.message || 'Có lỗi xảy ra khi xử lý bài viết', 'error');
            }
        } catch (error) {
            console.error('Error submitting post:', error);
            this.showToast('Không thể kết nối đến máy chủ', 'error');
        } finally {
            this.publishBtn.disabled = false;
            this.publishBtn.textContent = this.editingPostId ? 'Cập nhật' : 'Đăng bài';
        }
    }
    
    async editPost(postId) {
        try {
            const response = await fetch(`/api/posts/${postId}`, {
                method: 'GET',
                headers: { 'Accept': 'application/json' },
                credentials: 'same-origin'
            });
            
            if (response.ok) {
                const post = await response.json();
                
                // Fill form with post data
                this.titleInput.value = post.title || '';
                this.contentInput.value = post.content || '';
                this.setPrivacy(post.privacy || 'PUBLIC');
                
                // Initialize hashtags
                if (window.HashtagManager) {
                    window.HashtagManager.setHashtags(post.topicNames || []);
                }
                
                // Set edit mode
                this.editingPostId = postId;
                this.publishBtn.textContent = 'Cập nhật';
                
                // Open modal
                const modal = new bootstrap.Modal(this.modal);
                modal.show();
                
                this.validateForm();
            } else {
                this.showToast('Không thể tải thông tin bài viết', 'error');
            }
        } catch (error) {
            console.error('Error loading post:', error);
            this.showToast('Có lỗi xảy ra khi tải bài viết', 'error');
        }
    }
    
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
                this.showToast('Đã xóa bài viết thành công!', 'success');
                
                // Simple page reload after a delay to show toast
                setTimeout(() => {
                    window.location.reload();
                }, 1500);
            } else {
                const error = await response.json().catch(() => ({ message: 'Không thể xóa bài viết' }));
                this.showToast(error.message, 'error');
            }
        } catch (error) {
            console.error('Error deleting post:', error);
            this.showToast('Có lỗi xảy ra khi xóa bài viết', 'error');
        }
    }
    
    resetForm() {
        if (this.form) {
            this.form.reset();
        }
        
        this.currentPrivacy = 'PUBLIC';
        this.setPrivacy('PUBLIC');
        this.editingPostId = null;
        this.publishBtn.textContent = 'Đăng bài';
        
        // Clear hashtags
        if (window.HashtagManager) {
            window.HashtagManager.reset();
        }
        
        this.validateForm();
    }
    
    closeModal() {
        const modal = bootstrap.Modal.getInstance(this.modal);
        if (modal) {
            modal.hide();
        }
    }
    
    async refreshFeed() {
        try {
            // Determine which endpoint to use
            let apiUrl = '/api/posts/feed';
            if (window.location.pathname.includes('/profile')) {
                apiUrl = '/api/posts/my-posts';
            }
            
            const response = await fetch(apiUrl, {
                method: 'GET',
                headers: { 'Accept': 'application/json' },
                credentials: 'same-origin'
            });
            
            if (response.ok) {
                const posts = await response.json();
                this.renderPosts(posts);
            } else {
                console.error('Failed to refresh feed:', response.status);
            }
        } catch (error) {
            console.error('Error refreshing feed:', error);
        }
    }
    
    renderPosts(posts) {
        // Find feed container
        let feedContainer = document.getElementById('feed') || document.querySelector('main.col-lg-8');
        
        if (!feedContainer) {
            console.warn('Feed container not found');
            return;
        }
        
        // Clear existing posts
        if (feedContainer.id === 'feed') {
            feedContainer.innerHTML = '';
        } else {
            // Profile page - keep form, remove only posts
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
        
        // Generate hashtag badges
        let hashtagBadges = '';
        if (post.topicNames && post.topicNames.length > 0) {
            hashtagBadges = post.topicNames.map(topicName => 
                `<span class="hashtag-link me-2" style="color: #1da1f2; font-weight: 500; cursor: pointer;">#${this.escapeHtml(topicName)}</span>`
            ).join('');
        }
        
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
                    <div class="post-hashtags">
                        ${hashtagBadges}
                    </div>
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
            <div class="card-body">
                ${titleHtml}
                <p class="card-text">${this.escapeHtml(post.content).replace(/\n/g, '<br>')}</p>
            </div>
            <div class="card-footer bg-white border-0">
                <div class="d-flex justify-content-between align-items-center">
                    <div class="d-flex gap-3">
                        <button class="btn btn-link text-muted p-0">
                            <i class="fa fa-heart"></i> ${post.likeCount || 0}
                        </button>
                        <button class="btn btn-link text-muted p-0">
                            <i class="fa fa-comment"></i> ${post.commentCount || 0}
                        </button>
                        <button class="btn btn-link text-muted p-0">
                            <i class="fa fa-share"></i> ${post.shareCount || 0}
                        </button>
                    </div>
                </div>
            </div>
        `;
        
        return postDiv;
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
    
    showToast(message, type = 'info') {
        // Create toast container if not exists
        let toastContainer = document.getElementById('toast-container');
        if (!toastContainer) {
            toastContainer = document.createElement('div');
            toastContainer.id = 'toast-container';
            toastContainer.className = 'toast-container position-fixed top-0 end-0 p-3';
            toastContainer.style.zIndex = '1055';
            document.body.appendChild(toastContainer);
        }
        
        const toastId = 'toast-' + Date.now();
        const bgClass = type === 'error' ? 'bg-danger' : type === 'warning' ? 'bg-warning' : type === 'success' ? 'bg-success' : 'bg-info';
        
        const toastHTML = `
            <div id="${toastId}" class="toast align-items-center text-white ${bgClass} border-0" role="alert" aria-live="assertive" aria-atomic="true">
                <div class="d-flex">
                    <div class="toast-body">
                        ${message}
                    </div>
                    <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast" aria-label="Close"></button>
                </div>
            </div>
        `;
        
        toastContainer.insertAdjacentHTML('beforeend', toastHTML);
        
        const toastElement = document.getElementById(toastId);
        const toast = new bootstrap.Toast(toastElement, { delay: 3000 });
        toast.show();
        
        // Remove after hiding
        toastElement.addEventListener('hidden.bs.toast', () => {
            toastElement.remove();
        });
    }
}

// Initialize when DOM is ready
document.addEventListener('DOMContentLoaded', () => {
    window.postModal = new PostModal();
});
