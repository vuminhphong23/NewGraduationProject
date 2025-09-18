/**
 * Post Interactions - Xử lý like, comment và share
 * Tích hợp với PostManager hiện có
 */

class PostInteractions {
    constructor() {
        this.commentPages = new Map(); // Lưu trạng thái phân trang comment
        this.loadedComments = new Set(); // Track which posts have comments loaded
        this.init();
    }
    
    init() {
        this.bindEvents();
        this.loadInitialLikeStatus();
    }
    
    bindEvents() {
        // Bind Enter key events for comment inputs
        document.addEventListener('keypress', (event) => {
            if (event.target.classList.contains('comment-input')) {
                this.handleCommentInputKeyPress(event);
            }
        });
    }
    
    // ===========================
    // LIKE FUNCTIONALITY
    // ===========================
    
    async toggleLike(button) {
        const postId = button.getAttribute('data-post-id');
        if (!postId || button.dataset.loading === 'true') return;
        
        button.dataset.loading = 'true';
        
        try {
            const response = await authenticatedFetch(`/api/posts/${postId}/like`, { method: 'POST' });
            
            if (response.ok) {
                const result = await response.json();
                this.updateLikeUI(button, result.isLiked, result.likeCount);
                this.showToast(result.message, 'success');
            } else {
                const error = await response.json().catch(() => ({ message: 'Có lỗi xảy ra' }));
                this.showToast(error.message || 'Không thể thích bài viết', 'error');
            }
        } catch (error) {
            this.showToast('Không thể kết nối đến máy chủ', 'error');
        } finally {
            button.dataset.loading = 'false';
        }
    }
    
    updateLikeUI(button, isLiked, likeCount) {
        const icon = button.querySelector('i');
        const likeText = button.querySelector('.like-text');

        if (icon) {
            // Remove all classes first
            icon.classList.remove('fa-solid', 'fa-regular', 'text-primary');
            
            // Add appropriate classes
            if (isLiked) {
                icon.classList.add('fa-solid', 'text-primary');
            } else {
                icon.classList.add('fa-regular');
            }
        }

        if (likeText) {
            likeText.textContent = isLiked ? 'Đã thích' : 'Thích';
        }

        const card = button.closest('.card');
        const likeCountEl = card.querySelector('.like-count');
        if (likeCountEl) {
            likeCountEl.textContent = `${likeCount} lượt thích`;
        }

        button.setAttribute('data-liked', isLiked);
    }
    
    async loadInitialLikeStatus() {
        // Load like status for all posts
        const likeButtons = document.querySelectorAll('.like-btn');
        
        for (const button of likeButtons) {
            const postId = button.getAttribute('data-post-id');
            if (postId) {
                await this.loadLikeStatus(button);
            }
        }
    }
    
    async loadLikeStatus(button) {
        const postId = button.getAttribute('data-post-id');
        try {
            const response = await authenticatedFetch(`/api/posts/${postId}/like-status`);
            
            if (response.ok) {
                const result = await response.json();
                this.updateLikeUI(button, result.isLiked, result.likeCount);
            }
        } catch (error) {
            // Silent fail for like status loading
        }
    }
    
    // ===========================
    // COMMENT FUNCTIONALITY
    // ===========================
    
    toggleCommentSection(button) {
        const postId = button.getAttribute('data-post-id');
        
        // Find the post element by looking for the card that contains this button
        const postElement = button.closest('.card');
        if (!postElement) {
            console.error('Post element not found for comment button:', button);
            return;
        }
        
        // Find comment section within the same card
        const commentSection = postElement.querySelector('.comment-section');
        if (!commentSection) {
            console.error('Comment section not found for post:', postId);
            console.log('Available elements in post:', postElement.innerHTML);
            return;
        }
        
        if (commentSection.classList.contains('d-none')) {
            // Show comment section
            commentSection.classList.remove('d-none');
            // Only load comments if not already loaded
            if (!this.loadedComments.has(postId)) {
                this.loadComments(postId);
            }
        } else {
            // Hide comment section
            commentSection.classList.add('d-none');
        }
    }
    
