/**
 * Group Detail Page - JavaScript
 * Tích hợp với các class global: PostManager, FileUploadManager, HashtagManager
 */

// Global variables
let groupId = null;
let groupPostManager = null;
let groupFileUploadManager = null;

// Initialize when DOM is loaded
document.addEventListener('DOMContentLoaded', function() {
    console.log('Group Detail: Initializing...');
    
    // Get group ID from URL
    groupId = getGroupIdFromUrl();
    console.log('Group ID:', groupId);
    
    // Initialize managers
    initializeGroupPostManager();
    initializeGroupFileUploadManager();
    initializeGroupDetail();
    
    console.log('Group Detail: Initialization complete');
});

/**
 * Get group ID from current URL
 */
function getGroupIdFromUrl() {
    const path = window.location.pathname;
    const match = path.match(/\/groups\/(\d+)/);
    return match ? match[1] : null;
}

/**
 * Initialize Group Post Manager
 * Override PostManager để thêm groupId vào post data
 */
function initializeGroupPostManager() {
    console.log('Initializing Group Post Manager...');
    
    // Wait for PostManager to be available
    if (window.postManager) {
        groupPostManager = window.postManager;
        console.log('Using existing PostManager');
    } else {
        // Create new PostManager instance
        groupPostManager = new PostManager();
        console.log('Created new PostManager');
    }
    
    // Override submitPost method to include groupId
    if (groupPostManager && groupPostManager.submitPost) {
        const originalSubmitPost = groupPostManager.submitPost.bind(groupPostManager);
        
        groupPostManager.submitPost = async function() {
            console.log('Group Post Manager: Overriding submitPost');
            
            if (this.publishBtn.disabled) return;
            
            // Get selected topics
            const selectedTopics = window.HashtagManager?.getSelected() || [];
            console.log('Selected topics:', selectedTopics);
            
            const postData = {
                title: this.titleInput.value.trim(),
                content: this.contentInput.value.trim(),
                topicNames: selectedTopics,
                privacy: this.currentPrivacy,
                groupId: groupId  // Add groupId here
            };
            
            console.log('Post data with groupId:', postData);
            
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
                    console.log('Post created successfully:', result);
                    
                    // Upload files if any are selected
                    if (window.fileUploadManager && window.fileUploadManager.selectedFiles.length > 0) {
                        try {
                            console.log('Uploading files for post ID:', result.id);
                            await window.fileUploadManager.uploadFiles(result.id);
                            console.log('File upload completed successfully');
                            this.showToast('File đã được đính kèm thành công!', 'success');
                        } catch (fileError) {
                            console.warn('Failed to upload files:', fileError);
                            this.showToast('Bài viết đã được đăng nhưng có lỗi khi đính kèm file', 'warning');
                        }
                    }
                    
                    // Save topics
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
                console.error('Error submitting post:', error);
                this.showToast('Không thể kết nối đến máy chủ', 'error');
            } finally {
                this.setLoading(false);
            }
        };
        
        console.log('Group Post Manager: submitPost method overridden');
    }
}

/**
 * Initialize Group File Upload Manager
 * Sử dụng FileUploadManager global
 */
function initializeGroupFileUploadManager() {
    console.log('Initializing Group File Upload Manager...');
    
    if (window.fileUploadManager) {
        groupFileUploadManager = window.fileUploadManager;
        console.log('Using existing FileUploadManager');
    } else {
        console.warn('FileUploadManager not available');
    }
}

/**
 * Initialize Group Detail specific functionality
 */
function initializeGroupDetail() {
    console.log('Initializing Group Detail...');
    
    // Initialize post interactions
    initializePostInteractions();
    
    // Initialize gallery
    initializeGallery();
    
    // Initialize group actions
    initializeGroupActions();
    
    console.log('Group Detail: All components initialized');
}

/**
 * Initialize Post Interactions
 * Sử dụng PostInteractions class
 */
function initializePostInteractions() {
    console.log('Initializing Post Interactions...');
    
    if (window.postInteractions) {
        console.log('Using existing PostInteractions');
    } else {
        console.warn('PostInteractions not available');
    }
}

