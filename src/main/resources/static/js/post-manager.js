/**
 * Post Manager - Unified Post Management System
 * Quản lý tất cả chức năng liên quan đến bài viết
 */

class PostManager {
    constructor() {
        this.modal = document.getElementById('postModal');
        this.form = document.getElementById('postForm');
        this.titleInput = document.getElementById('postTitle');
        this.contentInput = document.getElementById('postContent');
        this.publishBtn = document.getElementById('publishBtn');
        this.currentPrivacy = 'PUBLIC';
        this.editingPostId = null;
        
        if (this.modal) this.init();
    }
    
    init() {
        this.bindEvents();
        this.bindExistingPosts();
    }
    
    bindEvents() {
        // Form events
        this.publishBtn?.addEventListener('click', (e) => {
            e.preventDefault();
            this.submitPost();
        });
        
        this.modal?.addEventListener('hidden.bs.modal', () => this.resetForm());
        this.titleInput?.addEventListener('input', () => this.validateForm());
        this.contentInput?.addEventListener('input', () => this.validateForm());
        
        // Privacy dropdown
        document.querySelectorAll('.privacy-option').forEach(item => {
            item.addEventListener('click', (e) => {
                e.preventDefault();
                this.setPrivacy(item.dataset.privacy);
            });
        });
        
        // Global post actions
        document.addEventListener('click', (e) => {
            // Support multiple selectors for delete buttons
            const target = e.target.closest('.edit-post-btn, .delete-post-btn, .like-btn, .share-btn') ||
                          e.target.closest('a[onclick*="deletePost"]') ||
                          e.target.closest('a[onclick*="editPost"]') ||
                          e.target.closest('.dropdown-item.text-danger');
            
            if (!target) return;
            
            e.preventDefault();
            e.stopPropagation(); // Prevent event bubbling
            
            const postId = this.getPostId(target);
            console.log('Post action triggered:', target.className, 'postId:', postId); // Debug log
            
            if (!postId) {
                console.error('No post ID found for element:', target);
                this.showToast('Không tìm thấy ID bài viết', 'error');
                return;
            }
            
            // Determine action based on class or onclick
            if (target.classList.contains('edit-post-btn') || target.getAttribute('onclick')?.includes('editPost')) {
                this.editPost(postId);
            } else if (target.classList.contains('delete-post-btn') || 
                      target.classList.contains('text-danger') || 
                      target.getAttribute('onclick')?.includes('deletePost') ||
                      target.textContent.trim() === 'Xoá') {
                this.deletePost(postId);
            } else if (target.classList.contains('like-btn')) {
                this.toggleLike(postId);
            } else if (target.classList.contains('share-btn')) {
                this.sharePost(postId);
            }
        });
    }
    
    getPostId(element) {
        // Try multiple ways to get post ID
        const postId = element.getAttribute('data-id') || 
                      element.dataset.id || 
                      element.dataset.postId ||
                      element.closest('[data-post-id]')?.dataset.postId ||
                      element.closest('[data-id]')?.dataset.id ||
                      element.closest('.card')?.dataset.postId;
        
        console.log('Getting post ID from element:', element, 'Found ID:', postId); // Debug log
        return postId;
    }
    
    validateForm() {
        if (!this.titleInput || !this.contentInput || !this.publishBtn) return;
        const isValid = this.titleInput.value.trim() && this.contentInput.value.trim();
        this.publishBtn.disabled = !isValid;
    }
    
    setPrivacy(privacy) {
        this.currentPrivacy = privacy;
        const privacyBtn = document.querySelector('.privacy-btn');
        if (!privacyBtn) return;
        
        const config = {
            'PUBLIC': { text: 'Công khai', icon: 'fa-globe-asia' },
            'FRIENDS': { text: 'Bạn bè', icon: 'fa-users' },
            'PRIVATE': { text: 'Riêng tư', icon: 'fa-lock' }
        }[privacy];
        
        if (config) {
            privacyBtn.innerHTML = `<i class="fa ${config.icon}"></i> ${config.text}`;
        }
    }
    
