/**
 * Post Manager - Unified Post Management System
 * Quản lý tất cả chức năng liên quan đến bài viết
 */

class PostManager {
    constructor() {
        this.modal = document.getElementById('postModal');
        this.modalTitle = document.getElementById('postModalLabel');
        this.form = document.getElementById('postForm');
        this.titleInput = document.getElementById('postTitle');
        this.contentInput = document.getElementById('postContent');
        this.publishBtn = document.getElementById('publishBtn');
        this.currentPrivacy = 'PUBLIC';
        this.editingPostId = null;
        this.groupId = null; // Add groupId property
        
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

            if (!postId) {
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
                // Let post-interactions.js handle share functionality
                return;
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
    
    setGroupId(groupId) {
        this.groupId = groupId;
    }
    
    async submitPost() {
        if (this.publishBtn.disabled) return;
        
        // Get selected topics but don't save them yet
        const selectedTopics = window.HashtagManager?.getSelected() || [];
        
        const postData = {
            title: this.titleInput.value.trim(),
            content: this.contentInput.value.trim(),
            topicNames: selectedTopics,
            privacy: this.currentPrivacy,
            groupId: this.groupId // Add groupId to post data
        };
        
        const url = this.editingPostId ? `/api/posts/${this.editingPostId}` : '/api/posts';
        const method = this.editingPostId ? 'PUT' : 'POST';
        
        try {
            this.setLoading(true);
            
            const response = await authenticatedFetch(url, {
                method,
                headers: { 
                    'Content-Type': 'application/json',
                    'Accept': 'application/json'
                },
                body: JSON.stringify(postData)
            });
            
            if (response.ok) {
                const result = await response.json();
                
                // Upload files if any are selected
                console.log('Checking for files to upload...');
                console.log('fileUploadManager exists:', !!window.fileUploadManager);
                if (window.fileUploadManager) {
                    console.log('Selected files count:', window.fileUploadManager.selectedFiles.length);
                    console.log('Selected files:', window.fileUploadManager.selectedFiles.map(f => f.name));
                }
                
                if (window.fileUploadManager && window.fileUploadManager.selectedFiles.length > 0) {
                    try {
                        console.log('Starting file upload for post ID:', result.id);
                        await window.fileUploadManager.uploadFiles(result.id);
                        console.log('File upload completed successfully');
                        this.showToast('File đã được đính kèm thành công!', 'success');
                    } catch (fileError) {
                        console.warn('Failed to upload files:', fileError);
                        this.showToast('Bài viết đã được đăng nhưng có lỗi khi đính kèm file', 'warning');
                    }
                } else {
                    console.log('No files to upload');
                }
                
                // Only save topics to HashtagManager after successful post creation
                if (selectedTopics.length > 0 && window.HashtagManager && typeof window.HashtagManager.saveTopics === 'function') {
                    try {
                        await window.HashtagManager.saveTopics(selectedTopics);
                    } catch (topicError) {
                        console.warn('Failed to save topics:', topicError);
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
            const response = await authenticatedFetch(`/api/posts/${postId}`, {
                headers: { 'Accept': 'application/json' }
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
                if (this.modalTitle) {
                    this.modalTitle.textContent = 'Chỉnh sửa bài viết';
                }
                
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

            const response = await authenticatedFetch(`/api/posts/${postId}`, {
                method: 'DELETE',
                headers
            });

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
            this.showToast('Có lỗi xảy ra khi xóa bài viết: ' + error.message, 'error');
        }
    }
    
    async toggleLike(postId) {
        try {
            const response = await authenticatedFetch(`/api/posts/${postId}/like`, {
                method: 'POST',
                headers: { 'Accept': 'application/json' }
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
    
    
    resetForm() {
        this.form?.reset();
        this.currentPrivacy = 'PUBLIC';
        this.setPrivacy('PUBLIC');
        this.editingPostId = null;
        this.groupId = null; // Reset groupId
        this.publishBtn.textContent = 'Đăng bài';
        if (this.modalTitle) {
            this.modalTitle.textContent = 'Tạo bài viết';
        }
        
        // Reset hashtag selection but don't save to server
        if (window.HashtagManager) {
            window.HashtagManager.reset();
        }
        
        // Reset file upload
        if (window.fileUploadManager) {
            window.fileUploadManager.clear();
        }
        
        this.validateForm();
    }
    
    closeModal() {
        bootstrap.Modal.getInstance(this.modal)?.hide();
        // resetForm will be called automatically by modal hidden event
    }
    
    showToast(message, type = 'info') {
        // Sử dụng ToastManager nếu có
        if (window.toastManager) {
            return window.toastManager.show(message, type);
        } else {
            // Fallback nếu ToastManager chưa load
            console.log(`Toast (${type}): ${message}`);
        }
    }
    
    bindExistingPosts() {

        // Find all delete buttons and log them
        const deleteButtons = document.querySelectorAll('[onclick*="deletePost"], .delete-post-btn, .dropdown-item.text-danger');

        // Remove old onclick handlers and add new event listeners
        document.querySelectorAll('[onclick*="deletePost"]').forEach(btn => {
            btn.removeAttribute('onclick');
            
            // Add direct event listener as backup
            btn.addEventListener('click', (e) => {
                e.preventDefault();
                e.stopPropagation();
                const postId = this.getPostId(btn);
                this.deletePost(postId);

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
    }
}

// ===========================
// GLOBAL FUNCTIONS (for compatibility)
// ===========================

window.deletePost = (element) => {
    const postId = window.postManager?.getPostId(element);

    if (postId && window.postManager) {
        window.postManager.deletePost(postId);
    } else {
        // Use toastManager for error notifications
        if (window.toastManager) {
            window.toastManager.error('Không thể xóa bài viết. Vui lòng tải lại trang.');
        } else {
            alert('Không thể xóa bài viết. Vui lòng tải lại trang.');
        }
    }
};

window.editPost = (element) => {
    const postId = window.postManager?.getPostId(element);
    if (postId && window.postManager) {
        window.postManager.editPost(postId);
    } else {
        // Use toastManager for error notifications
        if (window.toastManager) {
            window.toastManager.error('Không thể chỉnh sửa bài viết. Vui lòng tải lại trang.');
        } else {
            alert('Không thể chỉnh sửa bài viết. Vui lòng tải lại trang.');
        }
    }
};

window.toggleLike = (element) => {
    const postId = window.postManager?.getPostId(element);
    if (postId && window.postManager) {
        window.postManager.toggleLike(postId);
    }
};

// Don't override global sharePost - let post-interactions.js handle it
// window.sharePost = (element) => {
//     const postId = window.postManager?.getPostId(element);
//     if (postId && window.postManager) {
//         window.postManager.sharePost(postId);
//     }
// };

window.refreshPosts = () => window.location.reload();


// Initialize
document.addEventListener('DOMContentLoaded', () => {
    window.postManager = new PostManager();
});