    async loadComments(postId, page = 0) {
        try {
            const response = await authenticatedFetch(`/api/posts/${postId}/comments?page=${page}&size=5`);
            
            if (response.ok) {
                const comments = await response.json();
                this.displayComments(postId, comments, page === 0);
                
                // Update pagination state
                this.commentPages.set(postId, { currentPage: page, hasMore: comments.length === 5 });
                this.updateLoadMoreButton(postId);
                
                // Mark as loaded for first page
                if (page === 0) {
                    this.loadedComments.add(postId);
                }
            } else {
                console.error('Failed to load comments:', response.status, response.statusText);
                this.showToast('Không thể tải bình luận', 'error');
            }
        } catch (error) {
            console.error('Load comments error:', error);
            this.showToast('Không thể tải bình luận', 'error');
        }
    }
    
    displayComments(postId, comments, replace = false) {
        // Find the comment section by data-post-id
        const commentSection = document.querySelector(`.comment-section[data-post-id="${postId}"]`);
        if (!commentSection) {
            console.error('Comment section not found for post ID:', postId);
            return;
        }
        
        const commentsList = commentSection.querySelector('.comments-list');
        if (!commentsList) {
            console.error('Comments list not found for post:', postId);
            return;
        }
        
        if (replace) {
            commentsList.innerHTML = '';
        }
        
        if (comments.length === 0 && replace) {
            commentsList.innerHTML = '<div class="text-muted text-center py-3">Chưa có bình luận nào</div>';
            return;
        }
        
        comments.forEach(comment => {
            const commentHtml = this.createCommentHTML(comment);
            commentsList.insertAdjacentHTML('beforeend', commentHtml);
            
            // Load like status for this comment
            this.loadCommentLikeStatus(comment.id);
        });
    }
    
    createCommentHTML(comment) {
        const createdAt = new Date(comment.createdAt).toLocaleString('vi-VN');
        const isLiked = comment.isLiked || false;
        const likeCount = comment.likeCount || 0;
        
        // Sử dụng avatar từ backend, để trống nếu không có
        let avatarSrc = 'https://cdn.pixabay.com/photo/2023/02/18/11/00/icon-7797704_640.png'; // Ảnh placeholder trống
        if (comment.userAvatar && comment.userAvatar.trim() !== '') {
            avatarSrc = comment.userAvatar; // Sử dụng avatar từ Cloudinary
        }
        
        return `
            <div class="comment-item d-flex gap-2 mb-2" data-comment-id="${comment.id}">
                <img src="${avatarSrc}" alt="avatar" class="rounded-circle" width="24" height="24"
                     onerror="this.src='https://cdn.pixabay.com/photo/2023/02/18/11/00/icon-7797704_640.png'">
                <div class="flex-grow-1">
                    <div class="bg-light rounded p-2">
                        <div class="d-flex justify-content-between align-items-center mb-1">
                            <div class="fw-semibold small">${comment.userName}</div>
                            <div class="d-flex align-items-center gap-2 flex-shrink-0">
                                <button class="btn btn-link btn-sm p-0 me-1" onclick="startEditComment(${comment.id})" title="Sửa">
                                    <i class="fa fa-pen text-secondary"></i>
                                </button>
                                <button class="btn btn-link btn-sm p-0" onclick="deleteComment(${comment.id}, ${comment.userId})" title="Xóa">
                                    <i class="fa fa-trash text-danger"></i>
                                </button>
                            </div>
                        </div>
                        <div class="comment-content" data-content>${comment.content}</div>
                        <div class="text-muted small mt-1">${createdAt}</div>
                        
                        <!-- Comment Like Section -->
                        <div class="d-flex align-items-center gap-3 mt-2">
                            <button class="btn btn-link btn-sm p-0 like-comment-btn" 
                                    onclick="toggleCommentLike(${comment.id})" 
                                    data-liked="${isLiked}" 
                                    data-loading="false">
                                <i class="fa ${isLiked ? 'fa-solid text-primary' : 'fa-regular'}"></i>
                                <span class="like-text">${isLiked ? 'Đã thích' : 'Thích'}</span>
                            </button>
                            <span class="comment-like-count text-muted small">${likeCount} lượt thích</span>
                        </div>
                    </div>
                </div>
            </div>
        `;
    }
    
