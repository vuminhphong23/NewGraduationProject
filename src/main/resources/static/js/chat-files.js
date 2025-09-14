/**
 * Chat Files Manager - Xử lý file upload và hiển thị trong chat
 * Tái sử dụng logic từ FileUploadManager
 */
class ChatFilesManager {
    constructor() {
        this.currentRoomId = null;
        this.files = [];
        this.images = [];
        this.documents = [];
        this.selectedFiles = [];
        this.isUploading = false;
        
        // Tái sử dụng validation từ FileUploadManager
        this.maxFileSize = 10 * 1024 * 1024; // 10MB
        this.allowedTypes = [
            'image/jpeg', 'image/png', 'image/gif', 'image/webp',
            'video/mp4', 'video/avi', 'video/mov', 'video/wmv',
            'application/pdf', 'application/msword', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
            'text/plain', 'application/vnd.ms-excel', 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
            'application/vnd.ms-powerpoint', 'application/vnd.openxmlformats-officedocument.presentationml.presentation'
        ];
        
        this.init();
    }

    init() {
        console.log('ChatFilesManager: Initializing...');
        this.setupEventListeners();
        this.setupDragAndDrop();
        console.log('ChatFilesManager: Initialization complete');
    }

    setupEventListeners() {
        console.log('ChatFilesManager: Setting up event listeners...');
        
        // Toggle files sidebar
        const filesToggleBtn = document.getElementById('filesToggleBtn');
        const closeFilesBtn = document.getElementById('closeFilesBtn');
        const chatFilesSidebar = document.getElementById('chatFilesSidebar');
        
        console.log('ChatFilesManager: Elements found:', {
            filesToggleBtn: !!filesToggleBtn,
            closeFilesBtn: !!closeFilesBtn,
            chatFilesSidebar: !!chatFilesSidebar
        });

        if (filesToggleBtn) {
            filesToggleBtn.addEventListener('click', () => {
                this.toggleFilesSidebar();
            });
        }

        if (closeFilesBtn) {
            closeFilesBtn.addEventListener('click', () => {
                this.hideFilesSidebar();
            });
        }

        // File tabs
        const fileTabs = document.querySelectorAll('.files-tab');
        fileTabs.forEach(tab => {
            tab.addEventListener('click', (e) => {
                this.switchTab(e.target.closest('.files-tab').dataset.tab);
            });
        });

        // Upload buttons
        const attachFileBtn = document.getElementById('attachFileBtn');
        const uploadFilesBtn = document.getElementById('uploadFilesBtn');
        const fileInput = document.getElementById('fileInput');

        if (attachFileBtn) {
            attachFileBtn.addEventListener('click', () => {
                this.showUploadModal();
            });
        }

        if (uploadFilesBtn) {
            uploadFilesBtn.addEventListener('click', () => {
                this.showUploadModal();
            });
        }

        if (fileInput) {
            fileInput.addEventListener('change', (e) => {
                this.handleFileSelect(e.target.files);
            });
        }

        // Upload modal
        this.setupUploadModal();
    }

    setupUploadModal() {
        const fileUploadModal = document.getElementById('fileUploadModal');
        const fileUploadOverlay = document.getElementById('fileUploadOverlay');
        const closeFileUploadBtn = document.getElementById('closeFileUploadBtn');
        const cancelUploadBtn = document.getElementById('cancelUploadBtn');
        const confirmUploadBtn = document.getElementById('confirmUploadBtn');
        const fileUploadArea = document.getElementById('fileUploadArea');
        const uploadLink = document.querySelector('.upload-link');

        // Close modal
        [fileUploadOverlay, closeFileUploadBtn, cancelUploadBtn].forEach(element => {
            if (element) {
                element.addEventListener('click', () => {
                    this.hideUploadModal();
                });
            }
        });

        // Confirm upload
        if (confirmUploadBtn) {
            confirmUploadBtn.addEventListener('click', () => {
                this.uploadSelectedFiles();
            });
        }

        // Click to select files
        if (uploadLink) {
            uploadLink.addEventListener('click', () => {
                document.getElementById('fileInput').click();
            });
        }

        if (fileUploadArea) {
            fileUploadArea.addEventListener('click', () => {
                document.getElementById('fileInput').click();
            });
        }
    }

