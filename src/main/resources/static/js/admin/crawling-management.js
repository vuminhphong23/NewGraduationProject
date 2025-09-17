// Admin Crawling Management JavaScript
let configs = [];
let currentConfig = null;
let groups = [];
let selectedGroupIds = [];

document.addEventListener('DOMContentLoaded', function() {
    loadStatistics();
    loadConfigs();
    loadGroups();
    initializeCustomCombobox();
    
    // Auto refresh every 30 seconds
    setInterval(() => {
        loadStatistics();
        loadConfigs();
    }, 30000);
});

// Load statistics
async function loadStatistics() {
    try {
        const response = await authenticatedFetch('/api/crawling/statistics');
        const stats = await response.json();
        
        document.getElementById('totalConfigs').textContent = stats.totalConfigs || 0;
        document.getElementById('activeConfigs').textContent = stats.activeConfigs || 0;
        document.getElementById('errorConfigs').textContent = stats.errorConfigs || 0;
        document.getElementById('totalCrawled').textContent = stats.totalCrawledPosts || 0;
    } catch (error) {
        console.error('Error loading statistics:', error);
    }
}

// Load groups
async function loadGroups() {
    try {
        const response = await authenticatedFetch('/api/crawling/groups');
        const apiResponse = await response.json();
        
        if (apiResponse.success) {
            groups = apiResponse.data;
            populateCustomCombobox();
            
            if (groups.length === 0) {
                toastManager.warning('Không có group nào. Posts sẽ được tạo công khai.');
            }
        } else {
            toastManager.error(apiResponse.error || 'Không thể tải danh sách groups');
        }
    } catch (error) {
        console.error('Error loading groups:', error);
        toastManager.error('Không thể tải danh sách groups');
    }
}

// Initialize custom combobox
function initializeCustomCombobox() {
    const combobox = document.getElementById('customGroupSelect');
    const comboboxSelected = document.getElementById('comboboxSelected');
    const comboboxDropdown = document.getElementById('comboboxDropdown');
    const groupSearch = document.getElementById('groupSearch');
    const comboboxOptions = document.getElementById('comboboxOptions');
    
    // Toggle dropdown
    comboboxSelected.addEventListener('click', function(e) {
        e.stopPropagation();
        combobox.classList.toggle('open');
        if (combobox.classList.contains('open')) {
            groupSearch.focus();
        }
    });
    
    // Close dropdown when clicking outside
    document.addEventListener('click', function(e) {
        if (!combobox.contains(e.target)) {
            combobox.classList.remove('open');
        }
    });
    
    // Search functionality
    groupSearch.addEventListener('input', function() {
        const searchTerm = this.value.toLowerCase();
        const options = comboboxOptions.querySelectorAll('.combobox-option');
        
        options.forEach(option => {
            const groupName = option.textContent.toLowerCase();
            if (groupName.includes(searchTerm)) {
                option.style.display = 'flex';
            } else {
                option.style.display = 'none';
            }
        });
    });
    
    // Clear search when dropdown opens
    comboboxSelected.addEventListener('click', function() {
        groupSearch.value = '';
        const options = comboboxOptions.querySelectorAll('.combobox-option');
        options.forEach(option => {
            option.style.display = 'flex';
        });
    });
    
    // Prevent dropdown from closing when clicking inside
    comboboxDropdown.addEventListener('click', function(e) {
        e.stopPropagation();
    });
}

// Populate custom combobox options
function populateCustomCombobox() {
    const comboboxOptions = document.getElementById('comboboxOptions');
    comboboxOptions.innerHTML = '';
    
    
    if (groups.length === 0) {
        const noGroupsDiv = document.createElement('div');
        noGroupsDiv.className = 'no-groups-message';
        noGroupsDiv.textContent = 'Không có group nào - Posts sẽ được tạo công khai';
        comboboxOptions.appendChild(noGroupsDiv);
        return;
    }
    
    groups.forEach(group => {
        const optionDiv = document.createElement('div');
        optionDiv.className = 'combobox-option';
        optionDiv.innerHTML = `
            <label style="margin: 0; cursor: pointer; flex: 1;">
                <input type="checkbox" value="${group.id}" style="margin-right: 0.5rem;">
                ${group.name}
            </label>
        `;
        
        // Handle checkbox change
        const checkbox = optionDiv.querySelector('input[type="checkbox"]');
        checkbox.addEventListener('change', function() {
            if (this.checked) {
                if (!selectedGroupIds.includes(group.id)) {
                    selectedGroupIds.push(group.id);
                }
            } else {
                selectedGroupIds = selectedGroupIds.filter(id => id !== group.id);
            }
            updateSelectedDisplay();
            updateComboboxSelectedText();
        });
        
        comboboxOptions.appendChild(optionDiv);
    });
}

