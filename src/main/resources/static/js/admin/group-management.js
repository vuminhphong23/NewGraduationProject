// Group Management JavaScript
document.addEventListener('DOMContentLoaded', function() {
    initializeGroupManagement();
});

function initializeGroupManagement() {
    // Initialize tooltips
    initializeTooltips();
    
    // Initialize bulk actions
    initializeBulkActions();
    
    // Initialize search functionality
    initializeSearch();
    
    // Initialize confirmation dialogs
    initializeConfirmations();
    
    // Initialize members modal
    initializeMembersModal();
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

function initializeSearch() {
    const searchInput = document.querySelector('input[name="keyword"]');
    const searchForm = document.querySelector('form[action*="/admin/groups"]');
    
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
            const groupName = this.dataset.groupName || 'nhóm này';
            
            if (confirm(`Bạn có chắc chắn muốn xóa ${groupName}? Hành động này không thể hoàn tác.`)) {
                const form = this.closest('form');
                if (form) {
                    form.submit();
                }
            }
        });
    });
}

function initializeMembersModal() {
    const membersModal = document.getElementById('membersModal');
    if (membersModal) {
        membersModal.addEventListener('show.bs.modal', function(event) {
            const button = event.relatedTarget;
            const groupId = button.getAttribute('data-group-id');
            const groupName = button.closest('tr').querySelector('.group-name').textContent;
            
            // Update modal title
            document.getElementById('membersModalLabel').textContent = `Thành viên nhóm: ${groupName}`;
            
            // Load members
            loadGroupMembers(groupId);
        });
    }
}

function loadGroupMembers(groupId) {
    const modalBody = document.querySelector('#membersModal .modal-body');
    
    // Show loading state
    modalBody.innerHTML = `
        <div class="text-center">
            <div class="spinner-border" role="status">
                <span class="visually-hidden">Loading...</span>
            </div>
            <p class="mt-2">Đang tải danh sách thành viên...</p>
        </div>
    `;
    
    // Call actual API
    fetch(`/admin/groups/${groupId}/members`)
        .then(response => {
            if (!response.ok) {
                throw new Error('Network response was not ok');
            }
            return response.json();
        })
        .then(members => {
            displayGroupMembers(members);
        })
        .catch(error => {
            console.error('Error loading group members:', error);
            modalBody.innerHTML = `
                <div class="alert alert-danger">
                    <i class="bi bi-exclamation-triangle"></i>
                    <strong>Lỗi!</strong> Không thể tải danh sách thành viên: ${error.message}
                </div>
            `;
        });
}

function displayGroupMembers(members) {
    const modalBody = document.querySelector('#membersModal .modal-body');
    
    if (members.length === 0) {
        modalBody.innerHTML = `
            <div class="empty-state">
                <i class="bi bi-people"></i>
                <h4>Chưa có thành viên</h4>
                <p>Nhóm này chưa có thành viên nào.</p>
            </div>
        `;
        return;
    }
    
    const membersHtml = members.map(member => `
        <div class="member-item">
            <img src="${member.avatar}" alt="avatar" class="member-avatar">
            <div class="member-info">
                <div class="member-name">${member.firstName} ${member.lastName}</div>
                <div class="member-username">@${member.username}</div>
            </div>
            <div class="member-role">
                <span class="badge ${member.role === 'ADMIN' ? 'badge-role-admin' : 'badge-role-member'}">
                    ${member.role === 'ADMIN' ? 'Quản trị' : 'Thành viên'}
                </span>
            </div>
        </div>
    `).join('');
    
    modalBody.innerHTML = `
        <div class="members-list">
            ${membersHtml}
        </div>
    `;
}

// Bulk actions
function bulkDeleteGroups() {
    const checkedIds = getCheckedGroupIds();
    if (checkedIds.length === 0) {
        alert('Vui lòng chọn ít nhất một nhóm để xóa.');
        return;
    }
    
    if (confirm(`Bạn có chắc chắn muốn xóa ${checkedIds.length} nhóm đã chọn? Hành động này không thể hoàn tác.`)) {
        performBulkAction('delete', checkedIds);
    }
}

function getCheckedGroupIds() {
    const checkedCheckboxes = document.querySelectorAll('tbody input[type="checkbox"]:checked');
    return Array.from(checkedCheckboxes).map(checkbox => {
        const row = checkbox.closest('tr');
        return row.dataset.groupId;
    }).filter(id => id);
}

function performBulkAction(action, groupIds) {
    // Show loading state
    showLoadingState();
    
    // Create form for bulk action
    const form = document.createElement('form');
    form.method = 'POST';
    form.action = `/admin/groups/bulk-${action}`;
    
    // Add CSRF token if available
    const csrfToken = document.querySelector('meta[name="_csrf"]');
    if (csrfToken) {
        const csrfInput = document.createElement('input');
        csrfInput.type = 'hidden';
        csrfInput.name = csrfToken.getAttribute('content');
        csrfInput.value = csrfToken.getAttribute('content');
        form.appendChild(csrfInput);
    }
    
    // Add group IDs
    groupIds.forEach(id => {
        const input = document.createElement('input');
        input.type = 'hidden';
        input.name = 'groupIds';
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

// Group management functions
function deleteGroup(groupId) {
    if (confirm('Bạn có chắc chắn muốn xóa nhóm này?')) {
        const form = document.createElement('form');
        form.method = 'POST';
        form.action = `/admin/groups/${groupId}/delete`;
        
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
window.bulkDeleteGroups = bulkDeleteGroups;
window.deleteGroup = deleteGroup;
