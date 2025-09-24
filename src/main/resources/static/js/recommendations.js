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
    
    // Gọi API để lấy bài viết được phân tích điểm dựa trên topic quan tâm và tương tác
    authenticatedFetch('/api/recommendations/personalized?limit=20')
        .then(response => response.json())
        .then(data => {
            displayPosts(data, 'personalizedContent');
        })
        .catch(error => {
            console.error('Error loading personalized content:', error);
            showError('personalizedContent', 'Không thể phân tích mối quan tâm của bạn');
        });
}

function loadTrendingContent() {
    showLoading('trendingContent');
    
    // Gọi API để lấy bài viết trending theo điểm: Like(1) + Comment(2) + Share(3)
    authenticatedFetch('/api/recommendations/trending?limit=20')
        .then(response => response.json())
        .then(data => {
            displayPosts(data, 'trendingContent');
        })
        .catch(error => {
            console.error('Error loading trending content:', error);
            showError('trendingContent', 'Không thể tính toán bài viết trending');
        });
}

function loadGroupsContent() {
    showLoading('groupsContent');
    
    // Gọi API để lấy nhóm gợi ý dựa trên topic quan tâm và bạn chung
    authenticatedFetch('/api/recommendations/groups?limit=20')
        .then(response => response.json())
        .then(data => {
            displayGroups(data, 'groupsContent');
        })
        .catch(error => {
            console.error('Error loading groups content:', error);
            showError('groupsContent', 'Không thể tìm nhóm phù hợp');
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

function displayGroups(groups, containerId) {
    const container = document.getElementById(containerId);
    
    if (!groups || groups.length === 0) {
        container.innerHTML = `
            <div class="text-center text-muted py-4">
                <i class="fa fa-users fa-3x mb-3"></i>
                <p>Không có nhóm nào phù hợp</p>
            </div>
        `;
        return;
    }
    
    container.innerHTML = '';
    
    groups.forEach(group => {
        const groupElement = createGroupElement(group);
        container.appendChild(groupElement);
    });
}

function createPostElement(post) {
    const div = document.createElement('div');
    div.className = 'card mb-4 border-0 shadow-sm post-card-clickable';
    div.setAttribute('data-post-id', post.id);
    div.style.cursor = 'pointer';
    div.onclick = function() {
        window.location.href = `/posts/${post.id}`;
    };
    
    // Format creation date
    const createdDate = new Date(post.createdAt);
    const formattedDate = createdDate.toLocaleDateString('vi-VN', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
    });
    
    // Privacy icon
    const privacyIcon = post.privacy === 'PUBLIC' ? 'fa-globe-asia' : 
                       post.privacy === 'FRIENDS' ? 'fa-users' : 'fa-lock';
    
    // Recommendation score badge
    let recommendationBadge = '';
    if (post.recommendationScore !== null && post.recommendationScore > 0) {
        const scoreColor = post.recommendationScore >= 5 ? 'bg-success' : 
                          post.recommendationScore >= 3 ? 'bg-warning' : 'bg-info';
        recommendationBadge = `<span class="badge ${scoreColor} text-white">Score: ${post.recommendationScore.toFixed(1)}</span>`;
    }
    
    // Group badge
    let groupBadge = '';
    if (post.groupId) {
        groupBadge = `
            <span class="badge bg-primary ms-2 d-flex align-items-center" style="font-size: 0.7rem; padding: 2px 6px;">
                <i class="fa fa-users me-1"></i>
                <span>${post.groupName}</span>
            </span>
        `;
    }
    
    // Topics/hashtags - will be placed in header
    let topicsHtml = '';
    if (post.topicNames && post.topicNames.length > 0) {
        topicsHtml = post.topicNames.map(topic => `
            <span class="fw-bold" style="font-size: 0.9rem; color: #1da1f2">
                #${topic}
            </span>
        `).join('');
    }
    
    // Documents
    let documentsHtml = '';
    if (post.documents && post.documents.length > 0) {
        documentsHtml = `
            <div class="mt-2">
                <small class="text-muted">
                    <i class="fa fa-paperclip me-1"></i>
                    <span>${post.documents.length} tệp đính kèm</span>
                </small>
            </div>
        `;
    }
    
    // Shared post content
    let sharedPostContent = '';
    if (post.originalPost) {
        // This is a shared post
        const originalDate = new Date(post.originalPost.createdAt);
        const originalFormattedDate = originalDate.toLocaleDateString('vi-VN', {
            day: '2-digit',
            month: '2-digit',
            year: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        });
        
        sharedPostContent = `
            <!-- Message của người chia sẻ -->
            <div class="share-message mb-3" ${!post.content ? 'style="display: none;"' : ''}>
                <p class="card-text" style="white-space: pre-wrap; font-size: 16px;">${post.content || ''}</p>
            </div>
            
            <!-- Original Post Preview -->
            <div class="original-post-preview">
                <div class="card border-0 bg-light">
                    <div class="card-body p-3">
                        <!-- Original Post Author -->
                        <div class="d-flex align-items-center mb-2">
                            <img src="${post.originalPost.userAvatar || 'https://cdn.pixabay.com/photo/2023/02/18/11/00/icon-7797704_640.png'}" 
                                 alt="avatar" class="rounded-circle me-2" width="32" height="32"
                                 onerror="this.src='https://cdn.pixabay.com/photo/2023/02/18/11/00/icon-7797704_640.png'">
                            <div class="flex-grow-1">
                                <div class="d-flex align-items-center mb-1">
                                    <strong>${post.originalPost.userName}</strong>
                                    ${post.originalPost.groupName ? `
                                        <span class="badge bg-primary ms-2 d-flex align-items-center" style="font-size: 0.7rem; padding: 2px 6px;">
                                            <i class="fa fa-users me-1"></i>
                                            <span>${post.originalPost.groupName}</span>
                                        </span>
                                    ` : ''}
                                </div>
                                <div class="text-muted small">
                                    <small>${originalFormattedDate}</small>
                                    •
                                    <small>
                                        <i class="fa fa-globe-asia"></i>
                                    </small>
                                </div>
                            </div>
                        </div>
                        
                    </div>
                    
                    <!-- Original Post Content -->
                    <div class="original-post-content">
                        <h6 class="mb-2 fw-bold">${post.originalPost.title}</h6>
                        <div class="mb-2 text-muted">${post.originalPost.content}</div>
                        
                        <!-- Original Post Documents -->
                        ${post.originalPost.documents && post.originalPost.documents.length > 0 ? `
                            <div class="mt-2">
                                <small class="text-muted">
                                    <i class="fa fa-paperclip me-1"></i>
                                    <span>${post.originalPost.documents.length} tệp đính kèm</span>
                                </small>
                            </div>
                        ` : ''}
                        
                        <!-- Original Post Stats -->
                        <div class="mt-2 d-flex gap-3">
                            <small class="text-muted">
                                <i class="fa fa-thumbs-up me-1"></i>
                                <span>${post.originalPost.likeCount || 0} lượt thích</span>
                            </small>
                            <small class="text-muted">
                                <i class="fa fa-comment me-1"></i>
                                <span>${post.originalPost.commentCount || 0} bình luận</span>
                            </small>
                            <small class="text-muted">
                                <i class="fa fa-share me-1"></i>
                                <span>${post.originalPost.shareCount || 0} lượt chia sẻ</span>
                            </small>
                        </div>
                    </div>
                    
                    <!-- View Original Button -->
                    <div class="mt-2">
                        <a href="/posts/${post.originalPost.id}" class="btn btn-sm btn-outline-primary">
                            <i class="fa fa-external-link-alt me-1"></i>Xem bài viết gốc
                        </a>
                    </div>
                </div>
            </div>
        `;
    } else {
        // Regular post content
        sharedPostContent = `
            <h5 class="card-title mb-2">${post.title}</h5>
            <div class="card-text">${post.content}</div>
            ${documentsHtml}
        `;
    }
    
    div.innerHTML = `
        <!-- Header -->
        <div class="card-header bg-white border-0 d-flex align-items-center justify-content-between">
            <div class="d-flex align-items-center gap-2">
                <a href="/profile/${post.userName}" class="text-decoration-none">
                    <img src="${post.userAvatar || 'https://cdn.pixabay.com/photo/2023/02/18/11/00/icon-7797704_640.png'}"
                         class="rounded-circle" width="40" height="40" alt="avatar"
                         onerror="this.src='https://cdn.pixabay.com/photo/2023/02/18/11/00/icon-7797704_640.png'"
                         style="cursor: pointer;">
                </a>
                <div>
                    <div class="d-flex align-items-center gap-2">
                        <strong>${post.userName}</strong>
                        ${groupBadge}
                        ${recommendationBadge}
                    </div>
                    <div class="text-muted small">
                        <span>${formattedDate}</span>
                        •
                        <i class="fa ${privacyIcon}"></i>
                    </div>
                </div>
            </div>
            <div class="d-flex align-items-center gap-2">
                ${topicsHtml}
            </div>
        </div>

        <!-- Body -->
        <div class="card-body pt-2">
            ${sharedPostContent}
        </div>

        <!-- Engagement Stats -->
        <div class="card-footer bg-white border-0 pt-0">
            <div class="d-flex justify-content-between text-muted small mb-2">
                <span class="like-count">${post.likeCount || 0} lượt thích</span>
                <div class="d-flex gap-3">
                    <span class="comment-count">${post.commentCount || 0} bình luận</span>
                    <span class="share-count">${post.shareCount || 0} lượt chia sẻ</span>
                </div>
            </div>
        </div>
    `;
    
    return div;
}


function createGroupElement(group) {
    const div = document.createElement('div');
    div.className = 'card mb-4 border-0 shadow-sm group-card';
    div.setAttribute('data-group-id', group.id);
    
    // Format creation date
    const createdDate = new Date(group.createdAt);
    const formattedDate = createdDate.toLocaleDateString('vi-VN', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric'
    });
    
    // Create badges
    let badges = '';
    if (group.memberCount) {
        badges += `<span class="badge bg-primary text-white me-2">${group.memberCount} thành viên</span>`;
    }
    if (group.commonFriendsCount > 0) {
        badges += `<span class="badge bg-success text-white">${group.commonFriendsCount} bạn chung</span>`;
    }
    
    // Create topics display
    let topicsDisplay = '';
    if (group.topics && group.topics.length > 0) {
        topicsDisplay = group.topics.map(topic => 
            `<span class="badge bg-light text-dark me-1 mb-1">#${topic}</span>`
        ).join('');
    } else {
        topicsDisplay = '<span class="text-muted small">Không có chủ đề</span>';
    }
    
    div.innerHTML = `
        <div class="card-body p-3">
            <div class="d-flex align-items-start">
                <div class="group-avatar me-3">
                    <img src="${group.avatar || 'https://cdn.pixabay.com/photo/2016/11/29/08/41/apple-1868496_640.jpg'}" 
                         alt="group avatar" class="rounded-circle" width="50" height="50"
                         onerror="this.src='https://cdn.pixabay.com/photo/2016/11/29/08/41/apple-1868496_640.jpg'">
                </div>
                <div class="flex-grow-1">
                    <div class="d-flex justify-content-between align-items-start mb-2">
                        <h6 class="group-name mb-0">${group.name}</h6>
                        <div class="group-badges">
                            ${badges}
                        </div>
                    </div>
                    
                    <p class="group-description text-muted small mb-2">${group.description || 'Không có mô tả'}</p>
                    
                    <div class="group-meta mb-2">
                        <div class="d-flex align-items-center text-muted small mb-1">
                            <i class="fa fa-calendar me-1"></i>
                            <span>Tạo ngày ${formattedDate}</span>
                        </div>
                        <div class="topics-container">
                            <div class="d-flex flex-wrap">
                                ${topicsDisplay}
                            </div>
                        </div>
                    </div>
                    
                    <div class="group-actions d-flex gap-2">
                        <button class="btn btn-primary btn-sm" onclick="joinGroup(${group.id})">
                            <i class="fa fa-plus me-1"></i>Tham gia
                        </button>
                        <button class="btn btn-outline-secondary btn-sm" onclick="viewGroup(${group.id})">
                            <i class="fa fa-eye me-1"></i>Xem chi tiết
                        </button>
                    </div>
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
    toastManager.success('Đã làm mới gợi ý thành công!');
}


function joinGroup(groupId) {
    // Implement join group functionality
    console.log('Joining group:', groupId);
    toastManager.success('Đã gửi yêu cầu tham gia nhóm!');
}

function viewGroup(groupId) {
    // Navigate to group detail page
    window.location.href = `/groups/${groupId}`;
}

function getFileIcon(fileName) {
    const extension = fileName.split('.').pop().toLowerCase();
    const iconMap = {
        'pdf': 'pdf',
        'doc': 'word',
        'docx': 'word',
        'xls': 'excel',
        'xlsx': 'excel',
        'ppt': 'powerpoint',
        'pptx': 'powerpoint',
        'txt': 'alt',
        'jpg': 'image',
        'jpeg': 'image',
        'png': 'image',
        'gif': 'image',
        'mp4': 'video',
        'avi': 'video',
        'zip': 'archive',
        'rar': 'archive'
    };
    return iconMap[extension] || 'alt';
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



