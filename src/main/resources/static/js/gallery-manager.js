// Gallery Manager - Handles image gallery, lightbox, and file management

// Global variables for lightbox
let currentPostId = null;
let currentImageIndex = 0;
let currentImages = [];
let allFilesData = {};

// Initialize gallery functionality
document.addEventListener('DOMContentLoaded', function() {
    initializeLazyLoading();
    initializeGalleryEvents();
    initializeModalCleanup();
});

// Initialize modal cleanup listeners
function initializeModalCleanup() {
    // Global cleanup for any modal issues
    document.addEventListener('click', function(e) {
        // If clicking on emergency close button
        if (e.target.closest('[onclick*="forceCloseModal"]')) {
            e.preventDefault();
            forceCloseModal();
        }
    });
    
    // Listen for modal hidden events
    document.addEventListener('hidden.bs.modal', function(e) {
        // Clean up any remaining modal states
        setTimeout(() => {
            if (document.body.classList.contains('modal-open')) {
                document.body.classList.remove('modal-open');
                document.body.style.overflow = '';
                document.body.style.paddingRight = '';
            }
            
            // Remove any remaining backdrops
            const backdrops = document.querySelectorAll('.modal-backdrop');
            backdrops.forEach(backdrop => backdrop.remove());
        }, 100);
    });
    
    // Emergency cleanup on page visibility change
    document.addEventListener('visibilitychange', function() {
        if (document.hidden) {
            // Clean up when page becomes hidden
            forceCloseModal();
        }
    });
}

// Lazy loading for images
function initializeLazyLoading() {
    const lazyImages = document.querySelectorAll('.lazy-load');
    
    if ('IntersectionObserver' in window) {
        const imageObserver = new IntersectionObserver((entries, observer) => {
            entries.forEach(entry => {
                if (entry.isIntersecting) {
                    const img = entry.target;
                    img.src = img.dataset.src || img.src;
                    img.classList.add('loaded');
                    observer.unobserve(img);
                }
            });
        });

        lazyImages.forEach(img => imageObserver.observe(img));
    } else {
        // Fallback for older browsers
        lazyImages.forEach(img => {
            img.src = img.dataset.src || img.src;
            img.classList.add('loaded');
        });
    }
}

// Initialize gallery event listeners
function initializeGalleryEvents() {
    // Keyboard navigation for lightbox
    document.addEventListener('keydown', function(e) {
        const lightboxModal = document.getElementById('lightboxModal');
        if (lightboxModal && lightboxModal.classList.contains('show')) {
            switch(e.key) {
                case 'ArrowLeft':
                    e.preventDefault();
                    changeLightboxImage(-1);
                    break;
                case 'ArrowRight':
                    e.preventDefault();
                    changeLightboxImage(1);
                    break;
                case 'Escape':
                    e.preventDefault();
                    bootstrap.Modal.getInstance(lightboxModal).hide();
                    break;
                case 'f':
                case 'F':
                    e.preventDefault();
                    toggleFullscreen();
                    break;
            }
        }
    });
}

// Open lightbox for images
function openLightbox(postId, imageIndex) {
    currentPostId = postId;
    currentImageIndex = parseInt(imageIndex);
    
    // Try to get images from server data first, fallback to DOM
    if (window.postFilesData && window.postFilesData[postId]) {
        currentImages = window.postFilesData[postId]
            .filter(file => file.fileType === 'image')
            .map(file => ({
                src: file.previewUrl,
                alt: file.originalName,
                downloadUrl: file.downloadUrl
            }));
    } else {
        // Fallback to DOM extraction
        const postElement = document.querySelector(`[data-post-id="${postId}"]`);
        if (!postElement) return;
        
        const images = postElement.querySelectorAll('.gallery-image img');
        currentImages = Array.from(images).map(img => ({
            src: img.src,
            alt: img.alt,
            downloadUrl: img.closest('.gallery-image').dataset.downloadUrl || img.src
        }));
    }
    
    if (currentImages.length === 0) {
        // If no images found, try to load from server
        loadImagesForLightbox(postId, imageIndex);
        return;
    }
    
    // Show lightbox
    showLightboxImage();
    
    // Get modal element and ensure it's properly initialized
    const lightboxModalElement = document.getElementById('lightboxModal');
    
    // Hide any existing modal instances first
    const existingModal = bootstrap.Modal.getInstance(lightboxModalElement);
    if (existingModal) {
        existingModal.hide();
    }
    
    // Create new modal instance
    const lightboxModal = new bootstrap.Modal(lightboxModalElement, {
        backdrop: true,
        keyboard: true,
        focus: true
    });
    
    // Add event listeners for proper cleanup
    lightboxModalElement.addEventListener('hidden.bs.modal', function() {
        // Clean up any remaining modal states
        document.body.classList.remove('modal-open');
        document.body.style.overflow = '';
        document.body.style.paddingRight = '';
        
        // Remove any remaining backdrop
        const backdrops = document.querySelectorAll('.modal-backdrop');
        backdrops.forEach(backdrop => backdrop.remove());
    });
    
    lightboxModal.show();
}