// Update selected groups display
function updateSelectedDisplay() {
    const selectedGroupsDiv = document.getElementById('selectedGroups');
    selectedGroupsDiv.innerHTML = '';
    
    selectedGroupIds.forEach(groupId => {
        const group = groups.find(g => g.id === groupId);
        if (group) {
            const badge = document.createElement('div');
            badge.className = 'selected-group-badge';
            badge.innerHTML = `
                <i class="fa fa-users"></i>
                ${group.name}
                <button type="button" class="remove-btn" onclick="removeGroup(${groupId})">
                    <i class="fa fa-times"></i>
                </button>
            `;
            selectedGroupsDiv.appendChild(badge);
        }
    });
}

// Update combobox selected text
function updateComboboxSelectedText() {
    const comboboxSelected = document.getElementById('comboboxSelected');
    const placeholder = comboboxSelected.querySelector('.placeholder');
    const selectedText = comboboxSelected.querySelector('.selected-text');
    
    if (selectedGroupIds.length === 0) {
        if (placeholder) placeholder.style.display = 'block';
        if (selectedText) selectedText.style.display = 'none';
    } else {
        if (placeholder) placeholder.style.display = 'none';
        if (!selectedText) {
            const textSpan = document.createElement('span');
            textSpan.className = 'selected-text';
            comboboxSelected.insertBefore(textSpan, comboboxSelected.querySelector('.combobox-arrow'));
        }
        const textSpan = comboboxSelected.querySelector('.selected-text');
        textSpan.textContent = `${selectedGroupIds.length} group(s) đã chọn`;
        textSpan.style.display = 'block';
    }
}

// Remove group from selection
function removeGroup(groupId) {
    selectedGroupIds = selectedGroupIds.filter(id => id !== groupId);
    
    // Uncheck the checkbox
    const checkbox = document.querySelector(`input[value="${groupId}"]`);
    if (checkbox) {
        checkbox.checked = false;
    }
    
    updateSelectedDisplay();
    updateComboboxSelectedText();
}

// Load configs
async function loadConfigs() {
    try {
        const response = await authenticatedFetch('/api/crawling/configs');
        const apiResponse = await response.json();
        
        if (apiResponse.success) {
            configs = apiResponse.data;
            displayConfigs(configs);
        } else {
            toastManager.error(apiResponse.error || 'Không thể tải danh sách cấu hình');
        }
    } catch (error) {
        console.error('Error loading configs:', error);
        toastManager.error('Không thể tải danh sách cấu hình');
    }
}

// Display configs
function displayConfigs(configs) {
    const container = document.getElementById('configsList');
    container.innerHTML = '';
    
    if (configs.length === 0) {
        container.innerHTML = `
            <div class="col-12 text-center text-muted py-5">
                <i class="fa fa-inbox fa-3x mb-3"></i>
                <p>Chưa có cấu hình nào. Hãy tạo cấu hình đầu tiên!</p>
            </div>
        `;
        return;
    }
    
    configs.forEach(config => {
        const configCard = createConfigCard(config);
        container.appendChild(configCard);
    });
}

