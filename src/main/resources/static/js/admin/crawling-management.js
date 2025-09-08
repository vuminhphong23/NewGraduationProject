// Admin Crawling Management JavaScript
let configs = [];
let currentConfig = null;

document.addEventListener('DOMContentLoaded', function() {
    loadStatistics();
    loadConfigs();
    
    // Auto refresh every 30 seconds
    setInterval(() => {
        loadStatistics();
        loadConfigs();
    }, 30000);
});

// Load statistics
async function loadStatistics() {
    try {
        const response = await fetch('/api/crawling/statistics');
        const stats = await response.json();
        
        document.getElementById('totalConfigs').textContent = stats.totalConfigs || 0;
        document.getElementById('activeConfigs').textContent = stats.activeConfigs || 0;
        document.getElementById('errorConfigs').textContent = stats.errorConfigs || 0;
        document.getElementById('totalCrawled').textContent = stats.totalCrawledPosts || 0;
    } catch (error) {
        console.error('Error loading statistics:', error);
    }
}

// Load configs
async function loadConfigs() {
    try {
        const response = await fetch('/api/crawling/configs');
        configs = await response.json();
        displayConfigs(configs);
    } catch (error) {
        console.error('Error loading configs:', error);
        showToast('Không thể tải danh sách configs', 'error');
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
                <p>Chưa có config nào. Hãy tạo config đầu tiên!</p>
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
        <div class="card config-card h-100">
            <div class="card-header d-flex justify-content-between align-items-center">
                <h6 class="mb-0">${config.name}</h6>
                <div>
                    <span class="badge bg-${statusClass} status-badge me-1">${config.status}</span>
                    <span class="badge bg-${enabledClass} status-badge">${config.enabled ? 'ON' : 'OFF'}</span>
                </div>
            </div>
            <div class="card-body">
                <p class="text-muted small mb-2">${config.description || 'Không có mô tả'}</p>
                <div class="mb-2">
                    <strong>URL:</strong><br>
                    <small class="text-break">${config.baseUrl}</small>
                </div>
                <div class="mb-2">
                    <strong>Topic:</strong> ${config.topicName}
                </div>
                <div class="mb-2">
                    <strong>Max Posts:</strong> ${config.maxPosts}
                </div>
                <div class="mb-2">
                    <strong>Interval:</strong> ${config.intervalMinutes} phút
                </div>
                <div class="mb-2">
                    <strong>Title Selector:</strong><br>
                    <code class="selector-preview">${config.titleSelector}</code>
                </div>
                <div class="row text-center mb-2">
                    <div class="col-4">
                        <small class="text-muted">Total</small><br>
                        <strong>${config.totalCrawled || 0}</strong>
                    </div>
                    <div class="col-4">
                        <small class="text-muted">Success</small><br>
                        <strong class="text-success">${config.successCount || 0}</strong>
                    </div>
                    <div class="col-4">
                        <small class="text-muted">Errors</small><br>
                        <strong class="text-danger">${config.errorCount || 0}</strong>
                    </div>
                </div>
                <div class="mb-2">
                    <small class="text-muted">Last crawled:</small><br>
                    <small>${config.lastCrawledAt ? new Date(config.lastCrawledAt).toLocaleString('vi-VN') : 'Chưa crawl'}</small>
                </div>
                ${config.lastError ? `
                    <div class="alert alert-danger alert-sm mb-2">
                        <small><strong>Lỗi:</strong> ${config.lastError}</small>
                    </div>
                ` : ''}
            </div>
            <div class="card-footer">
                <div class="btn-group w-100" role="group">
                    <button class="btn btn-sm btn-outline-primary" onclick="editConfig(${config.id})" title="Chỉnh sửa">
                        <i class="fa fa-edit"></i>
                    </button>
                    <button class="btn btn-sm btn-outline-info" onclick="testConfig(${config.id})" title="Test">
                        <i class="fa fa-flask"></i>
                    </button>
                    <button class="btn btn-sm btn-outline-success" onclick="crawlConfig(${config.id})" title="Crawl ngay">
                        <i class="fa fa-play"></i>
                    </button>
                    <button class="btn btn-sm btn-outline-warning" onclick="toggleConfig(${config.id})" title="Bật/Tắt">
                        <i class="fa fa-power-off"></i>
                    </button>
                    <button class="btn btn-sm btn-outline-danger" onclick="deleteConfig(${config.id})" title="Xóa">
                        <i class="fa fa-trash"></i>
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
        const response = await fetch(`/api/crawling/configs/${configId}`);
        const config = await response.json();
        
        currentConfig = config;
        fillConfigForm(config);
        
        document.getElementById('modalTitle').textContent = 'Chỉnh sửa Config';
        const modal = new bootstrap.Modal(document.getElementById('configModal'));
        modal.show();
    } catch (error) {
        console.error('Error loading config:', error);
        showToast('Không thể tải config', 'error');
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
    document.getElementById('configTitleSelector').value = config.titleSelector || '';
    document.getElementById('configContentSelector').value = config.contentSelector || '';
    document.getElementById('configLinkSelector').value = config.linkSelector || '';
    document.getElementById('configImageSelector').value = config.imageSelector || '';
    document.getElementById('configAuthorSelector').value = config.authorSelector || '';
    document.getElementById('configDateSelector').value = config.dateSelector || '';
    document.getElementById('configIntervalMinutes').value = config.intervalMinutes || 60;
    document.getElementById('configTimeout').value = config.timeout || 10000;
    document.getElementById('configUserAgent').value = config.userAgent || '';
    document.getElementById('configAdditionalHeaders').value = config.additionalHeaders || '';
    document.getElementById('configEnabled').checked = config.enabled || false;
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
            titleSelector: document.getElementById('configTitleSelector').value,
            contentSelector: document.getElementById('configContentSelector').value,
            linkSelector: document.getElementById('configLinkSelector').value,
            imageSelector: document.getElementById('configImageSelector').value,
            authorSelector: document.getElementById('configAuthorSelector').value,
            dateSelector: document.getElementById('configDateSelector').value,
            intervalMinutes: parseInt(document.getElementById('configIntervalMinutes').value),
            timeout: parseInt(document.getElementById('configTimeout').value),
            userAgent: document.getElementById('configUserAgent').value,
            additionalHeaders: document.getElementById('configAdditionalHeaders').value,
            enabled: document.getElementById('configEnabled').checked
        };
        
        const configId = document.getElementById('configId').value;
        const url = configId ? `/api/crawling/configs/${configId}` : '/api/crawling/configs';
        const method = configId ? 'PUT' : 'POST';
        
        const response = await fetch(url, {
            method: method,
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(configData)
        });
        
        if (response.ok) {
            showToast('Config đã được lưu thành công!', 'success');
            bootstrap.Modal.getInstance(document.getElementById('configModal')).hide();
            loadConfigs();
        } else {
            showToast('Lỗi khi lưu config', 'error');
        }
    } catch (error) {
        console.error('Error saving config:', error);
        showToast('Lỗi khi lưu config', 'error');
    }
}