    setupDragAndDrop() {
        const fileUploadArea = document.getElementById('fileUploadArea');
        
        if (!fileUploadArea) {
            console.log('ChatFilesManager: fileUploadArea not found');
            return;
        }
        
        console.log('ChatFilesManager: Setting up drag and drop for fileUploadArea');

        ['dragenter', 'dragover', 'dragleave', 'drop'].forEach(eventName => {
            fileUploadArea.addEventListener(eventName, this.preventDefaults, false);
        });

        ['dragenter', 'dragover'].forEach(eventName => {
            fileUploadArea.addEventListener(eventName, () => {
                if (fileUploadArea) {
                    fileUploadArea.classList.add('dragover');
                }
            }, false);
        });

        ['dragleave', 'drop'].forEach(eventName => {
            fileUploadArea.addEventListener(eventName, () => {
                if (fileUploadArea) {
                    fileUploadArea.classList.remove('dragover');
                }
            }, false);
        });

        fileUploadArea.addEventListener('drop', (e) => {
            const files = e.dataTransfer.files;
            this.handleFileSelect(files);
        }, false);
    }

    preventDefaults(e) {
        e.preventDefault();
        e.stopPropagation();
    }

    // ========== Files Sidebar Management ==========

    toggleFilesSidebar() {
        const sidebar = document.getElementById('chatFilesSidebar');
        const chatMain = document.querySelector('.chat-main');
        
        if (sidebar) {
            sidebar.classList.toggle('show');
            if (sidebar.classList.contains('show')) {
                this.loadRoomFiles();
                if (chatMain) {
                    chatMain.classList.add('with-files-sidebar');
                }
            } else {
                if (chatMain) {
                    chatMain.classList.remove('with-files-sidebar');
                }
            }
        }
    }

    hideFilesSidebar() {
        const sidebar = document.getElementById('chatFilesSidebar');
        const chatMain = document.querySelector('.chat-main');
        
        if (sidebar) {
            sidebar.classList.remove('show');
            if (chatMain) {
                chatMain.classList.remove('with-files-sidebar');
            }
        }
    }

    switchTab(tabName) {
        // Update tab buttons
        document.querySelectorAll('.files-tab').forEach(tab => {
            tab.classList.remove('active');
        });
        const activeTab = document.querySelector(`[data-tab="${tabName}"]`);
        if (activeTab) {
            activeTab.classList.add('active');
        }

        // Update tab content
        document.querySelectorAll('.files-tab-content').forEach(content => {
            content.classList.remove('active');
        });
        const activeContent = document.getElementById(`${tabName}Content`);
        if (activeContent) {
            activeContent.classList.add('active');
        }

        // Load appropriate content
        switch (tabName) {
            case 'all':
                this.renderAllFiles();
                break;
            case 'images':
                this.renderImages();
                break;
            case 'documents':
                this.renderDocuments();
                break;
        }
    }

    // ========== File Loading ==========

    async loadRoomFiles() {
        if (!this.currentRoomId) return;

        try {
            // Load all files
            const response = await fetch(`/api/chat/files/room/${this.currentRoomId}`, {
                method: 'GET',
                credentials: 'include'
            });

            if (response.ok) {
                const data = await response.json();
                if (data.success) {
                    this.files = data.data || [];
                    this.categorizeFiles();
                    this.updateFilesCount();
                    this.renderAllFiles();
                }
            }
        } catch (error) {
            // Error loading files
        }
    }

    categorizeFiles() {
        this.images = this.files.filter(file => file.fileType === 'image' || file.attachmentType === 'IMAGE');
        this.documents = this.files.filter(file => file.fileType !== 'image' && file.attachmentType !== 'IMAGE');
    }

    updateFilesCount() {
        const filesCount = document.getElementById('filesCount');
        if (filesCount) {
            filesCount.textContent = `${this.files.length} files`;
        }
    }

    // ========== File Rendering ==========

    renderAllFiles() {
        const container = document.getElementById('allFilesList');
        if (!container) return;

        if (this.files.length === 0) {
            container.innerHTML = '<div class="no-files">Chưa có file nào</div>';
            return;
        }

        container.innerHTML = this.files.map(file => this.createFileItem(file)).join('');
        this.attachFileItemEvents(container);
    }

