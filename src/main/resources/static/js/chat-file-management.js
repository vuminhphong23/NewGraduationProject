/**
 * Chat File Management Sidebar
 * Handles file management functionality for chat rooms
 */
class ChatFileManagement {
    constructor() {
        this.currentRoomId = null;
        this.currentTab = 'media';
        this.filesData = {
            media: [],
            files: [],
            links: []
        };
        
        this.init();
    }

    init() {
        this.setupEventListeners();
        console.log('ChatFileManagement: Initialized');
    }

    setupEventListeners() {
        // Tab switching
        document.addEventListener('click', (e) => {
            if (e.target.closest('.files-tab')) {
                const tab = e.target.closest('.files-tab');
                const tabType = tab.dataset.tab;
                this.switchTab(tabType);
            }
        });


        // File item clicks
        document.addEventListener('click', (e) => {
            if (e.target.closest('.month-file-item')) {
                const fileItem = e.target.closest('.month-file-item');
                const fileId = fileItem.dataset.fileId;
                this.handleFileClick(fileId);
            }
        });
    }

    switchTab(tabType) {
        // Update active tab
        document.querySelectorAll('.files-tab').forEach(tab => {
            tab.classList.remove('active');
        });
        document.querySelector(`[data-tab="${tabType}"]`).classList.add('active');

        // Update active content
        document.querySelectorAll('.files-tab-content').forEach(content => {
            content.classList.remove('active');
        });
        document.getElementById(`${tabType}Content`).classList.add('active');

        this.currentTab = tabType;
        
        // Only load content for media and files tabs
        if (tabType === 'media' || tabType === 'files') {
            this.loadTabContent(tabType);
        }
    }

    async loadTabContent(tabType) {
        if (!this.currentRoomId) return;

        try {
            let files = [];
            
            switch (tabType) {
                case 'media':
                    files = await this.fetchRoomMedia(this.currentRoomId);
                    this.renderMediaFiles(files);
                    break;
                case 'files':
                    files = await this.fetchRoomDocuments(this.currentRoomId);
                    this.renderDocumentFiles(files);
                    break;
            }
            
            this.filesData[tabType] = files;
        } catch (error) {
            console.error('Error loading tab content:', error);
        }
    }

    async fetchRoomMedia(roomId) {
        try {
            const response = await authenticatedFetch(`/api/chat/rooms/${roomId}/media`, {
                credentials: 'include'
            });
            
            if (!response.ok) {
                throw new Error('Failed to fetch media files');
            }
            
            const result = await response.json();
            return result.success ? result.data : [];
        } catch (error) {
            console.error('Error fetching room media:', error);
            return [];
        }
    }

    async fetchRoomDocuments(roomId) {
        try {
            const response = await authenticatedFetch(`/api/chat/rooms/${roomId}/documents`, {
                credentials: 'include'
            });
            
            if (!response.ok) {
                throw new Error('Failed to fetch document files');
            }
            
            const result = await response.json();
            return result.success ? result.data : [];
        } catch (error) {
            console.error('Error fetching room documents:', error);
            return [];
        }
    }


    renderMediaFiles(files) {
        const container = document.getElementById('mediaByMonth');
        if (!container) return;

        // Group files by month
        const filesByMonth = this.groupFilesByMonth(files);
        
        let html = '';
        for (const [month, monthFiles] of Object.entries(filesByMonth)) {
            html += `
                <div class="month-section">
                    <div class="month-header">${month}</div>
                    <div class="month-files-grid">
                        ${monthFiles.map(file => this.createMediaFileHTML(file)).join('')}
                    </div>
                </div>
            `;
        }
        
        container.innerHTML = html || '<div class="no-files"><i class="fas fa-images"></i>Chưa có file phương tiện nào</div>';
    }

    renderDocumentFiles(files) {
        const container = document.getElementById('filesByMonth');
        if (!container) return;

        // Group files by month
        const filesByMonth = this.groupFilesByMonth(files);
        
        let html = '';
        for (const [month, monthFiles] of Object.entries(filesByMonth)) {
            html += `
                <div class="month-section">
                    <div class="month-header">${month}</div>
                    <div class="month-files-grid">
                        ${monthFiles.map(file => this.createDocumentFileHTML(file)).join('')}
                    </div>
                </div>
            `;
        }
        
        container.innerHTML = html || '<div class="no-files"><i class="fas fa-file"></i>Chưa có file tài liệu nào</div>';
    }


    groupFilesByMonth(files) {
        const groups = {};
        
        files.forEach(file => {
            // Use current date since FileUploadResponse doesn't have createdAt
            const date = new Date();
            const monthKey = this.formatMonthKey(date);
            
            if (!groups[monthKey]) {
                groups[monthKey] = [];
            }
            groups[monthKey].push(file);
        });
        
        // Sort months in descending order
        const sortedGroups = {};
        Object.keys(groups).sort((a, b) => {
            const dateA = new Date(a);
            const dateB = new Date(b);
            return dateB - dateA;
        }).forEach(key => {
            sortedGroups[key] = groups[key];
        });
        
        return sortedGroups;
    }

    formatMonthKey(date) {
        const months = [
            'Tháng 1', 'Tháng 2', 'Tháng 3', 'Tháng 4', 'Tháng 5', 'Tháng 6',
            'Tháng 7', 'Tháng 8', 'Tháng 9', 'Tháng 10', 'Tháng 11', 'Tháng 12'
        ];
        
        const month = months[date.getMonth()];
        const year = date.getFullYear();
        
        return `${month} ${year}`;
    }

