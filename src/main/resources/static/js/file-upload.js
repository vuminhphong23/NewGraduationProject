// File Upload Management
class FileUploadManager {
    constructor() {
        this.selectedFiles = [];
        this.uploadedFiles = [];
        this.currentPostId = null;
        this.init();
    }

    init() {
        this.setupDragAndDrop();
        this.setupFileInputs();
    }

    setupDragAndDrop() {
        const uploadArea = document.getElementById('fileUploadArea');
        if (!uploadArea) return;

        uploadArea.addEventListener('dragover', (e) => {
            e.preventDefault();
            uploadArea.classList.add('dragover');
        });

        uploadArea.addEventListener('dragleave', (e) => {
            e.preventDefault();
            uploadArea.classList.remove('dragover');
        });

        uploadArea.addEventListener('drop', (e) => {
            e.preventDefault();
            uploadArea.classList.remove('dragover');
            
            const files = Array.from(e.dataTransfer.files);
            this.handleFiles(files);
        });
    }

    setupFileInputs() {
        const fileInput = document.getElementById('fileInput');
        const documentInput = document.getElementById('documentInput');

        if (fileInput) {
            fileInput.addEventListener('change', (e) => {
                const files = Array.from(e.target.files);
                this.handleFiles(files);
            });
        }

        if (documentInput) {
            documentInput.addEventListener('change', (e) => {
                const files = Array.from(e.target.files);
                this.handleFiles(files);
            });
        }
    }

    handleFiles(files) {
        
        files.forEach(file => {
            if (this.validateFile(file)) {
                this.selectedFiles.push(file);
            }
        });
        
        this.updateFilePreview();
        this.showFilePreviewArea();
        
        // Clear file inputs to prevent double selection
        this.clearFileInputs();
    }

    validateFile(file) {
        // Check if file is empty
        if (!file || file.size === 0) {
            this.showError(`File ${file.name} trống hoặc không hợp lệ`);
            return false;
        }

        // Check file size (10MB limit)
        const maxSize = 10 * 1024 * 1024; // 10MB
        if (file.size > maxSize) {
            this.showError(`File ${file.name} quá lớn. Kích thước tối đa: 10MB`);
            return false;
        }

        // Check file type
        const allowedTypes = [
            'image/jpeg', 'image/png', 'image/gif', 'image/webp',
            'video/mp4', 'video/avi', 'video/mov', 'video/wmv',
            'application/pdf', 'application/msword', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
            'text/plain', 'application/vnd.ms-excel', 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
            'application/vnd.ms-powerpoint', 'application/vnd.openxmlformats-officedocument.presentationml.presentation',
            'application/zip', 'application/x-zip-compressed', 'application/x-rar-compressed',
            'application/x-7z-compressed', 'application/x-tar', 'application/gzip'
        ];

        if (!allowedTypes.includes(file.type)) {
            this.showError(`File ${file.name} không được hỗ trợ`);
            return false;
        }

        return true;
    }

    updateFilePreview() {
        const previewList = document.getElementById('filePreviewList');
        if (!previewList) return;

        previewList.innerHTML = '';
        
        this.selectedFiles.forEach((file, index) => {
            const filePreview = this.createFilePreview(file, index);
            previewList.appendChild(filePreview);
        });
    }

