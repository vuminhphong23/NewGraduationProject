/**
 * Group Detail Page - JavaScript
 * Sử dụng các class global có sẵn: PostManager, PostInteractions, FileUploadManager
 */

// Global variables
let groupId = null;

// Initialize when DOM is loaded
document.addEventListener('DOMContentLoaded', function() {
    // Get group ID from URL
    groupId = getGroupIdFromUrl();
    
    // Initialize group-specific functionality
    initializeGroupDetail();
    
    // Initialize smart filters
    initializeSmartFilters();
});

/**
 * Get group ID from current URL
 */
function getGroupIdFromUrl() {
    const path = window.location.pathname;
    const match = path.match(/\/groups\/(\d+)/);
    return match ? match[1] : null;
}

// PostManager và FileUploadManager đã được khởi tạo trong các file riêng

/**
 * Initialize Group Detail specific functionality
 */
function initializeGroupDetail() {
    // Override PostManager để thêm groupId
    overridePostManagerForGroup();
    
    // Initialize group actions
    initializeGroupActions();
    
    // Đảm bảo PostInteractions được khởi tạo
    if (!window.postInteractions) {
        window.postInteractions = new PostInteractions();
    }
}

/**
 * Override PostManager để thêm groupId vào post data
 */
function overridePostManagerForGroup() {
    // Đợi PostManager được khởi tạo
    setTimeout(() => {
        if (window.postManager && window.postManager.submitPost) {
            const originalSubmitPost = window.postManager.submitPost.bind(window.postManager);
            
            window.postManager.submitPost = async function() {
                if (this.publishBtn.disabled) return;
                
                // Get selected topics
                const selectedTopics = window.HashtagManager?.getSelected() || [];
                
                const postData = {
                    title: this.titleInput.value.trim(),
                    content: this.contentInput.value.trim(),
                    topicNames: selectedTopics,
                    privacy: this.currentPrivacy,
                    groupId: groupId  // Add groupId here
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
                        if (window.fileUploadManager && window.fileUploadManager.selectedFiles.length > 0) {
                            try {
                                await window.fileUploadManager.uploadFiles(result.id);
                                this.showToast('File đã được đính kèm thành công!', 'success');
                            } catch (fileError) {
                                this.showToast('Bài viết đã được đăng nhưng có lỗi khi đính kèm file', 'warning');
                            }
                        }
                        
                        // Save topics
                        if (selectedTopics.length > 0 && window.HashtagManager && typeof window.HashtagManager.saveTopics === 'function') {
                            try {
                                await window.HashtagManager.saveTopics(selectedTopics);
                            } catch (topicError) {
                                // Ignore topic save errors
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
            };
        }
    }, 100);
}

/**
 * Initialize Group Actions
 */
function initializeGroupActions() {
    
    // Join Group
    window.joinGroup = async function() {
        if (!groupId) {
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
            showToast('Có lỗi xảy ra khi tham gia nhóm', 'error');
        }
    };
    
    // Leave Group
    window.leaveGroup = async function() {
        if (!groupId) {
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
            showToast('Có lỗi xảy ra khi rời nhóm', 'error');
        }
    };
    
}

// Tất cả các chức năng post (like, comment, share) đã được định nghĩa trong post-interactions.js
// Không cần định nghĩa lại ở đây

// Smart Filter Functions
function initializeSmartFilters() {
    
    // Members filter
    initializeMembersFilter();
    
    // Documents filter - with delay to ensure DOM is fully rendered
    setTimeout(() => {
        initializeDocumentsFilter();
    }, 100);
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
                case 'joinedAt':
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
                    sortFilter.value = 'joinedAt';
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
    
    
    if (!documentsList) {
        return;
    }
    
    const documentItems = Array.from(documentsList.querySelectorAll('.document-item'));
    
    
    function filterDocuments() {
        const searchTerm = searchInput.value.toLowerCase();
        const typeValue = typeFilter.value;
        const sortValue = sortFilter.value;
        
        let filtered = documentItems.filter(item => {
            const name = item.dataset.name.toLowerCase();
            const type = item.dataset.type;
            
            const matchesSearch = name.includes(searchTerm);
            let matchesType = true;
            
            if (typeValue && typeValue !== 'all') {
                if (typeValue === 'pdf') {
                    matchesType = type === '.pdf' || type === 'pdf';
                } else if (typeValue === 'doc') {
                    matchesType = type === '.doc' || type === 'doc' || type === '.docx' || type === 'docx';
                } else if (typeValue === 'xls') {
                    matchesType = type === '.xls' || type === 'xls' || type === '.xlsx' || type === 'xlsx';
                } else if (typeValue === 'ppt') {
                    matchesType = type === '.ppt' || type === 'ppt' || type === '.pptx' || type === 'pptx';
                } else if (typeValue === 'image') {
                    matchesType = type === '.jpg' || type === 'jpg' || 
                                 type === '.jpeg' || type === 'jpeg' || 
                                 type === '.png' || type === 'png' || 
                                 type === '.gif' || type === 'gif';
                }
            }
            
            
            return matchesSearch && matchesType;
        });
        
        // Sort
        filtered.sort((a, b) => {
            switch(sortValue) {
                case 'name':
                    return a.dataset.name.localeCompare(b.dataset.name);
                case 'date':
                    // Parse date from dd/MM/yyyy format
                    const parseDate = (dateStr) => {
                        if (!dateStr) return new Date(0);
                        const [day, month, year] = dateStr.split('/');
                        return new Date(year, month - 1, day);
                    };
                    return parseDate(b.dataset.date) - parseDate(a.dataset.date);
                case 'size':
                    return parseFloat(b.dataset.size) - parseFloat(a.dataset.size);
                case 'downloads':
                    // Mock downloads - in real app, this would be based on actual data
                    return Math.random() - 0.5;
                default:
                    return 0;
            }
        });
        
        // For recent filter, limit to 10 most recent files
        if (typeValue === 'all' && sortValue === 'date') {
            filtered = filtered.slice(0, 10);
        }
        
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
                    // For recent, set type filter to all and sort by date
                    typeFilter.value = 'all';
                    sortFilter.value = 'date';
                } else {
                    // For other filters, set type filter and reset sort
                    typeFilter.value = filter;
                    sortFilter.value = 'name';
                }
                filterDocuments();
            });
        });
}

