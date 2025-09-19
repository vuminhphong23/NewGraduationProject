document.addEventListener('DOMContentLoaded', () => {
    const postId = document.querySelector('[data-post-id]')?.getAttribute('data-post-id');
    const commentInput = document.querySelector('.comment-input');
    const commentButton = document.querySelector('.post-comment-btn');
    const commentsList = document.querySelector('.comments-list');

    // Hàm chung submit comment
    const handleCommentSubmit = () => {
        const content = commentInput?.value.trim();
        if (content && window.postInteractions) {
            window.postInteractions.postComment(commentInput);
        }
    };

    // Đảm bảo PostInteractions được khởi tạo và load like status
    let retryCount = 0;
    const maxRetries = 20; // Maximum 1 second wait (20 * 50ms)
    
    const initializePostInteractions = () => {
        if (window.postInteractions) {
            // Load like status cho post này
            const likeButton = document.querySelector('.like-btn');
            if (likeButton) {
                window.postInteractions.loadLikeStatus(likeButton);
            }
        } else if (retryCount < maxRetries) {
            retryCount++;
            setTimeout(initializePostInteractions, 50);
        } else {
            console.warn('PostInteractions not available after maximum retries');
        }
    };

    // Load comments nếu chưa có
    setTimeout(() => {
        if (window.postInteractions && commentsList && !commentsList.children.length) {
            window.postInteractions.loadComments(postId, 0);
        }
    }, 100);

    // Initialize post interactions với delay để đảm bảo PostInteractions đã được khởi tạo
    setTimeout(() => {
        initializePostInteractions();
    }, 100);

    // Scroll tới comment theo hash
    const hash = window.location.hash;
    if (hash?.startsWith('#comment-')) {
        const commentId = hash.replace('#comment-', '');
        let scrollRetryCount = 0;
        const maxScrollRetries = 10; // Maximum 5 seconds (10 * 500ms)
        
        const checkAndScroll = () => {
            const el = document.querySelector(`[data-comment-id="${commentId}"]`);
            if (el) {
                el.scrollIntoView({ behavior: 'smooth', block: 'center' });
                el.style.backgroundColor = '#fff3cd';
                setTimeout(() => el.style.backgroundColor = '', 2000);
            } else if (scrollRetryCount < maxScrollRetries) {
                scrollRetryCount++;
                setTimeout(checkAndScroll, 500);
            } else {
                console.warn('Comment element not found after maximum retries');
            }
        };
        setTimeout(checkAndScroll, 1500);
    }

    // Enter để gửi comment
    commentInput?.addEventListener('keypress', e => {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            handleCommentSubmit();
        }
    });

    // Click button gửi comment
    commentButton?.addEventListener('click', handleCommentSubmit);
});

// Use existing toast manager
window.showToast = function(message, type = 'info') {
    if (window.toastManager) {
        window.toastManager.show(message, type);
    } else {
        console.log(`Toast: ${message} (${type})`);
    }
};