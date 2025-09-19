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
        console.log('Initializing PostInteractions from group-detail.js...');
        window.postInteractions = new PostInteractions();
    } else {
        console.log('PostInteractions already exists in group-detail.js');
    }
}

/**
 * Configure PostManager để thêm groupId vào post data
 */
function overridePostManagerForGroup() {
    // Đợi PostManager được khởi tạo
    setTimeout(() => {
        if (window.postManager) {
            // Set groupId for this group
            window.postManager.setGroupId(groupId);
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
                toastManager.success('Đã tham gia nhóm thành công!');
                setTimeout(() => window.location.reload(), 1000);
            } else {
                const error = await response.json().catch(() => ({ message: 'Có lỗi xảy ra' }));
                toastManager.error(error.message || 'Không thể tham gia nhóm');
            }
        } catch (error) {
            toastManager.error('Có lỗi xảy ra khi tham gia nhóm');
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
                toastManager.success('Đã rời nhóm thành công!');
                setTimeout(() => window.location.reload(), 1000);
            } else {
                const error = await response.json().catch(() => ({ message: 'Có lỗi xảy ra' }));
                toastManager.error(error.message || 'Không thể rời nhóm');
            }
        } catch (error) {
            toastManager.error('Có lỗi xảy ra khi rời nhóm');
        }
    };
    
}


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
    const paginationContainer = document.getElementById('membersPagination');
    
    if (!membersList) return;
    
    let currentPage = 0;
    let currentSearch = '';
    let currentRole = '';
    let currentSort = 'name';
    
    // Load members with current filters
    function loadMembers(page = 0) {
        const params = new URLSearchParams({
            search: currentSearch,
            role: currentRole,
            sortBy: currentSort,
            page: page,
            size: 10
        });
        
        showLoading();
        
        authenticatedFetch(`/groups/${groupId}/members?${params}`)
            .then(response => {
                if (!response.ok) {
                    throw new Error(`HTTP error! status: ${response.status}`);
                }
                return response.json();
            })
            .then(data => {
                console.log('Members data received:', data);
                if (data && data.content) {
                    // Set current user ID from response
                    if (data.currentUserId) {
                        window.currentUserId = data.currentUserId;
                    }
                    displayMembers(data.content);
                    updatePagination(data);
                } else {
                    console.error('Invalid data structure:', data);
                    displayMembers([]);
                }
                hideLoading();
            })
            .catch(error => {
                console.error('Error loading members:', error);
                toastManager.error('Không thể tải danh sách thành viên: ' + error.message);
                displayMembers([]);
                hideLoading();
            });
    }
    
    // Display members
    function displayMembers(members) {
        membersList.innerHTML = '';
        
        if (members.length === 0) {
            membersList.innerHTML = `
                <div class="col-12 text-center text-muted py-4">
                    <i class="fa fa-users fa-3x mb-3"></i>
                    <p>Không tìm thấy thành viên nào</p>
                </div>
            `;
            return;
        }
        
        // Create grid container
        const gridContainer = document.createElement('div');
        gridContainer.className = 'row g-3';
        
        // For activity sorting, we need to get post counts from server
        if (currentSort === 'activity') {
            // Server already handles activity sorting, just display
            members.forEach(member => {
                const memberElement = createMemberElement(member);
                gridContainer.appendChild(memberElement);
            });
        } else {
            // For other sorting, do client-side sorting
            const sortedMembers = sortMembers(members, currentSort);
            sortedMembers.forEach(member => {
                const memberElement = createMemberElement(member);
                gridContainer.appendChild(memberElement);
            });
        }
        
        membersList.appendChild(gridContainer);
    }
    
    // Sort members based on sort option
    function sortMembers(members, sortBy) {
        const sorted = [...members];
        
            switch(sortBy) {
                case 'name':
                    return sorted.sort((a, b) => {
                        const nameA = (a.fullName || `${a.firstName || ''} ${a.lastName || ''}`.trim() || a.username).toLowerCase();
                        const nameB = (b.fullName || `${b.firstName || ''} ${b.lastName || ''}`.trim() || b.username).toLowerCase();
                        return nameA.localeCompare(nameB);
                    });
                case 'joinedAt':
                    return sorted.sort((a, b) => new Date(b.joinedAt) - new Date(a.joinedAt));
                case 'activity':
                    return sorted.sort((a, b) => {
                        const activityA = a.postCount || 0;
                        const activityB = b.postCount || 0;
                        if (activityA === activityB) {
                            return new Date(b.joinedAt) - new Date(a.joinedAt);
                        }
                        return activityB - activityA;
                    });
                default:
                    return sorted;
            }
    }
    
    // Create member element
    function createMemberElement(member) {
        const col = document.createElement('div');
        col.className = 'col-md-4 col-lg-3 col-xl-2';
        
        const fullName = member.fullName || `${member.firstName || ''} ${member.lastName || ''}`.trim() || member.username;
        const joinDate = new Date(member.joinedAt).toLocaleDateString('vi-VN');
        const postCount = member.postCount || 0;
        
        // Check if this is current user
        const isCurrentUser = member.userId === getCurrentUserId();
        const currentUserIndicator = isCurrentUser ? 
            '<div class="position-absolute top-0 end-0 m-1"><span class="badge bg-warning text-dark" style="font-size: 0.6rem;">Tôi</span></div>' : '';
        
        col.innerHTML = `
            <div class="card member-item h-100 member-card-clickable position-relative" 
                 data-name="${fullName.toLowerCase()}" 
                 data-username="${member.username.toLowerCase()}" 
                 data-role="${member.role}"
                 data-activity="${postCount}"
                 data-user-id="${member.userId}"
                 style="cursor: pointer; transition: all 0.2s ease; ${isCurrentUser ? 'border: 2px solid #ffc107;' : ''}">
                ${currentUserIndicator}
                <div class="card-body text-center p-3">
                    <img src="${member.avatar}" alt="${member.username}" 
                         class="rounded-circle mb-2" width="45" height="45">
                    <h6 class="card-title mb-1 small" style="font-size: 0.85rem; ${isCurrentUser ? 'color: #ffc107; font-weight: bold;' : ''}">${fullName}</h6>
                    <p class="text-muted small mb-1" style="font-size: 0.75rem;">@${member.username}</p>
                    <span class="badge bg-${member.role === 'ADMIN' ? 'primary' : 'secondary'} mb-2" style="font-size: 0.7rem;">
                        ${member.role === 'ADMIN' ? 'Admin' : 'Member'}
                    </span>
                    <div class="member-info">
                        <p class="text-muted small mb-1" style="font-size: 0.7rem;">
                            <i class="fa fa-calendar me-1"></i>
                            ${joinDate}
                        </p>
                        <p class="text-muted small mb-0" style="font-size: 0.7rem;">
                            <i class="fa fa-pencil me-1"></i>
                            ${postCount} bài
                        </p>
                    </div>
                </div>
            </div>
        `;
        
        // Add click event to navigate to profile
        const card = col.querySelector('.member-card-clickable');
        card.addEventListener('click', () => {
            window.location.href = `/profile/${member.username}`;
        });
        
        // Add hover effects
        card.addEventListener('mouseenter', () => {
            card.style.transform = 'translateY(-2px)';
            card.style.boxShadow = '0 4px 8px rgba(0,0,0,0.1)';
        });
        
        card.addEventListener('mouseleave', () => {
            card.style.transform = 'translateY(0)';
            card.style.boxShadow = '0 2px 4px rgba(0,0,0,0.1)';
        });
        
        return col;
    }
    
    // Update pagination
    function updatePagination(data) {
        if (!paginationContainer) return;
        
        const { totalPages, currentPage: page, first, last } = data;
        
        if (totalPages <= 1) {
            paginationContainer.innerHTML = '';
            return;
        }
        
        let paginationHTML = '<nav><ul class="pagination justify-content-center">';
        
        // Previous button
        paginationHTML += `
            <li class="page-item ${first ? 'disabled' : ''}">
                <a class="page-link" href="#" onclick="changePage(${page - 1})">Trước</a>
            </li>
        `;
        
        // Page numbers
        const startPage = Math.max(0, page - 2);
        const endPage = Math.min(totalPages - 1, page + 2);
        
        for (let i = startPage; i <= endPage; i++) {
            paginationHTML += `
                <li class="page-item ${i === page ? 'active' : ''}">
                    <a class="page-link" href="#" onclick="changePage(${i})">${i + 1}</a>
                </li>
            `;
        }
        
        // Next button
        paginationHTML += `
            <li class="page-item ${last ? 'disabled' : ''}">
                <a class="page-link" href="#" onclick="changePage(${page + 1})">Sau</a>
            </li>
        `;
        
        paginationHTML += '</ul></nav>';
        paginationContainer.innerHTML = paginationHTML;
    }
    
    // Change page function (global)
    window.changePage = function(page) {
        currentPage = page;
        loadMembers(page);
    };
    
    // Filter function
    function applyFilters() {
        currentPage = 0;
        currentSearch = searchInput ? searchInput.value : '';
        currentRole = roleFilter ? roleFilter.value : '';
        currentSort = sortFilter ? sortFilter.value : 'name';
        loadMembers();
    }
    
    // Event listeners
    if (searchInput) {
        let searchTimeout;
        searchInput.addEventListener('input', () => {
            clearTimeout(searchTimeout);
            searchTimeout = setTimeout(applyFilters, 500); // Debounce
        });
    }
    
    if (roleFilter) roleFilter.addEventListener('change', applyFilters);
    if (sortFilter) sortFilter.addEventListener('change', applyFilters);
    
    if (clearBtn) {
        clearBtn.addEventListener('click', () => {
            if (searchInput) searchInput.value = '';
            if (roleFilter) roleFilter.value = '';
            if (sortFilter) sortFilter.value = 'name';
            quickFilters.forEach(btn => btn.classList.remove('active'));
            applyFilters();
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
                    if (roleFilter) roleFilter.value = 'ADMIN';
                    if (sortFilter) sortFilter.value = 'name';
                    break;
                case 'active':
                    if (sortFilter) sortFilter.value = 'activity';
                    if (roleFilter) roleFilter.value = '';
                    break;
                case 'new':
                    if (sortFilter) sortFilter.value = 'joinedAt';
                    if (roleFilter) roleFilter.value = '';
                    break;
                case 'all':
                    if (roleFilter) roleFilter.value = '';
                    if (sortFilter) sortFilter.value = 'name';
                    break;
            }
            applyFilters();
        });
    });
    
    // Load initial data
    loadMembers();
}