/**
 * Initialize Gallery
 * Sử dụng GalleryManager
 */
function initializeGallery() {
    console.log('Initializing Gallery...');
    
    if (window.galleryManager) {
        console.log('Using existing GalleryManager');
        } else {
        console.warn('GalleryManager not available');
    }
    
    // Initialize gallery event listeners for group posts
    initializeGroupGalleryEvents();
}

/**
 * Initialize Gallery Events for Group Posts
 */
function initializeGroupGalleryEvents() {
    console.log('Initializing Group Gallery Events...');
    
    // Wait a bit for DOM to be fully rendered
    setTimeout(() => {
        // Add click handlers for gallery items
        document.querySelectorAll('.gallery-image').forEach((item, index) => {
            // Remove any existing listeners to avoid duplicates
            item.replaceWith(item.cloneNode(true));
        });
        
        // Re-query after cloning
        document.querySelectorAll('.gallery-image').forEach((item, index) => {
            item.addEventListener('click', function(e) {
            e.preventDefault();
                e.stopPropagation();
                const postId = this.closest('[data-post-id]').dataset.postId;
                console.log('Gallery image clicked:', postId, index);
                // Call the global function from gallery-manager.js
                if (typeof openLightbox === 'function') {
                    openLightbox(postId, index);
            } else {
                    console.warn('openLightbox function not available');
            }
        });
    });
        
        // Add click handlers for more items indicator
        document.querySelectorAll('.more-items-indicator').forEach((item) => {
            // Remove any existing listeners to avoid duplicates
            item.replaceWith(item.cloneNode(true));
        });
        
        // Re-query after cloning
        document.querySelectorAll('.more-items-indicator').forEach((item) => {
            item.addEventListener('click', function(e) {
                e.preventDefault();
                e.stopPropagation();
                const postId = this.closest('[data-post-id]').dataset.postId;
                console.log('More items clicked:', postId);
                // Call the global function from gallery-manager.js
                if (typeof showAllFiles === 'function') {
                    showAllFiles(postId);
                } else {
                    console.warn('showAllFiles function not available');
                }
            });
        });
        
        console.log('Group Gallery Events: Initialized');
    }, 100);
}

/**
 * Initialize Group Actions
 */
function initializeGroupActions() {
    console.log('Initializing Group Actions...');
    
    // Join Group
    window.joinGroup = async function() {
        if (!groupId) {
            console.error('Group ID not found');
            return;
        }
        
        try {
            const response = await authenticatedFetch(`/api/groups/${groupId}/join`, {
                method: 'POST',
                headers: { 'Accept': 'application/json' }
            });
            
            if (response.ok) {
                showToast('Đã tham gia nhóm thành công!', 'success');
                setTimeout(() => window.location.reload(), 1000);
            } else {
                const error = await response.json().catch(() => ({ message: 'Có lỗi xảy ra' }));
                showToast(error.message || 'Không thể tham gia nhóm', 'error');
            }
        } catch (error) {
            console.error('Error joining group:', error);
            showToast('Có lỗi xảy ra khi tham gia nhóm', 'error');
        }
    };
    
    // Leave Group
    window.leaveGroup = async function() {
        if (!groupId) {
            console.error('Group ID not found');
            return;
        }
        
    if (!confirm('Bạn có chắc chắn muốn rời nhóm này?')) {
        return;
    }
    
        try {
            const response = await authenticatedFetch(`/api/groups/${groupId}/leave`, {
                method: 'POST',
                headers: { 'Accept': 'application/json' }
            });
            
            if (response.ok) {
                showToast('Đã rời nhóm thành công!', 'success');
                setTimeout(() => window.location.reload(), 1000);
            } else {
                const error = await response.json().catch(() => ({ message: 'Có lỗi xảy ra' }));
                showToast(error.message || 'Không thể rời nhóm', 'error');
            }
        } catch (error) {
            console.error('Error leaving group:', error);
            showToast('Có lỗi xảy ra khi rời nhóm', 'error');
        }
    };
    
    console.log('Group Actions: joinGroup and leaveGroup functions defined');
}