// Create config card
function createConfigCard(config) {
    const col = document.createElement('div');
    col.className = 'col-md-6 col-lg-4 mb-4';
    
    const statusClass = getStatusClass(config.status);
    const enabledClass = config.enabled ? 'success' : 'secondary';
    
    col.innerHTML = `
        <div class="card config-card h-100 shadow-sm">
            <div class="card-header d-flex justify-content-between align-items-center bg-light">
                <h6 class="mb-0 fw-bold text-primary">${config.name}</h6>
                <div>
                    <span class="badge bg-${statusClass} status-badge me-1">${config.status}</span>
                    <span class="badge bg-${enabledClass} status-badge">${config.enabled ? 'ON' : 'OFF'}</span>
                </div>
            </div>
            <div class="card-body">
                <p class="text-muted small mb-3 fst-italic">${config.description || 'Không có mô tả'}</p>
                
                <div class="mb-3">
                    <div class="d-flex align-items-center mb-1">
                        <i class="fa fa-link text-muted me-2"></i>
                        <strong class="text-dark">URL nguồn:</strong>
                    </div>
                    <small class="text-break text-primary">${config.baseUrl}</small>
                </div>
                
                <div class="row mb-3">
                    <div class="col-6">
                        <div class="d-flex align-items-center mb-1">
                            <i class="fa fa-tag text-muted me-2"></i>
                            <strong class="text-dark">Chủ đề:</strong>
                        </div>
                        <span class="badge bg-info">${config.topicName}</span>
                    </div>
                    <div class="col-6">
                        <div class="d-flex align-items-center mb-1">
                            <i class="fa fa-list text-muted me-2"></i>
                            <strong class="text-dark">Số bài tối đa:</strong>
                        </div>
                        <span class="badge bg-secondary">${config.maxPosts}</span>
                    </div>
                </div>
                
                <div class="mb-3">
                    <div class="d-flex align-items-center mb-2">
                        <i class="fa fa-users text-muted me-2"></i>
                        <strong class="text-dark">Nhóm đã chọn:</strong>
                    </div>
                    ${config.groupIds && config.groupIds.length > 0 ? 
                        config.groupIds.map(id => {
                            const group = groups.find(g => g.id === id);
                            return group ? `<span class="badge bg-primary me-1 mb-1">${group.name}</span>` : '';
                        }).join('') : 
                        '<span class="text-muted small"><i class="fa fa-info-circle me-1"></i>Bài viết sẽ được tạo công khai</span>'
                    }
                </div>
                
                <div class="row text-center bg-light rounded p-2 mb-3">
                    <div class="col-6">
                        <div class="d-flex flex-column align-items-center">
                            <i class="fa fa-database text-primary mb-1"></i>
                            <small class="text-muted">Tổng bài viết</small>
                            <strong class="text-primary fs-5">${config.totalCrawled || 0}</strong>
                        </div>
                    </div>
                    <div class="col-6">
                        <div class="d-flex flex-column align-items-center">
                            <i class="fa fa-clock text-success mb-1"></i>
                            <small class="text-muted">Lần crawl cuối</small>
                            <small class="text-success">${config.lastCrawledAt ? new Date(config.lastCrawledAt).toLocaleString('vi-VN') : 'Chưa crawl'}</small>
                        </div>
                    </div>
                </div>
            </div>
            <div class="card-footer bg-light">
                <div class="btn-group w-100" role="group">
                    <button class="btn btn-sm btn-outline-primary" onclick="editConfig(${config.id})" title="Chỉnh sửa">
                        <i class="fa fa-edit me-1"></i>Sửa
                    </button>
                    <button class="btn btn-sm btn-outline-success" onclick="crawlConfig(${config.id})" title="Crawl ngay">
                        <i class="fa fa-play me-1"></i>Crawl
                    </button>
                    <button class="btn btn-sm btn-outline-warning" onclick="toggleConfig(${config.id})" title="Bật/Tắt">
                        <i class="fa fa-power-off me-1"></i>Bật/Tắt
                    </button>
                    <button class="btn btn-sm btn-outline-danger" onclick="deleteConfig(${config.id})" title="Xóa">
                        <i class="fa fa-trash me-1"></i>Xóa
                    </button>
                </div>
            </div>
        </div>
    `;
    
    return col;
}

// Get status class
function getStatusClass(status) {
    switch (status) {
        case 'ACTIVE': return 'success';
        case 'ERROR': return 'danger';
        case 'INACTIVE': return 'secondary';
        default: return 'secondary';
    }
}

// Edit config
async function editConfig(configId) {
    try {
        const response = await authenticatedFetch(`/api/crawling/configs/${configId}`);
        const config = await response.json();
        
        currentConfig = config;
        fillConfigForm(config);
        
        document.getElementById('modalTitle').textContent = 'Chỉnh sửa cấu hình';
        const modal = new bootstrap.Modal(document.getElementById('configModal'));
        modal.show();
    } catch (error) {
        console.error('Error loading config:', error);
        toastManager.error('Không thể tải cấu hình');
    }
}

// Fill config form
function fillConfigForm(config) {
    document.getElementById('configId').value = config.id || '';
    document.getElementById('configName').value = config.name || '';
    document.getElementById('configDescription').value = config.description || '';
    document.getElementById('configBaseUrl').value = config.baseUrl || '';
    document.getElementById('configTopicName').value = config.topicName || '';
    document.getElementById('configMaxPosts').value = config.maxPosts || 10;
    document.getElementById('configEnabled').checked = config.enabled || false;
    
    // Set selected groups for custom combobox
    selectedGroupIds = config.groupIds ? [...config.groupIds] : [];
    
    // Update checkboxes
    const checkboxes = document.querySelectorAll('#comboboxOptions input[type="checkbox"]');
    checkboxes.forEach(checkbox => {
        checkbox.checked = selectedGroupIds.includes(parseInt(checkbox.value));
    });
    
    updateSelectedDisplay();
    updateComboboxSelectedText();
}