// Test config
async function testConfig(configId) {
    try {
        showToast('Đang test config...', 'info');
        
        const response = await fetch(`/api/crawling/configs/${configId}/test`, {
            method: 'POST'
        });
        
        const result = await response.text();
        
        document.getElementById('testResult').textContent = result;
        const modal = new bootstrap.Modal(document.getElementById('testModal'));
        modal.show();
    } catch (error) {
        console.error('Error testing config:', error);
        showToast('Lỗi khi test config', 'error');
    }
}

// Crawl config
async function crawlConfig(configId) {
    try {
        showToast('Đang bắt đầu crawl...', 'info');
        
        const response = await fetch(`/api/crawling/configs/${configId}/crawl`, {
            method: 'POST'
        });
        
        const result = await response.text();
        
        if (response.ok) {
            showToast('Crawl đã bắt đầu thành công!', 'success');
            loadConfigs();
        } else {
            showToast('Lỗi khi crawl: ' + result, 'error');
        }
    } catch (error) {
        console.error('Error crawling config:', error);
        showToast('Lỗi khi crawl config', 'error');
    }
}

// Toggle config
async function toggleConfig(configId) {
    try {
        const response = await fetch(`/api/crawling/configs/${configId}/toggle`, {
            method: 'POST'
        });
        
        if (response.ok) {
            showToast('Config đã được cập nhật!', 'success');
            loadConfigs();
        } else {
            showToast('Lỗi khi cập nhật config', 'error');
        }
    } catch (error) {
        console.error('Error toggling config:', error);
        showToast('Lỗi khi cập nhật config', 'error');
    }
}

// Delete config
async function deleteConfig(configId) {
    if (!confirm('Bạn có chắc chắn muốn xóa config này?')) {
        return;
    }
    
    try {
        const response = await fetch(`/api/crawling/configs/${configId}`, {
            method: 'DELETE'
        });
        
        if (response.ok) {
            showToast('Config đã được xóa!', 'success');
            loadConfigs();
        } else {
            showToast('Lỗi khi xóa config', 'error');
        }
    } catch (error) {
        console.error('Error deleting config:', error);
        showToast('Lỗi khi xóa config', 'error');
    }
}

// Refresh data
function refreshData() {
    loadStatistics();
    loadConfigs();
    showToast('Đã làm mới dữ liệu!', 'success');
}

// Crawl all active configs
async function crawlAllActive() {
    try {
        showToast('Đang bắt đầu crawl tất cả configs...', 'info');
        
        const response = await fetch('/api/crawling/crawl-all', {
            method: 'POST'
        });
        
        const result = await response.text();
        
        if (response.ok) {
            showToast('Crawl tất cả configs đã bắt đầu!', 'success');
            loadConfigs();
        } else {
            showToast('Lỗi khi crawl: ' + result, 'error');
        }
    } catch (error) {
        console.error('Error crawling all configs:', error);
        showToast('Lỗi khi crawl tất cả configs', 'error');
    }
}

// Show toast using existing toast manager
function showToast(message, type = 'info') {
    if (typeof showToastMessage === 'function') {
        showToastMessage(message, type);
    } else {
        // Fallback toast implementation
        const toast = document.createElement('div');
        toast.className = `toast align-items-center text-white bg-${type} border-0`;
        toast.setAttribute('role', 'alert');
        toast.innerHTML = `
            <div class="d-flex">
                <div class="toast-body">${message}</div>
                <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button>
            </div>
        `;
        
        document.body.appendChild(toast);
        
        const bsToast = new bootstrap.Toast(toast);
        bsToast.show();
        
        toast.addEventListener('hidden.bs.toast', () => {
            toast.remove();
        });
    }
}

// Reset form when modal is hidden
document.getElementById('configModal').addEventListener('hidden.bs.modal', function() {
    document.getElementById('configForm').reset();
    document.getElementById('configId').value = '';
    currentConfig = null;
    document.getElementById('modalTitle').textContent = 'Thêm Config Crawling';
});