    updateLoadMoreButton(postId) {
        // Find the comment section by data-post-id
        const commentSection = document.querySelector(`.comment-section[data-post-id="${postId}"]`);
        if (!commentSection) {
            console.error('Comment section not found for post ID:', postId);
            return;
        }
        
        const loadMoreButton = commentSection.querySelector('.load-more-comments');
        if (!loadMoreButton) {
            console.error('Load more button not found for post:', postId);
            return;
        }
        
        const loadMoreDiv = loadMoreButton.parentElement;
        const pageInfo = this.commentPages.get(postId);
        
        if (pageInfo && pageInfo.hasMore) {
            loadMoreDiv.classList.remove('d-none');
        } else {
            loadMoreDiv.classList.add('d-none');
        }
    }
    
    async postComment(inputOrButton) {
        if (inputOrButton.dataset.submitting === 'true') return;
        
        const container = inputOrButton.closest('.comment-section');
        const input = container.querySelector('.comment-input');
        const content = input.value.trim();
        
        if (!content) {
            this.showToast('Vui lòng nhập nội dung bình luận', 'warning');
            return;
        }
        
        inputOrButton.dataset.submitting = 'true';
        const submitButton = container.querySelector('.post-comment-btn');
        if (submitButton) {
            submitButton.disabled = true;
            submitButton.innerHTML = '<i class="fa fa-spinner fa-spin"></i>';
        }
        
        try {
            const response = await authenticatedFetch(`/api/posts/${input.getAttribute('data-post-id')}/comments`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ content })
            });
            
            if (response.ok) {
                const result = await response.json();
                input.value = '';
                
                const commentsList = container.querySelector('.comments-list');
                commentsList.querySelector('.text-muted.text-center')?.remove();
                commentsList.insertAdjacentHTML('beforeend', this.createCommentHTML(result.comment));

                const commentCountElement = container.closest('.card').querySelector('.comment-count');
                if (commentCountElement) {
                    commentCountElement.textContent = `${result.commentCount} bình luận`;
                }
                
                this.commentPages.set(input.getAttribute('data-post-id'), { currentPage: 0, hasMore: false });
                this.updateLoadMoreButton(input.getAttribute('data-post-id'));
                this.loadedComments.add(input.getAttribute('data-post-id'));
                
                this.showToast('Đã đăng bình luận thành công!', 'success');
            } else {
                const error = await response.json().catch(() => ({ message: 'Có lỗi xảy ra' }));
                this.showToast(error.message || 'Không thể đăng bình luận', 'error');
            }
        } catch (error) {
            console.error('Post comment error:', error);
            this.showToast('Không thể kết nối đến máy chủ', 'error');
        } finally {
            inputOrButton.dataset.submitting = 'false';
            if (submitButton) {
                submitButton.disabled = false;
                submitButton.innerHTML = '<i class="fa fa-paper-plane"></i>';
            }
        }
    }

    // Xử lý Enter key cho comment input
    handleCommentInputKeyPress(event) {
        if (event.key === 'Enter' && !event.shiftKey) {
            event.preventDefault();
            this.postComment(event.target);
        }
    }

    startEditComment(commentId) {
        const item = document.querySelector(`[data-comment-id="${commentId}"]`);
        if (!item) return;
        
        const contentEl = item.querySelector('[data-content]');
        const currentContent = contentEl.textContent;
        
        const editHtml = `
            <div class="edit-comment-form">
                <textarea class="form-control mb-2" rows="2">${currentContent}</textarea>
                <div class="d-flex gap-2">
                    <button class="btn btn-primary btn-sm" onclick="saveEditComment(${commentId})">Lưu</button>
                    <button class="btn btn-secondary btn-sm" onclick="cancelEditComment(${commentId})">Hủy</button>
                </div>
            </div>
        `;
        
        contentEl.style.display = 'none';
        contentEl.insertAdjacentHTML('afterend', editHtml);
    }

    async confirmEditComment(commentId) {
        const item = document.querySelector(`[data-comment-id="${commentId}"]`);
        if (!item) return;
        const container = item.closest('.comment-section');
        const input = item.querySelector('input');
        const newContent = input.value.trim();
        if (!newContent) {
            this.showToast('Nội dung không được trống', 'warning');
            return;
        }
        try {
            const res = await authenticatedFetch(`/api/posts/${container.getAttribute('data-post-id')}/comments/${commentId}`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
                body: JSON.stringify({ content: newContent })
            });
            if (res.ok) {
                const data = await res.json();
                const contentEl = item.querySelector('[data-content]');
                contentEl.textContent = data.comment.content;
                this.showToast('Đã cập nhật bình luận', 'success');
            } else {
                this.showToast('Không thể cập nhật bình luận', 'error');
            }
        } catch (e) {
            this.showToast('Không thể kết nối đến máy chủ', 'error');
        }
    }

    cancelEditComment(commentId) {
        const item = document.querySelector(`[data-comment-id="${commentId}"]`);
        if (!item) return;
        
        const editForm = item.querySelector('.edit-comment-form');
        const contentEl = item.querySelector('[data-content]');
        
        contentEl.style.display = 'block';
        editForm.remove();
    }
    
    async loadMoreComments(button) {
        const postId = button.getAttribute('data-post-id');
        const pageInfo = this.commentPages.get(postId);
        
        if (pageInfo && pageInfo.hasMore) {
            await this.loadComments(postId, pageInfo.currentPage + 1);
        }
    }
    
    async deleteComment(commentId, commentUserId) {
        // Check if current user can delete this comment
        // This would need to be implemented based on your authentication system
        
        if (!confirm('Bạn có chắc chắn muốn xóa bình luận này?')) return;
        
        try {
            // Get post ID from the comment element
            const commentElement = document.querySelector(`[data-comment-id="${commentId}"]`);
            const commentSection = commentElement.closest('.comment-section');
            const postId = commentSection.getAttribute('data-post-id');
            
            const response = await authenticatedFetch(`/api/posts/${postId}/comments/${commentId}`, {
                method: 'DELETE'
            });
            
            if (response.ok) {
                // Remove comment from UI
                commentElement.remove();

                // Update comment count
                const result = await response.json();
                const postCard = commentSection.closest('.card');
                const commentCountElement = postCard.querySelector('.comment-count');
                if (commentCountElement) {
                    commentCountElement.textContent = `${result.commentCount} bình luận`;
                }

                // If list becomes empty, show empty state
                const commentsList = commentSection.querySelector('.comments-list');
                if (!commentsList.querySelector('.comment-item')) {
                    commentsList.innerHTML = '<div class="text-muted text-center py-3">Chưa có bình luận nào</div>';
                }

                this.showToast('Đã xóa bình luận thành công!', 'success');
            } else {
                const error = await response.json().catch(() => ({ message: 'Có lỗi xảy ra' }));
                this.showToast(error.message || 'Không thể xóa bình luận', 'error');
            }
        } catch (error) {
            console.error('Delete comment error:', error);
            this.showToast('Không thể kết nối đến máy chủ', 'error');
        }
    }
    
    // ===========================
    // SHARE FUNCTIONALITY
    // ===========================
    
    async sharePost(button) {
        const postId = button.getAttribute('data-post-id');
        
        // Show share modal with post preview
        await this.showShareModal(postId);
    }
    
    async showShareModal(postId) {
        try {
            // Load post data for preview
            const response = await authenticatedFetch(`/api/posts/${postId}`);
            if (!response.ok) throw new Error('Failed to load post');
            const postData = await response.json();
            
            const modalHtml = `
                <div class="modal fade" id="shareModal" tabindex="-1">
                    <div class="modal-dialog modal-dialog-centered modal-lg">
                        <div class="modal-content">
                            <div class="modal-header bg-primary text-white">
                                <h5 class="modal-title">
                                    <i class="fa fa-share-alt me-2"></i>Chia sẻ bài viết
                                </h5>
                                <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
                            </div>
                            <div class="modal-body">
                                <!-- Post Preview -->
                                <div class="post-preview mb-4">
                                    <div class="card border-0 bg-light">
                                        <div class="card-body p-3">
                                            <div class="d-flex align-items-center mb-2">
                                                <img src="${postData.userAvatar || 'https://cdn.pixabay.com/photo/2023/02/18/11/00/icon-7797704_640.png'}" 
                                                     alt="avatar" class="rounded-circle me-2" width="32" height="32">
                                                <div>
                                                    <strong>${postData.userName}</strong>
                                                    ${postData.groupName ? `<span class="badge bg-primary ms-2"><i class="fa fa-users me-1"></i>${postData.groupName}</span>` : ''}
                                                    <small class="text-muted d-block">${this.formatTime(postData.createdAt)}</small>
                                                </div>
                                           
                                                ${postData.topicNames && postData.topicNames.length > 0 ? `<span style="color: #1da1f2" class="ms-auto fw-bold">#${postData.topicNames.map(topic => topic.toLowerCase().replace(/\s+/g, '_')).join(' #')}</span>` : ''}
                                            </div>
                                            <div class="post-content">
                                                <h6 class="mb-2">${postData.title || ''}</h6>
                                                <p class="mb-2 text-muted small">${postData.content ? postData.content.substring(0, 150) + (postData.content.length > 150 ? '...' : '') : ''}</p>
                                                
                                                <!-- File attachments preview -->
                                                ${postData.documents && postData.documents.length > 0 ? `
                                                    <div class="mt-2">
                                                        <small class="text-muted">
                                                            <i class="fa fa-paperclip me-1"></i>
                                                            ${postData.documents.length} tệp đính kèm
                                                        </small>
                                                    </div>
                                                ` : ''}
                                                
                                                <!-- Post stats -->
                                                <div class="d-flex justify-content-around mt-3 border-top pt-2">
                                                    <small class="text-muted me-3">
                                                        <i class="fa fa-heart me-1"></i>${postData.likeCount || 0} lượt thích
                                                    </small>
                                                    <small class="text-muted me-3">
                                                        <i class="fa fa-comment me-1"></i>${postData.commentCount || 0} bình luận
                                                    </small>
                                                    <small class="text-muted">
                                                        <i class="fa fa-share me-1"></i>${postData.shareCount || 0} lượt chia sẻ
                                                    </small>
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                                
                                <!-- Share Options -->
                                <div class="share-options">
                                    <div class="mb-3">
                                        <label class="form-label fw-bold">Thêm tin nhắn của bạn</label>
                                        <textarea class="form-control" id="shareMessage" rows="3" 
                                                  placeholder="Viết gì đó về bài viết này..."></textarea>
                                    </div>
                                    
                                    <div class="mb-3">
                                        <label class="form-label fw-bold">Chia sẻ với</label>
                                        <div class="privacy-options">
                                            <div class="form-check">
                                                <input class="form-check-input" type="radio" name="sharePrivacy" id="sharePublic" value="PUBLIC" checked>
                                                <label class="form-check-label" for="sharePublic">
                                                    <i class="fa fa-globe-asia me-2"></i>Công khai
                                                </label>
                                            </div>
                                            <div class="form-check">
                                                <input class="form-check-input" type="radio" name="sharePrivacy" id="shareFriends" value="FRIENDS">
                                                <label class="form-check-label" for="shareFriends">
                                                    <i class="fa fa-users me-2"></i>Bạn bè
                                                </label>
                                            </div>
                                            <div class="form-check">
                                                <input class="form-check-input" type="radio" name="sharePrivacy" id="sharePrivate" value="PRIVATE">
                                                <label class="form-check-label" for="sharePrivate">
                                                    <i class="fa fa-lock me-2"></i>Chỉ mình tôi
                                                </label>
                                            </div>
                                        </div>
                                    </div>
                                    
                                    <div class="share-methods">
                                        <div class="row g-2">
                                            <div class="col-6">
                                                <button type="button" class="btn btn-primary w-100" onclick="confirmShare(${postId})">
                                                    <i class="fa fa-share me-2"></i>Chia sẻ nội bộ
                                                </button>
                                            </div>
                                            <div class="col-6">
                                                <button type="button" class="btn btn-outline-primary w-100" data-post-id="${postId}" id="copyLinkBtn">
                                                    <i class="fa fa-link me-2"></i>Copy link
                                                </button>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            `;
            
            // Remove existing modal if any
            const existingModal = document.getElementById('shareModal');
            if (existingModal) {
                existingModal.remove();
            }
            
            // Add new modal
            document.body.insertAdjacentHTML('beforeend', modalHtml);
            
            // Show modal
            const modal = new bootstrap.Modal(document.getElementById('shareModal'));
            
            // Focus on the share button when modal opens
            modal._element.addEventListener('shown.bs.modal', () => {
                const shareButton = modal._element.querySelector('button[onclick*="confirmShare"]');
                if (shareButton) {
                    shareButton.focus();
                }
                
                // Add click event for copy link button
                const copyLinkBtn = modal._element.querySelector('#copyLinkBtn');
                if (copyLinkBtn) {
                    copyLinkBtn.addEventListener('click', () => {
                        this.copyPostLink(postId);
                    });
                }
            });
            
            modal.show();
            
        } catch (error) {
            console.error('Error loading post for share:', error);
            this.showToast('Có lỗi xảy ra khi tải bài viết', 'error');
        }
    }
    
    formatTime(timestamp) {
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
    
    async confirmShare(postId) {
        const message = document.getElementById('shareMessage')?.value?.trim() || null;
        const privacy = document.querySelector('input[name="sharePrivacy"]:checked')?.value || 'PUBLIC';
        
        try {
            const response = await authenticatedFetch(`/api/posts/${postId}/share`, {
                method: 'POST',
                headers: { 
                    'Content-Type': 'application/json',
                    'Accept': 'application/json'
                },
                body: JSON.stringify({ 
                    message,
                    privacy 
                })
            });
            
            if (response.ok) {
                const result = await response.json();
                
                // Close modal
                bootstrap.Modal.getInstance(document.getElementById('shareModal')).hide();
                
                // Update share count
                const postElement = document.querySelector(`[data-post-id="${postId}"]`).closest('.card');
                const shareCountElement = postElement.querySelector('.share-count');
                if (shareCountElement) {
                    shareCountElement.textContent = result.shareCount;
                }
                
                this.showToast('Đã chia sẻ bài viết thành công!', 'success');
                
                // Reload page to show shared post
                setTimeout(() => window.location.reload(), 1000);
            } else {
                const error = await response.json().catch(() => ({ message: 'Có lỗi xảy ra' }));
                this.showToast(error.message || 'Không thể chia sẻ bài viết', 'error');
            }
        } catch (error) {
            console.error('Share error:', error);
            this.showToast('Không thể kết nối đến máy chủ', 'error');
        }
    }
    
    async copyPostLink(postId) {
        const url = `${window.location.origin}/posts/${postId}`;
        
        try {
            await navigator.clipboard.writeText(url);
            this.showToast('Đã copy link bài viết!', 'success');
            bootstrap.Modal.getInstance(document.getElementById('shareModal')).hide();
        } catch (error) {
            // Fallback
            const textArea = document.createElement('textarea');
            textArea.value = url;
            document.body.appendChild(textArea);
            textArea.select();
            document.execCommand('copy');
            document.body.removeChild(textArea);
            this.showToast('Đã copy link bài viết!', 'success');
            bootstrap.Modal.getInstance(document.getElementById('shareModal')).hide();
        }
    }
    
    // ===========================
    // UTILITY FUNCTIONS
    // ===========================
    
    showToast(message, type = 'info') {
        if (window.toastManager) {
            window.toastManager.show(message, type);
        }
    }

    async toggleCommentLike(commentId) {
        const likeButton = document.querySelector(`[data-comment-id="${commentId}"] .like-comment-btn`);
        if (!likeButton || likeButton.dataset.loading === 'true') return;
        
        likeButton.dataset.loading = 'true';
        
        try {
            const response = await authenticatedFetch(`/api/comments/${commentId}/like`, { method: 'POST' });
            
            if (response.ok) {
                const result = await response.json();
                this.updateCommentLikeUI(likeButton, result.isLiked, result.likeCount);
                this.showToast(result.message || (result.isLiked ? 'Đã thích bình luận' : 'Đã bỏ thích bình luận'), 'success');
            } else {
                const error = await response.json().catch(() => ({ message: 'Có lỗi xảy ra' }));
                this.showToast(error.message || 'Không thể thích bình luận', 'error');
            }
        } catch (error) {
            console.error('Comment like error:', error);
            this.showToast('Không thể kết nối đến máy chủ', 'error');
        } finally {
            likeButton.dataset.loading = 'false';
        }
    }

    updateCommentLikeUI(button, isLiked, likeCount) {
        const icon = button.querySelector('i');
        const likeText = button.querySelector('.like-text');
        const commentItem = button.closest('.comment-item');
        const likeCountEl = commentItem.querySelector('.comment-like-count');

        // Update icon
        icon.classList.remove('fa-solid', 'fa-regular', 'text-primary');
        icon.classList.add(isLiked ? 'fa-solid' : 'fa-regular');
        if (isLiked) {
            icon.classList.add('text-primary');
        }

        // Update text
        likeText.textContent = isLiked ? 'Đã thích' : 'Thích';

        // Update count
        if (likeCountEl) {
            likeCountEl.textContent = `${likeCount} lượt thích`;
        }

        // Update button state
        button.setAttribute('data-liked', isLiked);
    }

    async saveEditComment(commentId) {
        const item = document.querySelector(`[data-comment-id="${commentId}"]`);
        if (!item) return;
        
        const editForm = item.querySelector('.edit-comment-form');
        const textarea = editForm.querySelector('textarea');
        const content = textarea.value.trim();
        
        if (!content) {
            this.showToast('Nội dung không được để trống', 'warning');
            return;
        }
        
        try {
            const response = await authenticatedFetch(`/api/comments/${commentId}`, {
                method: 'PUT',
                headers: { 
                    'Content-Type': 'application/json',
                    'Accept': 'application/json'
                },
                body: JSON.stringify({ content })
            });
            
            if (response.ok) {
                const contentEl = item.querySelector('[data-content]');
                contentEl.textContent = content;
                contentEl.style.display = 'block';
                editForm.remove();
                this.showToast('Đã cập nhật bình luận thành công!', 'success');
            } else {
                const error = await response.json().catch(() => ({ message: 'Có lỗi xảy ra' }));
                this.showToast(error.message || 'Không thể cập nhật bình luận', 'error');
            }
        } catch (error) {
            console.error('Edit comment error:', error);
            this.showToast('Không thể kết nối đến máy chủ', 'error');
        }
    }

    cancelEditComment(commentId) {
        const item = document.querySelector(`[data-comment-id="${commentId}"]`);
        if (!item) return;
        
        const editForm = item.querySelector('.edit-comment-form');
        const contentEl = item.querySelector('[data-content]');
        
        contentEl.style.display = 'block';
        editForm.remove();
    }

    async loadCommentLikeStatus(commentId) {
        try {
            const response = await authenticatedFetch(`/api/comments/${commentId}/like-status`);
            
            if (response.ok) {
                const result = await response.json();
                const likeButton = document.querySelector(`[data-comment-id="${commentId}"] .like-comment-btn`);
                if (likeButton) {
                    this.updateCommentLikeUI(likeButton, result.isLiked, result.likeCount);
                }
            }
        } catch (error) {
            console.error('Load comment like status error:', error);
        }
    }
}

// ===========================
// HASHTAG SEARCH FUNCTIONALITY
// ===========================

window.searchByHashtag = (hashtag) => {
    
    // Remove # if present
    const cleanHashtag = hashtag.startsWith('#') ? hashtag.substring(1) : hashtag;
    
    // Show toast notification
    if (window.postInteractions) {
        window.postInteractions.showToast(`Đang tìm kiếm bài viết với hashtag #${cleanHashtag}`, 'info');
    }
    
    // Lọc bài viết theo hashtag
    filterPostsByHashtag(cleanHashtag);
};