// Save config
async function saveConfig() {
    try {
        const configData = {
            name: document.getElementById('configName').value,
            description: document.getElementById('configDescription').value,
            baseUrl: document.getElementById('configBaseUrl').value,
            topicName: document.getElementById('configTopicName').value,
            maxPosts: parseInt(document.getElementById('configMaxPosts').value),
            enabled: document.getElementById('configEnabled').checked,
            groupIds: selectedGroupIds
        };
        
        const configId = document.getElementById('configId').value;
        const url = configId ? `/api/crawling/configs/${configId}` : '/api/crawling/configs';
        const method = configId ? 'PUT' : 'POST';
        
        const response = await authenticatedFetch(url, {
            method: method,
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(configData)
        });
        
        if (response.ok) {
            toastManager.success('Config đã được lưu thành công!');
            bootstrap.Modal.getInstance(document.getElementById('configModal')).hide();
            loadConfigs();
        } else {
            toastManager.error('Lỗi khi lưu config');
        }
    } catch (error) {
        console.error('Error saving config:', error);
        toastManager.error('Lỗi khi lưu config');
    }
}


// Crawl config
async function crawlConfig(configId) {
    try {
        toastManager.info('Đang bắt đầu crawl...');
        
        const response = await authenticatedFetch(`/api/crawling/configs/${configId}/crawl`, {
            method: 'POST'
        });
        
        const result = await response.text();
        
        if (response.ok) {
            toastManager.success('Crawl đã bắt đầu thành công! Posts sẽ được tạo trong groups đã chọn.');
            loadConfigs();
        } else {
            toastManager.error('Lỗi khi crawl: ' + result);
        }
    } catch (error) {
        console.error('Error crawling config:', error);
        toastManager.error('Lỗi khi crawl config');
    }
}

// Toggle config
async function toggleConfig(configId) {
    try {
        const response = await authenticatedFetch(`/api/crawling/configs/${configId}/toggle`, {
            method: 'POST'
        });
        
        if (response.ok) {
            toastManager.success('Config đã được cập nhật!');
            loadConfigs();
        } else {
            toastManager.error('Lỗi khi cập nhật config');
        }
    } catch (error) {
        console.error('Error toggling config:', error);
        toastManager.error('Lỗi khi cập nhật config');
    }
}

// Delete config
async function deleteConfig(configId) {
    if (!confirm('Bạn có chắc chắn muốn xóa config này?')) {
        return;
    }
    
    try {
        const response = await authenticatedFetch(`/api/crawling/configs/${configId}`, {
            method: 'DELETE'
        });
        
        if (response.ok) {
            toastManager.success('Config đã được xóa!');
            loadConfigs();
        } else {
            toastManager.error('Lỗi khi xóa config');
        }
    } catch (error) {
        console.error('Error deleting config:', error);
        toastManager.error('Lỗi khi xóa config');
    }
}

// Refresh data
function refreshData() {
    loadStatistics();
    loadConfigs();
    toastManager.success('Đã làm mới dữ liệu!');
}

// Crawl all active configs
async function crawlAllActive() {
    try {
        toastManager.info('Đang bắt đầu crawl tất cả configs...');
        
        const response = await authenticatedFetch('/api/crawling/crawl-all', {
            method: 'POST'
        });
        
        const result = await response.text();
        
        if (response.ok) {
            toastManager.success('Crawl tất cả configs đã bắt đầu! Posts sẽ được tạo trong các groups đã chọn.');
            loadConfigs();
        } else {
            toastManager.error('Lỗi khi crawl: ' + result);
        }
    } catch (error) {
        console.error('Error crawling all configs:', error);
        toastManager.error('Lỗi khi crawl tất cả configs');
    }
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

// Reset form when modal is hidden
document.getElementById('configModal').addEventListener('hidden.bs.modal', function() {
    document.getElementById('configForm').reset();
    document.getElementById('configId').value = '';
    currentConfig = null;
    document.getElementById('modalTitle').textContent = 'Thêm cấu hình crawling';
    
    // Clear group selection for custom combobox
    selectedGroupIds = [];
    
    // Uncheck all checkboxes
    const checkboxes = document.querySelectorAll('#comboboxOptions input[type="checkbox"]');
    checkboxes.forEach(checkbox => {
        checkbox.checked = false;
    });
    
    updateSelectedDisplay();
    updateComboboxSelectedText();
    
    // Clear search
    document.getElementById('groupSearch').value = '';
    
    // Close combobox
    document.getElementById('customGroupSelect').classList.remove('open');
});
