// Post Management JavaScript
document.addEventListener('DOMContentLoaded', function() {
    initializePostManagement();
});

function initializePostManagement() {
    // Initialize tooltips
    initializeTooltips();
    
    // Initialize bulk actions
    initializeBulkActions();
    
    // Initialize status toggles
    initializeStatusToggles();
    
    // Initialize search functionality
    initializeSearch();
    
    // Initialize confirmation dialogs
    initializeConfirmations();
}

function initializeTooltips() {
    // Initialize Bootstrap tooltips
    var tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
    var tooltipList = tooltipTriggerList.map(function (tooltipTriggerEl) {
        return new bootstrap.Tooltip(tooltipTriggerEl);
    });
}

function initializeBulkActions() {
    const selectAllCheckbox = document.querySelector('thead input[type="checkbox"]');
    const rowCheckboxes = document.querySelectorAll('tbody input[type="checkbox"]');
    
    if (selectAllCheckbox) {
        selectAllCheckbox.addEventListener('change', function() {
            rowCheckboxes.forEach(checkbox => {
                checkbox.checked = this.checked;
            });
            updateBulkActionButtons();
        });
    }
    
    rowCheckboxes.forEach(checkbox => {
        checkbox.addEventListener('change', function() {
            updateSelectAllState();
            updateBulkActionButtons();
        });
    });
}

function updateSelectAllState() {
    const selectAllCheckbox = document.querySelector('thead input[type="checkbox"]');
    const rowCheckboxes = document.querySelectorAll('tbody input[type="checkbox"]');
    const checkedCheckboxes = document.querySelectorAll('tbody input[type="checkbox"]:checked');
    
    if (selectAllCheckbox) {
        if (checkedCheckboxes.length === 0) {
            selectAllCheckbox.indeterminate = false;
            selectAllCheckbox.checked = false;
        } else if (checkedCheckboxes.length === rowCheckboxes.length) {
            selectAllCheckbox.indeterminate = false;
            selectAllCheckbox.checked = true;
        } else {
            selectAllCheckbox.indeterminate = true;
        }
    }
}

function updateBulkActionButtons() {
    const checkedCheckboxes = document.querySelectorAll('tbody input[type="checkbox"]:checked');
    const bulkActionContainer = document.getElementById('bulkActions');
    
    if (bulkActionContainer) {
        if (checkedCheckboxes.length > 0) {
            bulkActionContainer.style.display = 'block';
            bulkActionContainer.querySelector('.selected-count').textContent = checkedCheckboxes.length;
        } else {
            bulkActionContainer.style.display = 'none';
        }
    }
}

function initializeStatusToggles() {
    const statusToggleButtons = document.querySelectorAll('[data-action="toggle-status"]');
    
    statusToggleButtons.forEach(button => {
        button.addEventListener('click', function(e) {
            e.preventDefault();
            const postId = this.dataset.postId;
            const currentStatus = this.dataset.currentStatus;
            
            if (confirm('Bạn có chắc chắn muốn thay đổi trạng thái bài viết này?')) {
                togglePostStatus(postId, currentStatus);
            }
        });
    });
}

function togglePostStatus(postId, currentStatus) {
    const form = document.createElement('form');
    form.method = 'POST';
    form.action = `/admin/posts/${postId}/toggle-status`;
    
    // Add CSRF token if available
    const csrfToken = document.querySelector('meta[name="_csrf"]');
    if (csrfToken) {
        const csrfInput = document.createElement('input');
        csrfInput.type = 'hidden';
        csrfInput.name = csrfToken.getAttribute('content');
        csrfInput.value = csrfToken.getAttribute('content');
        form.appendChild(csrfInput);
    }
    
    document.body.appendChild(form);
    form.submit();
}

function initializeSearch() {
    const searchInput = document.querySelector('input[name="keyword"]');
    const searchForm = document.querySelector('form[action*="/admin/posts"]');
    
    if (searchInput && searchForm) {
        let searchTimeout;
        
        searchInput.addEventListener('input', function() {
            clearTimeout(searchTimeout);
            searchTimeout = setTimeout(() => {
                // Auto-submit form after 500ms of no typing
                if (this.value.length >= 2 || this.value.length === 0) {
                    searchForm.submit();
                }
            }, 500);
        });
    }
}

function initializeConfirmations() {
    const deleteButtons = document.querySelectorAll('[data-action="delete"]');
    
    deleteButtons.forEach(button => {
        button.addEventListener('click', function(e) {
            e.preventDefault();
            const postTitle = this.dataset.postTitle || 'bài viết này';
            
            if (confirm(`Bạn có chắc chắn muốn xóa ${postTitle}? Hành động này không thể hoàn tác.`)) {
                const form = this.closest('form');
                if (form) {
                    form.submit();
                }
            }
        });
    });
}