    async submitPost() {
        if (this.publishBtn.disabled) return;
        
        // Get selected topics but don't save them yet
        const selectedTopics = window.HashtagManager?.getSelected() || [];
        
        const postData = {
            title: this.titleInput.value.trim(),
            content: this.contentInput.value.trim(),
            topicNames: selectedTopics,
            privacy: this.currentPrivacy
        };
        
        const url = this.editingPostId ? `/api/posts/${this.editingPostId}` : '/api/posts';
        const method = this.editingPostId ? 'PUT' : 'POST';
        
        try {
            this.setLoading(true);
            
            const response = await fetch(url, {
                method,
                headers: { 
                    'Content-Type': 'application/json',
                    'Accept': 'application/json'
                },
                credentials: 'same-origin',
                body: JSON.stringify(postData)
            });
            
            if (response.ok) {
                const result = await response.json();
                
                // Only save topics to HashtagManager after successful post creation
                if (selectedTopics.length > 0 && window.HashtagManager && typeof window.HashtagManager.saveTopics === 'function') {
                    try {
                        await window.HashtagManager.saveTopics(selectedTopics);
                        console.log('Topics saved successfully after post creation');
                    } catch (topicError) {
                        console.warn('Failed to save topics:', topicError);
                        // Don't show error to user since post was successful
                    }
                }
                
                const message = this.editingPostId ? 'Cập nhật thành công!' : 'Đăng bài thành công!';
                this.showToast(message, 'success');
                this.closeModal();
                setTimeout(() => window.location.reload(), 1000);
            } else {
                const error = await response.json().catch(() => ({ message: 'Có lỗi xảy ra' }));
                this.showToast(error.message || 'Có lỗi xảy ra khi xử lý bài viết', 'error');
            }
        } catch (error) {
            console.error('Submit error:', error);
            this.showToast('Không thể kết nối đến máy chủ', 'error');
        } finally {
            this.setLoading(false);
        }
    }
    
    setLoading(loading) {
        this.publishBtn.disabled = loading;
        this.publishBtn.textContent = loading ? 'Đang xử lý...' : 
            (this.editingPostId ? 'Cập nhật' : 'Đăng bài');
    }
    
    async editPost(postId) {
        try {
            const response = await fetch(`/api/posts/${postId}`, {
                headers: { 'Accept': 'application/json' },
                credentials: 'same-origin'
            });
            
            if (response.ok) {
                const post = await response.json();
                
                this.titleInput.value = post.title || '';
                this.contentInput.value = post.content || '';
                this.setPrivacy(post.privacy || 'PUBLIC');
                
                if (window.HashtagManager) {
                    window.HashtagManager.setHashtags(post.topicNames || []);
                }
                
                this.editingPostId = postId;
                this.publishBtn.textContent = 'Cập nhật';
                
                new bootstrap.Modal(this.modal).show();
                this.validateForm();
            } else {
                this.showToast('Không thể tải thông tin bài viết', 'error');
            }
        } catch (error) {
            this.showToast('Có lỗi xảy ra khi tải bài viết', 'error');
        }
    }
    
