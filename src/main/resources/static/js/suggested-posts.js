class SuggestedPostsManager {
    constructor() {
        this.suggestedPostsContainer = null;
        this.maxLevel = 3;
        this.limit = 20;
        this.init();
    }

    init() {
        // Tìm container cho suggested posts
        this.suggestedPostsContainer = document.getElementById('suggested-posts-container');
        if (!this.suggestedPostsContainer) {
            console.warn('Suggested posts container not found');
            return;
        }

        this.loadSuggestedPosts();
    }

    async loadSuggestedPosts() {
        try {
            const response = await fetch(`/api/suggested-posts?maxLevel=${this.maxLevel}&limit=${this.limit}`, {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json',
                }
            });

            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            const suggestedPosts = await response.json();
            this.displaySuggestedPosts(suggestedPosts);
        } catch (error) {
            console.error('Error loading suggested posts:', error);
            this.showError('Không thể tải bài viết gợi ý');
        }
    }

    displaySuggestedPosts(posts) {
        if (!posts || posts.length === 0) {
            this.suggestedPostsContainer.innerHTML = `
                <div class="alert alert-info">
                    <i class="fas fa-info-circle"></i>
                    Chưa có bài viết gợi ý cho bạn
                </div>
            `;
            return;
        }

        const postsHtml = posts.map(post => this.createPostCard(post)).join('');
        this.suggestedPostsContainer.innerHTML = postsHtml;
    }

    createPostCard(post) {
        const friendshipLevelText = this.getFriendshipLevelText(post.friendshipLevel);
        
        return `
            <div class="card mb-3 suggested-post-card" data-post-id="${post.id}">
                <div class="card-header d-flex justify-content-between align-items-center">
                    <div class="d-flex align-items-center">
                        <img src="https://cdn.pixabay.com/photo/2023/02/18/11/00/icon-7797704_640.png" 
                             class="rounded-circle me-2" width="40" height="40" alt="User Avatar">
                        <div>
                            <h6 class="mb-0">User ID: ${post.userId}</h6>
                            <small class="text-muted">
                                <i class="fas fa-users"></i> ${friendshipLevelText}
                            </small>
                        </div>
                    </div>
                </div>
                <div class="card-body">
                    <h5 class="card-title">${this.escapeHtml(post.title)}</h5>
                    <p class="card-text">${this.escapeHtml(post.content)}</p>
                    <div class="d-flex justify-content-between align-items-center">
                        <small class="text-muted">
                            <i class="fas fa-clock"></i> ${this.formatDate(post.createdAt)}
                        </small>
                        <a href="/posts/${post.id}" class="btn btn-primary btn-sm">
                            <i class="fas fa-eye"></i> Xem chi tiết
                        </a>
                    </div>
                </div>
            </div>
        `;
    }

    getFriendshipLevelText(level) {
        switch (level) {
            case 1: return 'Bạn bè trực tiếp';
            case 2: return 'Bạn của bạn bè';
            case 3: return 'Bạn của bạn của bạn bè';
            default: return `Mức độ ${level}`;
        }
    }

    formatDate(dateString) {
        const date = new Date(dateString);
        const now = new Date();
        const diffInMinutes = Math.floor((now - date) / (1000 * 60));
        
        if (diffInMinutes < 1) return 'Vừa xong';
        if (diffInMinutes < 60) return `${diffInMinutes} phút trước`;
        if (diffInMinutes < 1440) return `${Math.floor(diffInMinutes / 60)} giờ trước`;
        return `${Math.floor(diffInMinutes / 1440)} ngày trước`;
    }

    escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    showError(message) {
        this.suggestedPostsContainer.innerHTML = `
            <div class="alert alert-danger">
                <i class="fas fa-exclamation-triangle"></i>
                ${message}
            </div>
        `;
    }

    refresh() {
        this.loadSuggestedPosts();
    }
}

// Initialize when DOM is loaded
document.addEventListener('DOMContentLoaded', function() {
    window.suggestedPostsManager = new SuggestedPostsManager();
});
