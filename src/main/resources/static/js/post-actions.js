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

        const response = await fetch(`http://localhost:8080/api/posts/${postId}`, {
            method: 'DELETE',
            headers: headers,
            credentials: 'same-origin'
        });

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

// Global edit function - Use PostModal editPost instead
function editPost(element) {
    const postId = element.getAttribute('data-id') || element.dataset.id || element.dataset.postId;
    if (!postId) {
        console.error('Post ID not found');
        return;
    }

    // Use PostModal's editPost function
    if (window.postModal && typeof window.postModal.editPost === 'function') {
        window.postModal.editPost(postId);
    } else {
        console.error('PostModal not available');
        showErrorToast('Không thể mở chức năng chỉnh sửa');
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
    // Delete buttons - both onclick and event listener approach
    document.querySelectorAll('[onclick*="deletePost"]').forEach(btn => {
        btn.removeAttribute('onclick');
        btn.addEventListener('click', function(e) {
            e.preventDefault();
            deletePost(this);
        });
    });

    // Edit buttons - find by class and onclick
    document.querySelectorAll('.edit-post-btn, [onclick*="editPost"]').forEach(btn => {
        // Remove existing onclick if any
        btn.removeAttribute('onclick');
        btn.addEventListener('click', function(e) {
            e.preventDefault();
            editPost(this);
        });
    });

    // Legacy edit links (old href-based approach)
    document.querySelectorAll('a[href*="/posts/edit/"]').forEach(btn => {
        btn.addEventListener('click', function(e) {
            e.preventDefault();
            // Extract post ID from href
            const href = this.getAttribute('href');
            const postId = href.split('/').pop();
            this.setAttribute('data-id', postId);
            editPost(this);
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