    createMediaFileHTML(file) {
        const fileType = file.fileType || 'other';
        const fileName = file.originalName || file.fileName || 'Unknown file';
        const fileSize = this.formatFileSize(file.fileSize);
        const isImage = fileType === 'image';
        const isVideo = fileType === 'video';
        const isAudio = fileType === 'audio';
        
        if (isImage) {
            return `
                <div class="month-file-item" data-file-id="${file.id}">
                    <div class="file-thumbnail">
                        <img src="${file.previewUrl || file.downloadUrl}" alt="${fileName}" 
                             onerror="this.style.display='none'; this.nextElementSibling.style.display='flex';">
                        <div class="file-icon image" style="display: none;">
                            <i class="fas fa-image"></i>
                        </div>
                    </div>
                    <div class="file-info">
                        <div class="file-name">${fileName}</div>
                        <div class="file-size">${fileSize}</div>
                    </div>
                </div>
            `;
        } else if (isVideo) {
            return `
                <div class="month-file-item" data-file-id="${file.id}">
                    <div class="file-thumbnail">
                        <video class="video-preview" muted>
                            <source src="${file.previewUrl || file.downloadUrl}" type="video/mp4">
                        </video>
                        <div class="file-icon video">
                            <i class="fas fa-play"></i>
                        </div>
                    </div>
                    <div class="file-info">
                        <div class="file-name">${fileName}</div>
                        <div class="file-size">${fileSize}</div>
                    </div>
                </div>
            `;
        } else if (isAudio) {
            return `
                <div class="month-file-item" data-file-id="${file.id}">
                    <div class="file-thumbnail">
                        <div class="file-icon audio">
                            <i class="fas fa-music"></i>
                        </div>
                    </div>
                    <div class="file-info">
                        <div class="file-name">${fileName}</div>
                        <div class="file-size">${fileSize}</div>
                    </div>
                </div>
            `;
        } else {
            return `
                <div class="month-file-item" data-file-id="${file.id}">
                    <div class="file-thumbnail">
                        <div class="file-icon document">
                            <i class="fas fa-file"></i>
                        </div>
                    </div>
                    <div class="file-info">
                        <div class="file-name">${fileName}</div>
                        <div class="file-size">${fileSize}</div>
                    </div>
                </div>
            `;
        }
    }

    createDocumentFileHTML(file) {
        const fileType = file.fileType || 'other';
        const fileName = file.originalName || file.fileName || 'Unknown file';
        const fileSize = this.formatFileSize(file.fileSize);
        const iconClass = this.getFileIconClass(fileType);
        
        return `
            <div class="month-file-item" data-file-id="${file.id}">
                <div class="file-thumbnail">
                    <div class="file-icon document">
                        <i class="${iconClass}"></i>
                    </div>
                </div>
                <div class="file-info">
                    <div class="file-name">${fileName}</div>
                    <div class="file-size">${fileSize}</div>
                </div>
            </div>
        `;
    }


    getFileIconClass(fileType) {
        const iconMap = {
            'pdf': 'fas fa-file-pdf',
            'doc': 'fas fa-file-word',
            'docx': 'fas fa-file-word',
            'xls': 'fas fa-file-excel',
            'xlsx': 'fas fa-file-excel',
            'ppt': 'fas fa-file-powerpoint',
            'pptx': 'fas fa-file-powerpoint',
            'txt': 'fas fa-file-alt',
            'zip': 'fas fa-file-archive',
            'rar': 'fas fa-file-archive',
            'default': 'fas fa-file'
        };
        
        return iconMap[fileType] || iconMap.default;
    }

    formatFileSize(bytes) {
        if (!bytes || bytes === 0) return '0 B';
        
        const k = 1024;
        const sizes = ['B', 'KB', 'MB', 'GB'];
        const i = Math.floor(Math.log(bytes) / Math.log(k));
        
        return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
    }

    handleFileClick(fileId) {
        // Find file data
        const file = this.findFileById(fileId);
        if (!file) return;

        // Open file in new tab or download
        if (file.downloadUrl) {
            window.open(file.downloadUrl, '_blank');
        }
    }

    findFileById(fileId) {
        for (const tabType of ['media', 'files', 'links']) {
            const file = this.filesData[tabType].find(f => f.id == fileId);
            if (file) return file;
        }
        return null;
    }

    openSidebar(roomId) {
        this.currentRoomId = roomId;
        const sidebar = document.getElementById('chatFilesSidebar');
        if (sidebar) {
            sidebar.style.display = 'block';
            sidebar.classList.add('show');
            
            // Load current tab content
            this.loadTabContent(this.currentTab);
        }
    }

    closeSidebar() {
        const sidebar = document.getElementById('chatFilesSidebar');
        if (sidebar) {
            sidebar.classList.remove('show');
            setTimeout(() => {
                sidebar.style.display = 'none';
            }, 300);
        }
    }

    refreshCurrentTab() {
        if (this.currentRoomId) {
            this.loadTabContent(this.currentTab);
        }
    }

    addFile(file) {
        // Add new file to appropriate tab
        const fileType = file.fileType || 'other';
        
        if (['image', 'video', 'audio'].includes(fileType)) {
            this.filesData.media.unshift(file);
            if (this.currentTab === 'media') {
                this.renderMediaFiles(this.filesData.media);
            }
        } else {
            this.filesData.files.unshift(file);
            if (this.currentTab === 'files') {
                this.renderDocumentFiles(this.filesData.files);
            }
        }
    }
}

// Initialize when DOM is loaded
document.addEventListener('DOMContentLoaded', () => {
    window.chatFileManagement = new ChatFileManagement();
    console.log('ChatFileManagement: DOM loaded, initialized');
});

// Export for use in other modules
window.ChatFileManagement = ChatFileManagement;
