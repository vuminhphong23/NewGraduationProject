// Recommendations JavaScript
document.addEventListener('DOMContentLoaded', function() {
    // Load personalized content on page load
    loadPersonalizedContent();
    
    // Add click handlers for interest buttons
    document.querySelectorAll('.interest-btn').forEach(btn => {
        btn.addEventListener('click', function() {
            // Remove active class from all buttons
            document.querySelectorAll('.interest-btn').forEach(b => b.classList.remove('active'));
            // Add active class to clicked button
            this.classList.add('active');
        });
    });
});

function loadPersonalizedContent() {
    showLoading('personalizedContent');
    
    fetch('/api/recommendations/crawled-content?limit=20')
        .then(response => response.json())
        .then(data => {
            displayPosts(data, 'personalizedContent');
        })
        .catch(error => {
            console.error('Error loading personalized content:', error);
            showError('personalizedContent', 'Không thể tải nội dung gợi ý cá nhân');
        });
}

function loadTrendingContent() {
    showLoading('trendingContent');
    
    fetch('/api/recommendations/crawled-content/trending?limit=20')
        .then(response => response.json())
        .then(data => {
            displayPosts(data, 'trendingContent');
        })
        .catch(error => {
            console.error('Error loading trending content:', error);
            showError('trendingContent', 'Không thể tải nội dung trending');
        });
}

function loadInterestsContent() {
    // This is called when switching to interests tab
    // Content will be loaded when user clicks on specific interest buttons
}

function loadContentByInterest(interest) {
    showLoading('interestsContent');
    
    fetch(`/api/recommendations/crawled-content/interest/${interest}?limit=20`)
        .then(response => response.json())
        .then(data => {
            displayPosts(data, 'interestsContent');
        })
        .catch(error => {
            console.error('Error loading content by interest:', error);
            showError('interestsContent', 'Không thể tải nội dung theo chủ đề');
        });
}

function displayPosts(posts, containerId) {
    const container = document.getElementById(containerId);
    
    if (!posts || posts.length === 0) {
        container.innerHTML = `
            <div class="text-center text-muted py-4">
                <i class="fa fa-inbox fa-3x mb-3"></i>
                <p>Không có nội dung nào để hiển thị</p>
            </div>
        `;
        return;
    }
    
    container.innerHTML = '';
    
    posts.forEach(post => {
        const postElement = createPostElement(post);
        container.appendChild(postElement);
    });
}

function createPostElement(post) {
    const div = document.createElement('div');
    div.className = 'card mb-3 border-0 shadow-sm';
    div.setAttribute('data-post-id', post.id);
    
    // Format creation date
    const createdDate = new Date(post.createdAt);
    const formattedDate = createdDate.toLocaleDateString('vi-VN', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
    });
    
    // Create badges
    let badges = '';
    if (post.isCrawledContent) {
        badges += '<span class="badge bg-info text-white me-2">🤖 AI Content</span>';
    }
    if (post.recommendationScore !== null) {
        badges += `<span class="badge bg-success text-white">Score: ${post.recommendationScore.toFixed(1)}</span>`;
    }
    
    div.innerHTML = `
        <div class="card-header bg-white border-0 d-flex align-items-center justify-content-between">
            <div class="d-flex align-items-center gap-2">
                <img src="https://cdn.pixabay.com/photo/2023/02/18/11/00/icon-7797704_640.png"
                     class="rounded-circle" width="40" height="40" alt="avatar">
                <div>
                    <div class="fw-semibold d-flex align-items-center gap-2">
                        <span>${post.user.firstName} ${post.user.lastName}</span>
                        ${badges}
                    </div>
                    <div class="text-muted small">
                        <span>${formattedDate}</span>
                    </div>
                </div>
            </div>
        </div>
        <div class="card-body">
            <h5 class="card-title">${post.title}</h5>
            <p class="card-text">${post.content}</p>
            <div class="d-flex justify-content-between align-items-center">
                <div class="engagement-stats">
                    <span class="me-3"><i class="fa fa-thumbs-up text-primary"></i> ${post.likeCount || 0}</span>
                    <span class="me-3"><i class="fa fa-comment text-info"></i> ${post.commentCount || 0}</span>
                    <span class="me-3"><i class="fa fa-share text-success"></i> ${post.shareCount || 0}</span>
                </div>
                <div class="post-actions">
                    <button class="btn btn-sm btn-outline-primary" onclick="likePost(${post.id})">
                        <i class="fa fa-thumbs-up"></i> Like
                    </button>
                    <button class="btn btn-sm btn-outline-info" onclick="commentPost(${post.id})">
                        <i class="fa fa-comment"></i> Comment
                    </button>
                    <button class="btn btn-sm btn-outline-success" onclick="sharePost(${post.id})">
                        <i class="fa fa-share"></i> Share
                    </button>
                </div>
            </div>
        </div>
    `;
    
    return div;
}

function showLoading(containerId) {
    const container = document.getElementById(containerId);
    container.innerHTML = `
        <div class="text-center text-muted py-4">
            <i class="fa fa-spinner fa-spin fa-2x mb-3"></i>
            <p>Đang tải nội dung...</p>
        </div>
    `;
}

function showError(containerId, message) {
    const container = document.getElementById(containerId);
    container.innerHTML = `
        <div class="text-center text-danger py-4">
            <i class="fa fa-exclamation-triangle fa-2x mb-3"></i>
            <p>${message}</p>
            <button class="btn btn-outline-primary" onclick="location.reload()">
                <i class="fa fa-refresh me-1"></i> Thử lại
            </button>
        </div>
    `;
}

function refreshRecommendations() {
    // Refresh all tabs
    loadPersonalizedContent();
    loadTrendingContent();
    
    // Show success message
    showToast('Đã làm mới gợi ý thành công!', 'success');
}

function likePost(postId) {
    // Implement like functionality
    console.log('Liking post:', postId);
    showToast('Đã like bài viết!', 'success');
}

function commentPost(postId) {
    // Implement comment functionality
    console.log('Commenting on post:', postId);
    showToast('Mở form comment...', 'info');
}

function sharePost(postId) {
    // Implement share functionality
    console.log('Sharing post:', postId);
    showToast('Đã chia sẻ bài viết!', 'success');
}

function showToast(message, type = 'info') {
    // Create toast element
    const toast = document.createElement('div');
    toast.className = `toast align-items-center text-white bg-${type} border-0`;
    toast.setAttribute('role', 'alert');
    toast.innerHTML = `
        <div class="d-flex">
            <div class="toast-body">${message}</div>
            <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button>
        </div>
    `;
    
    // Add to page
    document.body.appendChild(toast);
    
    // Show toast
    const bsToast = new bootstrap.Toast(toast);
    bsToast.show();
    
    // Remove after hidden
    toast.addEventListener('hidden.bs.toast', () => {
        toast.remove();
    });
}