    renderImages() {
        const container = document.getElementById('imagesGrid');
        if (!container) return;

        if (this.images.length === 0) {
            container.innerHTML = '<div class="no-files">Chưa có ảnh nào</div>';
            return;
        }

        container.innerHTML = this.images.map(image => this.createImageItem(image)).join('');
        this.attachImageItemEvents(container);
    }

    renderDocuments() {
        const container = document.getElementById('documentsList');
        if (!container) return;

        if (this.documents.length === 0) {
            container.innerHTML = '<div class="no-files">Chưa có tài liệu nào</div>';
            return;
        }

        container.innerHTML = this.documents.map(doc => this.createFileItem(doc)).join('');
        this.attachFileItemEvents(container);
    }

    createFileItem(file) {
        const iconClass = this.getFileIconClass(file.fileType);
        const fileSize = this.formatFileSize(file.fileSize);
        const uploadDate = this.formatDate(file.uploadedAt);

        return `
            <div class="file-item" data-file-id="${file.id}">
                <div class="file-icon ${file.fileType}">
                    <i class="${iconClass}"></i>
                </div>
                <div class="file-info">
                    <div class="file-name" title="${file.originalName}" onclick="window.downloadFile('${file.downloadUrl}', ${file.id})" style="cursor: pointer;">${file.originalName}</div>
                    <div class="file-meta">
                        <span>${fileSize}</span>
                        <span>•</span>
                        <span>${uploadDate}</span>
                    </div>
                </div>
                <div class="file-actions">
                    <button class="file-action-btn" title="Xem" onclick="chatFilesManager.viewFile(${file.id})">
                        <i class="fas fa-eye"></i>
                    </button>
                    <button class="file-action-btn" title="Tải về" onclick="window.downloadFile('${file.downloadUrl}', ${file.id})">
                        <i class="fas fa-download"></i>
                    </button>
                    <button class="file-action-btn delete" title="Xóa" onclick="chatFilesManager.deleteFile(${file.id})">
                        <i class="fas fa-trash"></i>
                    </button>
                </div>
            </div>
        `;
    }

    createImageItem(image) {
        const uploadDate = this.formatDate(image.uploadedAt);

        return `
            <div class="image-item" data-file-id="${image.id}">
                <img src="${image.previewUrl}" alt="${image.originalName}" loading="lazy">
                <div class="image-overlay">
                    <button class="image-action-btn" title="Xem" onclick="chatFilesManager.viewFile(${image.id})">
                        <i class="fas fa-eye"></i>
                    </button>
                    <button class="image-action-btn" title="Tải về" onclick="window.downloadFile('${image.downloadUrl}', ${image.id})">
                        <i class="fas fa-download"></i>
                    </button>
                </div>
                <div class="image-info" style="position: absolute; bottom: 0; left: 0; right: 0; background: rgba(0,0,0,0.7); color: white; padding: 4px 8px; font-size: 10px;">
                    ${uploadDate}
                </div>
            </div>
        `;
    }

    getFileIconClass(fileType) {
        switch (fileType) {
            case 'image': return 'fas fa-image';
            case 'video': return 'fas fa-video';
            case 'document': return 'fas fa-file-alt';
            default: return 'fas fa-file';
        }
    }

    attachFileItemEvents(container) {
        container.querySelectorAll('.file-item').forEach(item => {
            item.addEventListener('click', (e) => {
                if (!e.target.closest('.file-actions')) {
                    const fileId = parseInt(item.dataset.fileId);
                    this.viewFile(fileId);
                }
            });
        });
    }

    attachImageItemEvents(container) {
        container.querySelectorAll('.image-item').forEach(item => {
            item.addEventListener('click', () => {
                const fileId = parseInt(item.dataset.fileId);
                this.viewFile(fileId);
            });
        });
    }

    // ========== File Actions ==========

    viewFile(fileId) {
        const file = this.files.find(f => f.id === fileId);
        if (file) {
            window.open(file.previewUrl, '_blank');
        }
    }

    // downloadFile is now handled by download-utils.js

    async deleteFile(fileId) {
        if (!confirm('Bạn có chắc chắn muốn xóa file này?')) {
            return;
        }

        try {
            const response = await fetch(`/api/chat/files/${fileId}`, {
                method: 'DELETE',
                credentials: 'include'
            });

            if (response.ok) {
                // Remove from local arrays
                this.files = this.files.filter(f => f.id !== fileId);
                this.categorizeFiles();
                this.updateFilesCount();
                
                // Re-render current view
                const activeTab = document.querySelector('.files-tab.active').dataset.tab;
                this.switchTab(activeTab);
            }
        } catch (error) {
            alert('Lỗi khi xóa file: ' + error.message);
        }
    }