// Helper function to get current user ID
function getCurrentUserId() {
    // Try to get from JWT token or global variable
    if (typeof window.currentUserId !== 'undefined') {
        return window.currentUserId;
    }
    
    // Try to get from JWT utils if available
    if (typeof JwtUtils !== 'undefined' && JwtUtils.getUserId) {
        return JwtUtils.getUserId();
    }
    
    // Try to get from meta tag
    const metaUserId = document.querySelector('meta[name="current-user-id"]');
    if (metaUserId) {
        return parseInt(metaUserId.getAttribute('content'));
    }
    
    // Fallback: try to get from data attribute
    const bodyUserId = document.body.getAttribute('data-current-user-id');
    if (bodyUserId) {
        return parseInt(bodyUserId);
    }
    
    return null;
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
        const response = await authenticatedFetch('/api/chat/private-chat', {
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

// Use global toastManager for notifications
function showToast(message, type = 'info') {
    if (window.toastManager) {
        window.toastManager.show(message, type);
    } else {
        console.warn('ToastManager not available, falling back to console');
        console.log(`[${type.toUpperCase()}] ${message}`);
    }
}

// Loading states
function showLoading() {
    const loadingElement = document.getElementById('loadingIndicator');
    if (loadingElement) {
        loadingElement.style.display = 'block';
    }
}

function hideLoading() {
    const loadingElement = document.getElementById('loadingIndicator');
    if (loadingElement) {
        loadingElement.style.display = 'none';
    }
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