    createFilePreview(file, index) {
        const col = document.createElement('div');
        col.className = 'col-12 col-md-6 col-lg-4';
        
        const card = document.createElement('div');
        card.className = 'file-preview-card card border-0 bg-light';
        
        let previewContent = '';
        
        if (file.type.startsWith('image/')) {
            previewContent = `
                <div class="document-image">
                    <img src="${URL.createObjectURL(file)}" alt="${file.name}" 
                         class="img-fluid rounded" style="max-height: 150px; width: 100%; object-fit: cover;">
                </div>
            `;
        } else if (file.type.startsWith('video/')) {
            previewContent = `
                <div class="document-video">
                    <video src="${URL.createObjectURL(file)}" class="img-fluid rounded" 
                           style="max-height: 150px; width: 100%;" controls>
                        <source src="${URL.createObjectURL(file)}" type="${file.type}">
                    </video>
                </div>
            `;
        } else {
            previewContent = `
                <div class="document-icon text-center py-3">
                    <i class="fa fa-file-alt fa-3x"></i>
                </div>
            `;
        }
        
        card.innerHTML = `
            ${previewContent}
            <div class="card-body p-2 file-info">
                <div class="d-flex align-items-center justify-content-between">
                    <div class="flex-grow-1">
                        <div class="fw-semibold small text-truncate" title="${file.name}">${file.name}</div>
                        <div class="file-size-badge">${this.formatFileSize(file.size)}</div>
                    </div>
                    <div class="file-actions">
                        <button class="btn btn-sm btn-outline-danger" onclick="fileUploadManager.removeFile(${index})" title="Xóa">
                            <i class="fa fa-trash"></i>
                        </button>
                    </div>
                </div>
            </div>
        `;
        
        col.appendChild(card);
        return col;
    }

    removeFile(index) {
        this.selectedFiles.splice(index, 1);
        this.updateFilePreview();
        
        if (this.selectedFiles.length === 0) {
            this.hideFilePreviewArea();
        }
    }

    showFilePreviewArea() {
        const previewArea = document.getElementById('filePreviewArea');
        if (previewArea) {
            previewArea.classList.remove('d-none');
        }
    }

    hideFilePreviewArea() {
        const previewArea = document.getElementById('filePreviewArea');
        if (previewArea) {
            previewArea.classList.add('d-none');
        }
    }

    formatFileSize(bytes) {
        if (bytes === 0) return '0 Bytes';
        const k = 1024;
        const sizes = ['Bytes', 'KB', 'MB', 'GB'];
        const i = Math.floor(Math.log(bytes) / Math.log(k));
        return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
    }

    async uploadFiles(postId) {
        if (this.selectedFiles.length === 0) {
            return [];
        }


        this.currentPostId = postId;
        const formData = new FormData();
        
        this.selectedFiles.forEach(file => {
            formData.append('files', file);
        });
        formData.append('postId', postId);

        try {
            const response = await fetch('/api/files/upload-multiple', {
                method: 'POST',
                body: formData
            });


            if (!response.ok) {
                const errorText = await response.text();
                console.error('Upload error:', errorText);
                throw new Error(`HTTP error! status: ${response.status} - ${errorText}`);
            }

            const uploadedFiles = await response.json();
            this.uploadedFiles = uploadedFiles;
            
            // Clear selected files after successful upload
            this.selectedFiles = [];
            this.updateFilePreview();
            this.hideFilePreviewArea();
            
            return uploadedFiles;
        } catch (error) {
            console.error('Upload error details:', error);
            this.showError('Có lỗi xảy ra khi upload file: ' + error.message);
            throw error;
        }
    }

    showError(message) {
        // Use toastManager for error notifications
        if (window.toastManager) {
            window.toastManager.error(message);
        } else {
            alert(message);
        }
    }

    clear() {
        this.selectedFiles = [];
        this.uploadedFiles = [];
        this.updateFilePreview();
        this.hideFilePreviewArea();
        this.clearFileInputs();
    }
    
    clearFileInputs() {
        const fileInput = document.getElementById('fileInput');
        const documentInput = document.getElementById('documentInput');
        
        if (fileInput) fileInput.value = '';
        if (documentInput) documentInput.value = '';
    }
}

// Global instance
let fileUploadManager;

// Initialize when DOM is loaded
document.addEventListener('DOMContentLoaded', function() {
    // Prevent multiple initializations
    if (window.fileUploadManager) {
        return;
    }
    
    fileUploadManager = new FileUploadManager();
    window.fileUploadManager = fileUploadManager;
});

// Global function for file input change (for backward compatibility)
function handleFileSelect(input, type) {
    if (window.fileUploadManager) {
        const files = Array.from(input.files);
        window.fileUploadManager.handleFiles(files);
    }
}