    // ========== Upload Modal ==========

    showUploadModal() {
        if (!this.currentRoomId) {
            alert('Vui lòng chọn một cuộc trò chuyện trước');
            return;
        }

        const modal = document.getElementById('fileUploadModal');
        if (modal) {
            modal.classList.add('show');
            this.selectedFiles = [];
            this.updateUploadPreview();
        }
    }

    hideUploadModal() {
        const modal = document.getElementById('fileUploadModal');
        if (modal) {
            modal.classList.remove('show');
            this.selectedFiles = [];
            this.updateUploadPreview();
        }
    }

    handleFileSelect(files) {
        this.selectedFiles = Array.from(files).filter(file => this.validateFile(file));
        this.updateUploadPreview();
    }

    validateFile(file) {
        // Check file size (10MB limit)
        if (file.size > this.maxFileSize) {
            this.showError(`File ${file.name} quá lớn. Kích thước tối đa: 10MB`);
            return false;
        }

        // Check file type
        if (!this.allowedTypes.includes(file.type)) {
            this.showError(`File ${file.name} không được hỗ trợ`);
            return false;
        }

        return true;
    }

    showError(message) {
        // Use toastManager for error notifications if available
        if (window.toastManager) {
            window.toastManager.error(message);
        } else {
            alert(message);
        }
    }

    updateUploadPreview() {
        const previewList = document.getElementById('filePreviewList');
        const uploadCount = document.getElementById('uploadCount');
        const confirmBtn = document.getElementById('confirmUploadBtn');

        if (!previewList || !uploadCount || !confirmBtn) return;

        if (this.selectedFiles.length === 0) {
            previewList.innerHTML = '';
            uploadCount.textContent = '0';
            confirmBtn.disabled = true;
            return;
        }

        previewList.innerHTML = this.selectedFiles.map(file => this.createFilePreview(file)).join('');
        uploadCount.textContent = this.selectedFiles.length;
        confirmBtn.disabled = false;

        // Attach remove events
        previewList.querySelectorAll('.file-preview-remove').forEach(btn => {
            btn.addEventListener('click', (e) => {
                const fileName = e.target.closest('.file-preview-item').dataset.fileName;
                this.selectedFiles = this.selectedFiles.filter(f => f.name !== fileName);
                this.updateUploadPreview();
            });
        });
    }

    createFilePreview(file) {
        const iconClass = this.getFileIconClass(this.getFileTypeFromMime(file.type));
        const fileSize = this.formatFileSize(file.size);

        return `
            <div class="file-preview-item" data-file-name="${file.name}">
                <div class="file-preview-icon">
                    <i class="${iconClass}"></i>
                </div>
                <div class="file-preview-info">
                    <div class="file-preview-name" title="${file.name}">${file.name}</div>
                    <div class="file-preview-size">${fileSize}</div>
                </div>
                <button class="file-preview-remove">
                    <i class="fas fa-times"></i>
                </button>
            </div>
        `;
    }

    async uploadSelectedFiles() {
        if (this.selectedFiles.length === 0 || this.isUploading) return;

        this.isUploading = true;
        const confirmBtn = document.getElementById('confirmUploadBtn');
        if (confirmBtn) {
            confirmBtn.disabled = true;
            confirmBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Đang upload...';
        }

        try {
            // First, create a message for the files
            const messageId = await this.createFileMessage();
            if (!messageId) {
                throw new Error('Không thể tạo message cho file');
            }

            const formData = new FormData();
            this.selectedFiles.forEach(file => {
                formData.append('files', file);
            });
            formData.append('messageId', messageId);
            formData.append('roomId', this.currentRoomId);

            const response = await fetch('/api/chat/files/upload-multiple', {
                method: 'POST',
                credentials: 'include',
                body: formData
            });

            if (response.ok) {
                const data = await response.json();
                if (data.success) {
                    // Add new files to local arrays
                    this.files.unshift(...data.data);
                    this.categorizeFiles();
                    this.updateFilesCount();
                    
                    // Re-render current view
                    const activeTab = document.querySelector('.files-tab.active').dataset.tab;
                    this.switchTab(activeTab);
                    
                    // Reload chat messages để hiển thị attachments
                    if (window.chatManager) {
                        window.chatManager.loadRoomMessages(this.currentRoomId);
                    }
                    
                    // Update file management sidebar
                    if (window.chatFileManagement) {
                        data.data.forEach(file => {
                            window.chatFileManagement.addFile(file);
                        });
                    }
                    
                    this.hideUploadModal();
                } else {
                    alert('Upload thất bại: ' + data.message);
                }
            } else {
                alert('Upload thất bại');
            }
        } catch (error) {
            alert('Lỗi khi upload: ' + error.message);
        } finally {
            this.isUploading = false;
            if (confirmBtn) {
                confirmBtn.disabled = false;
                confirmBtn.innerHTML = '<i class="fas fa-upload"></i> Upload <span id="uploadCount">0</span> file';
            }
        }
    }