// Bulk actions
function bulkApprove() {
    const checkedIds = getCheckedPostIds();
    if (checkedIds.length === 0) {
        alert('Vui lòng chọn ít nhất một bài viết để duyệt.');
        return;
    }
    
    if (confirm(`Bạn có chắc chắn muốn duyệt ${checkedIds.length} bài viết đã chọn?`)) {
        performBulkAction('approve', checkedIds);
    }
}

function bulkReject() {
    const checkedIds = getCheckedPostIds();
    if (checkedIds.length === 0) {
        alert('Vui lòng chọn ít nhất một bài viết để từ chối.');
        return;
    }
    
    if (confirm(`Bạn có chắc chắn muốn từ chối ${checkedIds.length} bài viết đã chọn?`)) {
        performBulkAction('reject', checkedIds);
    }
}

function bulkDelete() {
    const checkedIds = getCheckedPostIds();
    if (checkedIds.length === 0) {
        alert('Vui lòng chọn ít nhất một bài viết để xóa.');
        return;
    }
    
    if (confirm(`Bạn có chắc chắn muốn xóa ${checkedIds.length} bài viết đã chọn? Hành động này không thể hoàn tác.`)) {
        performBulkAction('delete', checkedIds);
    }
}

function getCheckedPostIds() {
    const checkedCheckboxes = document.querySelectorAll('tbody input[type="checkbox"]:checked');
    return Array.from(checkedCheckboxes).map(checkbox => {
        const row = checkbox.closest('tr');
        return row.dataset.postId;
    }).filter(id => id);
}

function performBulkAction(action, postIds) {
    // Show loading state
    showLoadingState();
    
    // Create form for bulk action
    const form = document.createElement('form');
    form.method = 'POST';
    form.action = `/admin/posts/bulk-${action}`;
    
    // Add CSRF token if available
    const csrfToken = document.querySelector('meta[name="_csrf"]');
    if (csrfToken) {
        const csrfInput = document.createElement('input');
        csrfInput.type = 'hidden';
        csrfInput.name = csrfToken.getAttribute('content');
        csrfInput.value = csrfToken.getAttribute('content');
        form.appendChild(csrfInput);
    }
    
    // Add post IDs
    postIds.forEach(id => {
        const input = document.createElement('input');
        input.type = 'hidden';
        input.name = 'postIds';
        input.value = id;
        form.appendChild(input);
    });
    
    document.body.appendChild(form);
    form.submit();
}

function showLoadingState() {
    const container = document.querySelector('.container-fluid');
    if (container) {
        container.classList.add('loading');
    }
}

function hideLoadingState() {
    const container = document.querySelector('.container-fluid');
    if (container) {
        container.classList.remove('loading');
    }
}

// Status change functions
function approvePost(postId) {
    if (confirm('Bạn có chắc chắn muốn duyệt bài viết này?')) {
        const form = document.createElement('form');
        form.method = 'POST';
        form.action = `/admin/posts/${postId}/approve`;
        
        // Add CSRF token if available
        const csrfToken = document.querySelector('meta[name="_csrf"]');
        if (csrfToken) {
            const csrfInput = document.createElement('input');
            csrfInput.type = 'hidden';
            csrfInput.name = csrfToken.getAttribute('content');
            csrfInput.value = csrfToken.getAttribute('content');
            form.appendChild(csrfInput);
        }
        
        document.body.appendChild(form);
        form.submit();
    }
}

function rejectPost(postId) {
    if (confirm('Bạn có chắc chắn muốn từ chối bài viết này?')) {
        const form = document.createElement('form');
        form.method = 'POST';
        form.action = `/admin/posts/${postId}/reject`;
        
        // Add CSRF token if available
        const csrfToken = document.querySelector('meta[name="_csrf"]');
        if (csrfToken) {
            const csrfInput = document.createElement('input');
            csrfInput.type = 'hidden';
            csrfInput.name = csrfToken.getAttribute('content');
            csrfInput.value = csrfToken.getAttribute('content');
            form.appendChild(csrfInput);
        }
        
        document.body.appendChild(form);
        form.submit();
    }
}

// Utility functions
function formatDate(dateString) {
    const date = new Date(dateString);
    return date.toLocaleDateString('vi-VN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
    });
}

function truncateText(text, maxLength) {
    if (text.length <= maxLength) {
        return text;
    }
    return text.substring(0, maxLength) + '...';
}

// Export functions for global access
window.bulkApprove = bulkApprove;
window.bulkReject = bulkReject;
window.bulkDelete = bulkDelete;
window.approvePost = approvePost;
window.rejectPost = rejectPost;