// Load images from server for lightbox
async function loadImagesForLightbox(postId, imageIndex) {
    try {
        const response = await authenticatedFetch(`/api/files/post/${postId}`, {
            method: 'GET'
        });
        
        if (!response || !response.ok) {
            throw new Error(`HTTP error! status: ${response?.status || 'No response'}`);
        }
        
        const files = await response.json();
        
        // Filter only images
        currentImages = files
            .filter(file => file.fileType === 'image')
            .map(file => ({
                src: file.previewUrl,
                alt: file.originalName,
                downloadUrl: file.downloadUrl
            }));
        
        // Store in global data for future use
        if (!window.postFilesData) window.postFilesData = {};
        window.postFilesData[postId] = files;
        
        if (currentImages.length === 0) {
            showToast('Không có hình ảnh nào để hiển thị', 'warning');
            return;
        }
        
        // Show lightbox
        showLightboxImage();
        
        // Get modal element and ensure it's properly initialized
        const lightboxModalElement = document.getElementById('lightboxModal');
        
        // Hide any existing modal instances first
        const existingModal = bootstrap.Modal.getInstance(lightboxModalElement);
        if (existingModal) {
            existingModal.hide();
        }
        
        // Create new modal instance
        const lightboxModal = new bootstrap.Modal(lightboxModalElement, {
            backdrop: true,
            keyboard: true,
            focus: true
        });
        
        lightboxModal.show();
        
    } catch (error) {
        console.error('Error loading images for lightbox:', error);
        showToast('Không thể tải hình ảnh', 'error');
    }
}

// Show current image in lightbox
function showLightboxImage() {
    const lightboxImage = document.getElementById('lightboxImage');
    const lightboxTitle = document.getElementById('lightboxTitle');
    const lightboxCounter = document.getElementById('lightboxCounter');
    const prevBtn = document.getElementById('prevBtn');
    const nextBtn = document.getElementById('nextBtn');
    
    if (currentImages.length === 0) return;
    
    const currentImage = currentImages[currentImageIndex];
    lightboxImage.src = currentImage.src;
    lightboxImage.alt = currentImage.alt;
    lightboxTitle.textContent = currentImage.alt || 'Hình ảnh';
    lightboxCounter.textContent = `${currentImageIndex + 1} / ${currentImages.length}`;
    
    // Update navigation buttons
    prevBtn.style.display = currentImages.length > 1 ? 'block' : 'none';
    nextBtn.style.display = currentImages.length > 1 ? 'block' : 'none';
    
    // Update download button
    const downloadBtn = document.getElementById('downloadBtn');
    downloadBtn.onclick = () => downloadFile(currentImage.downloadUrl);
}

// Change lightbox image
function changeLightboxImage(direction) {
    if (currentImages.length <= 1) return;
    
    currentImageIndex += direction;
    
    if (currentImageIndex < 0) {
        currentImageIndex = currentImages.length - 1;
    } else if (currentImageIndex >= currentImages.length) {
        currentImageIndex = 0;
    }
    
    showLightboxImage();
}

// Download current image
function downloadCurrentImage() {
    if (currentImages.length > 0) {
        downloadFile(currentImages[currentImageIndex].downloadUrl);
    }
}

// Download file
function downloadFile(url) {
    const link = document.createElement('a');
    link.href = url;
    link.download = '';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
}

// Toggle fullscreen
function toggleFullscreen() {
    const lightboxModal = document.getElementById('lightboxModal');
    const modalDialog = lightboxModal.querySelector('.modal-dialog');
    
    if (!document.fullscreenElement) {
        modalDialog.requestFullscreen().catch(err => {
            console.log('Error attempting to enable fullscreen:', err);
        });
    } else {
        document.exitFullscreen();
    }
}

// Force close modal and cleanup
function forceCloseModal() {
    // Close any open modals
    const modals = document.querySelectorAll('.modal.show');
    modals.forEach(modal => {
        const modalInstance = bootstrap.Modal.getInstance(modal);
        if (modalInstance) {
            modalInstance.hide();
        }
    });
    
    // Clean up body classes and styles
    document.body.classList.remove('modal-open');
    document.body.style.overflow = '';
    document.body.style.paddingRight = '';
    
    // Remove all modal backdrops
    const backdrops = document.querySelectorAll('.modal-backdrop');
    backdrops.forEach(backdrop => backdrop.remove());
    
    // Remove any remaining modal classes
    document.querySelectorAll('.modal').forEach(modal => {
        modal.classList.remove('show');
        modal.style.display = 'none';
    });
}