    // ========== Utility Methods ==========

    async createFileMessage() {
        try {
            // Tạo content text cho sidebar preview
            const fileCount = this.selectedFiles.length;
            const fileNames = this.selectedFiles.map(f => f.name).join(', ');
            const content = `📎 Đã gửi ${fileCount} file: ${fileNames}`;

            const response = await fetch(`/api/chat/rooms/${this.currentRoomId}/messages`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                credentials: 'include',
                body: JSON.stringify({ 
                    content: content, // Có content text cho sidebar
                    messageType: 'FILE'
                })
            });

            if (response.ok) {
                const data = await response.json();
                if (data.success) {
                    return data.data.id;
                }
            }
            return null;
        } catch (error) {
            console.error('Error creating file message:', error);
            return null;
        }
    }

    formatFileSize(bytes) {
        if (bytes === 0) return '0 Bytes';
        const k = 1024;
        const sizes = ['Bytes', 'KB', 'MB', 'GB'];
        const i = Math.floor(Math.log(bytes) / Math.log(k));
        return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
    }

    formatDate(dateString) {
        const date = new Date(dateString);
        const now = new Date();
        const diffMs = now - date;
        const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));

        if (diffDays === 0) {
            return date.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });
        } else if (diffDays === 1) {
            return 'Hôm qua';
        } else if (diffDays < 7) {
            return date.toLocaleDateString('vi-VN', { weekday: 'short' });
        } else {
            return date.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit' });
        }
    }

    getFileTypeFromMime(mimeType) {
        if (mimeType.startsWith('image/')) return 'image';
        if (mimeType.startsWith('video/')) return 'video';
        if (mimeType.startsWith('application/') || mimeType.startsWith('text/')) return 'document';
        return 'other';
    }

    // ========== Public API ==========

    setCurrentRoom(roomId) {
        this.currentRoomId = roomId;
        const sidebar = document.getElementById('chatFilesSidebar');
        if (sidebar && sidebar.classList.contains('show')) {
            this.loadRoomFiles();
        }
    }

    addFile(file) {
        this.files.unshift(file);
        this.categorizeFiles();
        this.updateFilesCount();
        
        // Re-render current view if sidebar is open
        const sidebar = document.getElementById('chatFilesSidebar');
        if (sidebar && sidebar.classList.contains('show')) {
            const activeTab = document.querySelector('.files-tab.active');
            if (activeTab) {
                this.switchTab(activeTab.dataset.tab);
            }
        }
    }
}

// Initialize Chat Files Manager
let chatFilesManager;

document.addEventListener('DOMContentLoaded', function() {
    console.log('ChatFilesManager: DOM loaded, initializing...');
    try {
        chatFilesManager = new ChatFilesManager();
        console.log('ChatFilesManager: Successfully initialized');
        console.log('chatFilesManager.downloadFile:', chatFilesManager.downloadFile);
        
        // Integrate with existing chat manager
        if (window.chatManager) {
            console.log('ChatFilesManager: Integrating with existing chat manager');
            // Override selectRoom method to update files manager
            const originalSelectRoom = window.chatManager.selectRoom;
            window.chatManager.selectRoom = function(roomId) {
                originalSelectRoom.call(this, roomId);
                chatFilesManager.setCurrentRoom(roomId);
            };
        } else {
            console.log('ChatFilesManager: No existing chat manager found');
        }
    } catch (error) {
        console.error('ChatFilesManager: Error during initialization:', error);
    }
});