/**
 * Show Toast Notification
 */
function showToast(message, type = 'info') {
    if (window.toastManager) {
        window.toastManager.show(message, type);
        } else {
        console.log(`Toast (${type}): ${message}`);
    }
}

/**
 * Global functions for backward compatibility
 */
window.toggleLike = function(element) {
    if (window.postManager) {
        const postId = window.postManager.getPostId(element);
        if (postId) {
            window.postManager.toggleLike(postId);
        }
    }
};

window.sharePost = function(element) {
    if (window.postManager) {
        const postId = window.postManager.getPostId(element);
        if (postId) {
            window.postManager.sharePost(postId);
        }
    }
};

window.deletePost = function(element) {
    if (window.postManager) {
        const postId = window.postManager.getPostId(element);
        if (postId) {
            window.postManager.deletePost(postId);
        }
    }
};

window.editPost = function(element) {
    if (window.postManager) {
        const postId = window.postManager.getPostId(element);
        if (postId) {
            window.postManager.editPost(postId);
        }
    }
};

// Gallery functions - These will be available from gallery-manager.js
// No need to redefine them here as they are global functions

// Download all files function
window.downloadAllFiles = function(postId) {
    console.log('downloadAllFiles called:', postId);
    // This function is defined in gallery-manager.js
    if (typeof downloadAllFiles === 'function') {
        downloadAllFiles(postId);
        } else {
        console.warn('downloadAllFiles function not available');
    }
};

// Comment functions
window.toggleCommentSection = function(element) {
    const postId = element.getAttribute('data-post-id');
    const commentSection = document.querySelector(`[data-post-id="${postId}"].comment-section`);
    
    if (commentSection) {
        commentSection.classList.toggle('d-none');
    }
};

window.postComment = function(element) {
    const postId = element.getAttribute('data-post-id');
    const commentInput = document.querySelector(`[data-post-id="${postId}"].comment-input`);
    const commentText = commentInput.value.trim();
    
    if (!commentText) return;
    
    // TODO: Implement comment posting
    console.log('Posting comment:', commentText, 'for post:', postId);
    commentInput.value = '';
};

window.loadMoreComments = function(element) {
    const postId = element.getAttribute('data-post-id');
    console.log('Loading more comments for post:', postId);
    // TODO: Implement load more comments
};

// Hashtag search
window.searchByHashtag = function(hashtag) {
    console.log('Searching by hashtag:', hashtag);
    // TODO: Implement hashtag search
};

// Smart Filter Functions
function initializeSmartFilters() {
    console.log('Initializing smart filters...');
    
    // Members filter
    initializeMembersFilter();
    
    // Documents filter
    initializeDocumentsFilter();
}

function initializeMembersFilter() {
    const searchInput = document.getElementById('memberSearch');
    const roleFilter = document.getElementById('roleFilter');
    const sortFilter = document.getElementById('memberSortFilter');
    const clearBtn = document.getElementById('clearFilters');
    const quickFilters = document.querySelectorAll('.quick-filter');
    const membersList = document.getElementById('membersList');
    
    if (!membersList) return;
    
    const memberItems = Array.from(membersList.querySelectorAll('.member-item'));
    
    function filterMembers() {
        const searchTerm = searchInput.value.toLowerCase();
        const roleValue = roleFilter.value;
        const sortValue = sortFilter.value;
        
        let filtered = memberItems.filter(item => {
            const name = item.dataset.name.toLowerCase();
            const username = item.dataset.username.toLowerCase();
            const role = item.dataset.role;
            
            const matchesSearch = name.includes(searchTerm) || username.includes(searchTerm);
            const matchesRole = !roleValue || role === roleValue;
            
            return matchesSearch && matchesRole;
        });
        
        // Sort
        filtered.sort((a, b) => {
            switch(sortValue) {
                case 'name':
                    return a.dataset.name.localeCompare(b.dataset.name);
                case 'joinDate':
                    return new Date(b.dataset.joinDate) - new Date(a.dataset.joinDate);
                case 'activity':
                    // Mock activity - in real app, this would be based on actual data
                    return Math.random() - 0.5;
                default:
                    return 0;
            }
        });
        
        // Hide all items
        memberItems.forEach(item => item.style.display = 'none');
        
        // Show filtered items
        filtered.forEach(item => item.style.display = 'flex');
    }
    
    // Event listeners
    if (searchInput) searchInput.addEventListener('input', filterMembers);
    if (roleFilter) roleFilter.addEventListener('change', filterMembers);
    if (sortFilter) sortFilter.addEventListener('change', filterMembers);
    
    if (clearBtn) {
        clearBtn.addEventListener('click', () => {
            searchInput.value = '';
            roleFilter.value = '';
            sortFilter.value = 'name';
            quickFilters.forEach(btn => btn.classList.remove('active'));
            memberItems.forEach(item => item.style.display = 'flex');
        });
    }
    
    // Quick filters
    quickFilters.forEach(btn => {
        btn.addEventListener('click', () => {
            quickFilters.forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
            
            const filter = btn.dataset.filter;
            switch(filter) {
                case 'admin':
                    roleFilter.value = 'ADMIN';
                    break;
                case 'active':
                    sortFilter.value = 'activity';
                    break;
                case 'new':
                    sortFilter.value = 'joinDate';
                    break;
            }
            filterMembers();
        });
    });
}