// Close lightbox when clicking outside image
function closeLightboxOnClick(event) {
    // Only close if clicking on the modal body, not on the image or controls
    if (event.target.classList.contains('modal-body')) {
        const lightboxModal = document.getElementById('lightboxModal');
        const modalInstance = bootstrap.Modal.getInstance(lightboxModal);
        if (modalInstance) {
            modalInstance.hide();
        }
    }
}

// Show all files modal
function showAllFiles(postId) {
    currentPostId = postId;
    
    // Show loading state
    const filesGrid = document.getElementById('allFilesGrid');
    filesGrid.innerHTML = `
        <div class="col-12 text-center py-5">
            <div class="spinner-border text-primary" role="status">
                <span class="visually-hidden">Đang tải...</span>
            </div>
            <p class="mt-3 text-muted">Đang tải tất cả tệp đính kèm...</p>
        </div>
    `;
    
    // Show modal first
    const allFilesModal = new bootstrap.Modal(document.getElementById('allFilesModal'));
    allFilesModal.show();
    
    // Load all files from server
    loadAllFilesFromServer(postId);
}

// Load all files from server
async function loadAllFilesFromServer(postId) {
    try {
        const response = await authenticatedFetch(`/api/files/post/${postId}`, {
            method: 'GET'
        });
        
        if (!response || !response.ok) {
            throw new Error(`HTTP error! status: ${response?.status || 'No response'}`);
        }
        
        const files = await response.json();
        displayAllFiles(files, postId);
        
    } catch (error) {
        console.error('Error loading files:', error);
        const filesGrid = document.getElementById('allFilesGrid');
        filesGrid.innerHTML = `
            <div class="col-12 text-center py-5">
                <i class="fa fa-exclamation-triangle fa-3x text-warning mb-3"></i>
                <h5 class="text-muted">Không thể tải tệp đính kèm</h5>
                <p class="text-muted">Vui lòng thử lại sau</p>
                <button class="btn btn-primary" onclick="loadAllFilesFromServer('${postId}')">
                    <i class="fa fa-refresh me-1"></i>Thử lại
                </button>
            </div>
        `;
    }
}

// Display all files in modal
function displayAllFiles(files, postId) {
    const filesGrid = document.getElementById('allFilesGrid');
    filesGrid.innerHTML = '';
    
    if (!files || files.length === 0) {
        filesGrid.innerHTML = `
            <div class="col-12 text-center py-5">
                <i class="fa fa-folder-open fa-3x text-muted mb-3"></i>
                <h5 class="text-muted">Không có tệp đính kèm</h5>
            </div>
        `;
        return;
    }
    
    files.forEach((file, index) => {
        const col = document.createElement('div');
        col.className = 'col-12 col-md-6 col-lg-4';
        
        let content = '';
        
        if (file.fileType === 'image') {
            content = `
                <div class="card border-0 shadow-sm h-100">
                    <div class="card-img-top" style="height: 200px; overflow: hidden; cursor: pointer;" 
                         onclick="openLightbox('${postId}', ${index})">
                        <img src="${file.previewUrl}" alt="${file.originalName}" 
                             class="img-fluid w-100 h-100" style="object-fit: cover;">
                        <div class="position-absolute top-0 end-0 m-2">
                            <span class="badge bg-dark bg-opacity-75">
                                <i class="fa fa-search-plus"></i>
                            </span>
                        </div>
                    </div>
                    <div class="card-body p-3 d-flex flex-column">
                        <h6 class="card-title text-truncate mb-2">${file.originalName}</h6>
                        <div class="mt-auto">
                            <div class="d-flex justify-content-between align-items-center">
                                <small class="text-muted">
                                    <i class="fa fa-image me-1"></i>
                                    ${formatFileSize(file.fileSize)}
                                </small>
                                <div class="btn-group btn-group-sm">
                                    <button class="btn btn-outline-primary" onclick="downloadFile('${file.downloadUrl}')" title="Tải về">
                                        <i class="fa fa-download"></i>
                                    </button>
                                    <button class="btn btn-outline-secondary" onclick="openLightbox('${postId}', ${index})" title="Xem">
                                        <i class="fa fa-eye"></i>
                                    </button>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            `;
        } else if (file.fileType === 'video') {
            content = `
                <div class="card border-0 shadow-sm h-100">
                    <div class="card-img-top position-relative" style="height: 200px; overflow: hidden;">
                        <video src="${file.previewUrl}" class="img-fluid w-100 h-100" style="object-fit: cover;" 
                               controls preload="metadata">
                            <source src="${file.previewUrl}">
                        </video>
                        <div class="position-absolute top-0 end-0 m-2">
                            <span class="badge bg-dark bg-opacity-75">
                                <i class="fa fa-play"></i>
                            </span>
                        </div>
                    </div>
                    <div class="card-body p-3 d-flex flex-column">
                        <h6 class="card-title text-truncate mb-2">${file.originalName}</h6>
                        <div class="mt-auto">
                            <div class="d-flex justify-content-between align-items-center">
                                <small class="text-muted">
                                    <i class="fa fa-video me-1"></i>
                                    ${formatFileSize(file.fileSize)}
                                </small>
                                <button class="btn btn-outline-primary btn-sm" onclick="downloadFile('${file.downloadUrl}')" title="Tải về">
                                    <i class="fa fa-download"></i>
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
            `;
        } else {
            // Document type
            const fileIcon = getFileIcon(file.originalName);
            content = `
                <div class="card border-0 shadow-sm h-100">
                    <div class="card-body text-center p-4 d-flex flex-column">
                        <div class="flex-grow-1 d-flex flex-column justify-content-center">
                            <i class="${fileIcon} fa-3x text-muted mb-3"></i>
                            <h6 class="card-title text-truncate">${file.originalName}</h6>
                        </div>
                        <div class="mt-auto">
                            <div class="d-flex justify-content-between align-items-center">
                                <small class="text-muted">
                                    <i class="fa fa-file me-1"></i>
                                    ${formatFileSize(file.fileSize)}
                                </small>
                                <button class="btn btn-outline-primary btn-sm" onclick="downloadFile('${file.downloadUrl}')" title="Tải về">
                                    <i class="fa fa-download"></i>
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
            `;
        }
        
        col.innerHTML = content;
        filesGrid.appendChild(col);
    });
    
    // Update download all button
    const downloadAllBtn = document.getElementById('downloadAllBtn');
    downloadAllBtn.onclick = () => downloadAllFilesFromModal(files);
}