// Function để lọc bài viết theo hashtag
function filterPostsByHashtag(hashtag) {
    
    // Ẩn nút "Xem tất cả bài viết" nếu đang hiển thị
    const existingButton = document.getElementById('showAllPostsBtn');
    if (existingButton) {
        existingButton.remove();
    }
    
    const allPosts = document.querySelectorAll('.card[data-post-id]');
    let visibleCount = 0;
    
    allPosts.forEach(post => {
        // Tìm hashtags trong bài viết bằng data-hashtag
        const hashtagElements = post.querySelectorAll('.hashtag-link[data-hashtag]');
        let hasMatchingHashtag = false;
        
        hashtagElements.forEach(hashtagEl => {
            const hashtagData = hashtagEl.getAttribute('data-hashtag');
            const cleanHashtagLower = hashtag.toLowerCase();
            
            // So sánh chính xác hashtag
            if (hashtagData && hashtagData.toLowerCase() === cleanHashtagLower) {
                hasMatchingHashtag = true;
            }
        });
        
        // Hiển thị/ẩn bài viết dựa trên hashtag
        if (hasMatchingHashtag) {
            post.style.display = 'block';
            visibleCount++;
            // Highlight hashtag matching
            post.style.border = '2px solid #007bff';
            post.style.borderRadius = '8px';
            post.style.boxShadow = '0 4px 8px rgba(0,123,255,0.2)';
        } else {
            post.style.display = 'none';
        }
    });
    
    // Hiển thị kết quả tìm kiếm
    if (window.postInteractions) {
        if (visibleCount > 0) {
            window.postInteractions.showToast(`Tìm thấy ${visibleCount} bài viết với hashtag #${hashtag}`, 'success');
        } else {
            window.postInteractions.showToast(`Không tìm thấy bài viết nào với hashtag #${hashtag}`, 'warning');
        }
    }
    
    // Chỉ thêm nút "Xem tất cả bài viết" nếu đang lọc và có bài viết bị ẩn
    if (visibleCount < allPosts.length) {
        addShowAllPostsButton();
    }
}