// Smart filters are initialized in the main DOMContentLoaded listener

// Global view function
function viewFile(url) {
    // Open file in new tab
    window.open(url, '_blank');
}

// Download functions are now handled by download-utils.js

// Start chat with user
async function startChatWithUser(userId) {
    try {
        const response = await fetch('/api/chat/private-chat', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({ userId: userId })
        });
        
        if (response.ok) {
            const result = await response.json();
            if (result.success) {
                // Redirect to chat page with the room ID
                window.location.href = `/chat?room=${result.data.id}`;
            } else {
                alert('Không thể tạo cuộc trò chuyện: ' + result.message);
            }
        } else {
            alert('Có lỗi xảy ra khi tạo cuộc trò chuyện');
        }
    } catch (error) {
        console.error('Error starting chat:', error);
        alert('Có lỗi xảy ra khi tạo cuộc trò chuyện');
    }
}

// View user profile
function viewUserProfile(username) {
    window.location.href = `/profile/${username}`;
}

// Switch to specific tab
function switchToTab(tabName) {
    
    // Remove active class from all tabs
    document.querySelectorAll('.nav-link').forEach(tab => {
        tab.classList.remove('active');
    });
    
    // Add active class to target tab
    const targetTab = document.querySelector(`a[href*="tab=${tabName}"]`);
    if (targetTab) {
        targetTab.classList.add('active');
    }
    
    // Hide all tab content
    document.querySelectorAll('.tab-content').forEach(content => {
        content.classList.remove('active');
    });
    
    // Show target tab content
    const targetContent = document.getElementById(tabName);
    if (targetContent) {
        targetContent.classList.add('active');
    }
    
    // Update URL without page reload
    const url = new URL(window.location);
    url.searchParams.set('tab', tabName);
    window.history.pushState({}, '', url);
}