// Get file icon based on file extension
function getFileIcon(fileName) {
    const extension = fileName.split('.').pop().toLowerCase();
    
    switch (extension) {
        case 'pdf':
            return 'fa fa-file-pdf text-danger';
        case 'doc':
        case 'docx':
            return 'fa fa-file-word text-primary';
        case 'xls':
        case 'xlsx':
            return 'fa fa-file-excel text-success';
        case 'ppt':
        case 'pptx':
            return 'fa fa-file-powerpoint text-warning';
        case 'txt':
            return 'fa fa-file-alt text-secondary';
        case 'zip':
        case 'rar':
            return 'fa fa-file-archive text-warning';
        default:
            return 'fa fa-file text-muted';
    }
}

// Download all files as ZIP
async function downloadAllFiles(postId) {
    try {
        showToast('Đang tạo file ZIP...', 'info');
        
        const response = await authenticatedFetch(`/api/files/download-all/${postId}`, {
            method: 'GET'
        });
        
        if (!response || !response.ok) {
            throw new Error(`HTTP error! status: ${response?.status || 'No response'}`);
        }
        
        // Get the ZIP file as blob
        const blob = await response.blob();
        
        // Create download link
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `post_${postId}_files.zip`;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        window.URL.revokeObjectURL(url);
        
        showToast('Đã tải về file ZIP thành công!', 'success');
        
    } catch (error) {
        console.error('Error downloading ZIP:', error);
        showToast('Không thể tải về file ZIP', 'error');
    }
}

// Download all files from modal
function downloadAllFilesFromModal(files) {
    if (!files || files.length === 0) {
        showToast('Không có tệp nào để tải về', 'warning');
        return;
    }
    
    // Use the ZIP download function
    downloadAllFiles(currentPostId);
}

// Use toastManager for notifications
function showToast(message, type = 'info') {
    if (window.toastManager) {
        window.toastManager.show(message, type);
    } else {
        // Fallback to console if no toast system available
        console.log(`Toast (${type}): ${message}`);
    }
}

// Utility function to format file size
function formatFileSize(bytes) {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
}

// Initialize gallery on page load
document.addEventListener('DOMContentLoaded', function() {
    // Add click handlers for gallery items
    document.querySelectorAll('.gallery-image').forEach((item, index) => {
        item.addEventListener('click', function() {
            const postId = this.closest('[data-post-id]').dataset.postId;
            openLightbox(postId, index);
        });
    });
    
    // Add click handlers for more items indicator
    document.querySelectorAll('.more-items-indicator').forEach((item) => {
        item.addEventListener('click', function() {
            const postId = this.closest('[data-post-id]').dataset.postId;
            showAllFiles(postId);
        });
    });
});