// Function để thêm nút "Xem tất cả bài viết"
function addShowAllPostsButton() {
    // Xóa nút cũ nếu có
    const existingButton = document.getElementById('showAllPostsBtn');
    if (existingButton) {
        existingButton.remove();
    }
    
    // Tạo nút mới
    const showAllButton = document.createElement('div');
    showAllButton.className = 'text-center my-3';
    showAllButton.innerHTML = `
        <button id="showAllPostsBtn" class="btn btn-outline-primary" onclick="showAllPosts()">
            <i class="fa fa-eye me-2"></i>Xem tất cả bài viết
        </button>
    `;
    
    // Thêm vào cuối feed, sau tất cả bài viết
    const feed = document.getElementById('feed');
    if (feed) {
        // Thêm vào cuối feed
        feed.appendChild(showAllButton);
    }
}

// Function để hiển thị tất cả bài viết
window.showAllPosts = () => {
    
    const allPosts = document.querySelectorAll('.card[data-post-id]');
    allPosts.forEach(post => {
        post.style.display = 'block';
        post.style.border = '';
        post.style.borderRadius = '';
        post.style.boxShadow = '';
    });
    
    // Xóa nút "Xem tất cả bài viết"
    const showAllButton = document.getElementById('showAllPostsBtn');
    if (showAllButton) {
        showAllButton.remove();
    }
    
    // Hiển thị toast
    if (window.postInteractions) {
        window.postInteractions.showToast('Đã hiển thị tất cả bài viết', 'info');
    }
};