    async deletePost(postId) {
        if (!confirm('Bạn có chắc chắn muốn xóa bài viết này?')) return;
        
        try {
            const headers = { 
                'Accept': 'application/json',
                'Content-Type': 'application/json'
            };
            
            // Add CSRF token if available
            const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
            const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;
            if (csrfToken && csrfHeader) {
                headers[csrfHeader] = csrfToken;
            }
            
            console.log('Deleting post:', postId); // Debug log
            
            const response = await fetch(`/api/posts/${postId}`, {
                method: 'DELETE',
                headers,
                credentials: 'same-origin'
            });
            
            console.log('Delete response status:', response.status); // Debug log
            
            if (response.ok || response.status === 204) {
                this.showToast('Đã xóa bài viết thành công!', 'success');
                
                // Remove post element immediately for better UX
                const postElement = document.querySelector(`[data-post-id="${postId}"]`);
                if (postElement) {
                    postElement.style.opacity = '0.5';
                    setTimeout(() => {
                        postElement.remove();
                    }, 500);
                }
                
                // Reload after a delay
                setTimeout(() => window.location.reload(), 1500);
            } else {
                const errorText = await response.text();
                console.error('Delete error response:', errorText); // Debug log
                
                let errorMessage = 'Không thể xóa bài viết';
                try {
                    const errorJson = JSON.parse(errorText);
                    errorMessage = errorJson.message || errorMessage;
                } catch (e) {
                    if (errorText) errorMessage = errorText;
                }
                
                this.showToast(errorMessage, 'error');
            }
        } catch (error) {
            console.error('Delete error:', error); // Debug log
            this.showToast('Có lỗi xảy ra khi xóa bài viết: ' + error.message, 'error');
        }
    }
    
    async toggleLike(postId) {
        try {
            const response = await fetch(`/api/posts/${postId}/like`, {
                method: 'POST',
                headers: { 'Accept': 'application/json' },
                credentials: 'same-origin'
            });

            if (response.ok) {
                const likeBtn = document.querySelector(`[data-post-id="${postId}"] .like-btn`);
                if (likeBtn) {
                    const icon = likeBtn.querySelector('i');
                    const isLiked = icon.classList.contains('fa-solid');
                    
                    icon.classList.toggle('fa-solid', !isLiked);
                    icon.classList.toggle('fa-regular', isLiked);
                    likeBtn.classList.toggle('text-primary', !isLiked);
                }
            }
        } catch (error) {
            this.showToast('Có lỗi xảy ra khi thích bài viết', 'error');
        }
    }
    
    sharePost(postId) {
        const url = `${window.location.origin}/posts/${postId}`;
        
        if (navigator.share) {
            navigator.share({
                title: 'Bài viết từ Forumikaa',
                url: url
            });
        } else {
            navigator.clipboard.writeText(url).then(() => {
                this.showToast('Đã copy link bài viết!', 'success');
            }).catch(() => {
                // Fallback
                const textArea = document.createElement('textarea');
                textArea.value = url;
                document.body.appendChild(textArea);
                textArea.select();
                document.execCommand('copy');
                document.body.removeChild(textArea);
                this.showToast('Đã copy link bài viết!', 'success');
            });
        }
    }
    
    resetForm() {
        this.form?.reset();
        this.currentPrivacy = 'PUBLIC';
        this.setPrivacy('PUBLIC');
        this.editingPostId = null;
        this.publishBtn.textContent = 'Đăng bài';
        
        // Reset hashtag selection but don't save to server
        if (window.HashtagManager) {
            window.HashtagManager.reset();
            console.log('Form reset - topics cleared from UI only, not saved to server');
        }
        
        this.validateForm();
    }
    
    closeModal() {
        bootstrap.Modal.getInstance(this.modal)?.hide();
        // resetForm will be called automatically by modal hidden event
    }
    
    showToast(message, type = 'info') {
        let container = document.getElementById('toast-container');
        if (!container) {
            container = document.createElement('div');
            container.id = 'toast-container';
            container.className = 'toast-container position-fixed top-0 end-0 p-3';
            container.style.zIndex = '1055';
            document.body.appendChild(container);
        }
        
        const toastId = 'toast-' + Date.now();
        const bgClass = {
            'error': 'bg-danger',
            'success': 'bg-success', 
            'warning': 'bg-warning'
        }[type] || 'bg-info';
        
        container.insertAdjacentHTML('beforeend', `
            <div id="${toastId}" class="toast align-items-center text-white ${bgClass} border-0">
                <div class="d-flex">
                    <div class="toast-body">${message}</div>
                    <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button>
                </div>
            </div>
        `);
        
        const toastElement = document.getElementById(toastId);
        const toast = new bootstrap.Toast(toastElement, { delay: 3000 });
        toast.show();
        
        toastElement.addEventListener('hidden.bs.toast', () => toastElement.remove());
    }
    