function initializeDocumentsFilter() {
    const searchInput = document.getElementById('documentSearch');
    const typeFilter = document.getElementById('typeFilter');
    const sortFilter = document.getElementById('documentSortFilter');
    const clearBtn = document.getElementById('clearDocFilters');
    const quickFilters = document.querySelectorAll('.quick-doc-filter');
    const documentsList = document.getElementById('documentsList');
    
    if (!documentsList) return;
    
    const documentItems = Array.from(documentsList.querySelectorAll('.document-item'));
    
    function filterDocuments() {
        const searchTerm = searchInput.value.toLowerCase();
        const typeValue = typeFilter.value;
        const sortValue = sortFilter.value;
        
        let filtered = documentItems.filter(item => {
            const name = item.dataset.name.toLowerCase();
            const type = item.dataset.type;
            
            const matchesSearch = name.includes(searchTerm);
            const matchesType = !typeValue || type === typeValue;
            
            return matchesSearch && matchesType;
        });
        
        // Sort
        filtered.sort((a, b) => {
            switch(sortValue) {
                case 'name':
                    return a.dataset.name.localeCompare(b.dataset.name);
                case 'date':
                    return new Date(b.dataset.date) - new Date(a.dataset.date);
                case 'size':
                    return parseFloat(b.dataset.size) - parseFloat(a.dataset.size);
                case 'downloads':
                    // Mock downloads - in real app, this would be based on actual data
                    return Math.random() - 0.5;
                default:
                    return 0;
            }
        });
        
        // Hide all items
        documentItems.forEach(item => item.style.display = 'none');
        
        // Show filtered items
        filtered.forEach(item => item.style.display = 'flex');
    }
    
    // Event listeners
    if (searchInput) searchInput.addEventListener('input', filterDocuments);
    if (typeFilter) typeFilter.addEventListener('change', filterDocuments);
    if (sortFilter) sortFilter.addEventListener('change', filterDocuments);
    
    if (clearBtn) {
        clearBtn.addEventListener('click', () => {
            searchInput.value = '';
            typeFilter.value = '';
            sortFilter.value = 'name';
            quickFilters.forEach(btn => btn.classList.remove('active'));
            documentItems.forEach(item => item.style.display = 'flex');
        });
    }
    
    // Quick filters
    quickFilters.forEach(btn => {
        btn.addEventListener('click', () => {
            quickFilters.forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
            
            const filter = btn.dataset.filter;
            if (filter === 'recent') {
                sortFilter.value = 'date';
            } else {
                typeFilter.value = filter;
            }
            filterDocuments();
        });
    });
}

// Initialize smart filters when DOM is ready
document.addEventListener('DOMContentLoaded', function() {
    initializeSmartFilters();
});

console.log('Group Detail JS: Loaded successfully');