// ===========================
// GLOBAL FUNCTIONS (for onclick)
// ===========================

window.toggleLike = (button) => {
    window.postInteractions?.toggleLike(button);
};

window.toggleCommentSection = (button) => {
    window.postInteractions?.toggleCommentSection(button);
};

window.postComment = (input) => {
    window.postInteractions?.postComment(input);
};

window.loadMoreComments = (button) => {
    window.postInteractions?.loadMoreComments(button);
};

window.deleteComment = (commentId, commentUserId) => {
    window.postInteractions?.deleteComment(commentId, commentUserId);
};

window.sharePost = (button) => {
    window.postInteractions?.sharePost(button);
};

window.confirmShare = (postId) => {
    window.postInteractions?.confirmShare(postId);
};

window.copyPostLink = (postId) => {
    window.postInteractions?.copyPostLink(postId);
};

// Global functions for HTML onclick
window.startEditComment = (commentId) => window.postInteractions?.startEditComment(commentId);
window.confirmEditComment = (commentId) => window.postInteractions?.confirmEditComment(commentId);
window.cancelEditComment = (commentId) => window.postInteractions?.cancelEditComment(commentId);
window.toggleCommentLike = (commentId) => window.postInteractions?.toggleCommentLike(commentId);
window.saveEditComment = (commentId) => window.postInteractions?.saveEditComment(commentId);
window.handleCommentKeyPress = (event) => window.postInteractions?.handleCommentInputKeyPress(event);

// Initialize
document.addEventListener('DOMContentLoaded', () => {
    if (!window.postInteractions) {
        window.postInteractions = new PostInteractions();
    }
});