    bindExistingPosts() {
        console.log('Binding existing posts...'); // Debug log
        
        // Find all delete buttons and log them
        const deleteButtons = document.querySelectorAll('[onclick*="deletePost"], .delete-post-btn, .dropdown-item.text-danger');
        console.log('Found delete buttons:', deleteButtons.length, deleteButtons); // Debug log
        
        // Remove old onclick handlers and add new event listeners
        document.querySelectorAll('[onclick*="deletePost"]').forEach(btn => {
            console.log('Processing delete button:', btn); // Debug log
            btn.removeAttribute('onclick');
            
            // Add direct event listener as backup
            btn.addEventListener('click', (e) => {
                e.preventDefault();
                e.stopPropagation();
                console.log('Direct delete click handler triggered'); // Debug log
                const postId = this.getPostId(btn);
                if (postId) {
                    this.deletePost(postId);
                } else {
                    console.error('No post ID found for delete button:', btn);
                }
            });
        });
        
        // Do the same for edit buttons
        document.querySelectorAll('[onclick*="editPost"]').forEach(btn => {
            btn.removeAttribute('onclick');
            btn.addEventListener('click', (e) => {
                e.preventDefault();
                e.stopPropagation();
                const postId = this.getPostId(btn);
                if (postId) this.editPost(postId);
            });
        });
        
        console.log('Existing posts bound successfully');
    }
}

// ===========================
// GLOBAL FUNCTIONS (for compatibility)
// ===========================

window.deletePost = (element) => {
    console.log('Global deletePost called with:', element); // Debug log
    const postId = window.postManager?.getPostId(element);
    console.log('Extracted post ID:', postId); // Debug log
    
    if (postId && window.postManager) {
        window.postManager.deletePost(postId);
    } else {
        console.error('PostManager not available or no post ID found');
        alert('Không thể xóa bài viết. Vui lòng tải lại trang.');
    }
};

window.editPost = (element) => {
    console.log('Global editPost called with:', element); // Debug log
    const postId = window.postManager?.getPostId(element);
    if (postId && window.postManager) {
        window.postManager.editPost(postId);
    } else {
        console.error('PostManager not available or no post ID found');
        alert('Không thể chỉnh sửa bài viết. Vui lòng tải lại trang.');
    }
};

window.toggleLike = (element) => {
    const postId = window.postManager?.getPostId(element);
    if (postId && window.postManager) {
        window.postManager.toggleLike(postId);
    }
};

window.sharePost = (element) => {
    const postId = window.postManager?.getPostId(element);
    if (postId && window.postManager) {
        window.postManager.sharePost(postId);
    }
};

window.refreshPosts = () => window.location.reload();

window.showSuccessToast = (msg) => window.postManager?.showToast(msg, 'success');
window.showErrorToast = (msg) => window.postManager?.showToast(msg, 'error');

// Initialize
document.addEventListener('DOMContentLoaded', () => {
    console.log('Initializing PostManager...'); // Debug log
    window.postManager = new PostManager();
    console.log('PostManager initialized:', window.postManager); // Debug log
    
    // Test if buttons exist
    const deleteButtons = document.querySelectorAll('[onclick*="deletePost"], .delete-post-btn, .dropdown-item.text-danger');
    console.log('Delete buttons found:', deleteButtons.length); // Debug log
    deleteButtons.forEach((btn, index) => {
        console.log(`Delete button ${index}:`, btn, 'data attributes:', {
            'data-id': btn.getAttribute('data-id'),
            'data-post-id': btn.getAttribute('data-post-id'),
            'onclick': btn.getAttribute('onclick')
        });
    });
});
