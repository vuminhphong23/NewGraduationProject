// Hashtag Manager - Global object for managing hashtags
window.HashtagManager = (function() {
    // State management
    let selectedHashtags = [];
    const maxHashtags = 5;
    
    // DOM references
    let hashtagInput = null;
    let hashtagList = null;
    let hashtagCounter = null;
    
    // Initialize DOM references
    function initializeDOMReferences() {
        hashtagInput = document.getElementById('topic');
        hashtagList = document.getElementById('hashtag-list');
        hashtagCounter = document.getElementById('hashtag-counter');
        
        if (!hashtagInput || !hashtagList || !hashtagCounter) {
            console.error('HashtagManager: Required DOM elements not found');
            return false;
        }
        
        setupEventListeners();
        updateDisplay();
        return true;
    }
    
    // Setup event listeners
    function setupEventListeners() {
        if (hashtagInput) {
            hashtagInput.addEventListener('keydown', function(e) {
                if (e.key === 'Enter' || e.key === ',') {
                    e.preventDefault();
                    const value = this.value.trim();
                    if (value) {
                        addHashtag(value);
                        this.value = '';
                    }
                }
            });
            
            hashtagInput.addEventListener('blur', function() {
                const value = this.value.trim();
                if (value) {
                    addHashtag(value);
                    this.value = '';
                }
            });
        }
    }
    
    // Add hashtag
    function addHashtag(value) {
        const cleanValue = value.trim().toLowerCase();
        
        // Validation
        if (!isValidHashtag(cleanValue)) {
            showToast('Hashtag không hợp lệ! Chỉ được chứa chữ cái, số và dấu gạch dưới', 'error');
            return false;
        }
        
        if (selectedHashtags.length >= maxHashtags) {
            showToast(`Chỉ được phép tối đa ${maxHashtags} hashtag`, 'error');
            return false;
        }
        
        if (selectedHashtags.includes(cleanValue)) {
            showToast('Hashtag đã tồn tại!', 'warning');
            return false;
        }
        
        // Add to list
        selectedHashtags.push(cleanValue);
        updateDisplay();
        showToast('Đã thêm hashtag thành công!', 'success');
        return true;
    }
    
    // Remove hashtag
    function removeHashtag(hashtag) {
        selectedHashtags = selectedHashtags.filter(h => h !== hashtag);
        updateDisplay();
        showToast('Đã xóa hashtag!', 'info');
    }
    
    // Validate hashtag format
    function isValidHashtag(hashtag) {
        return /^[a-zA-Z0-9_]+$/.test(hashtag) && hashtag.length >= 2 && hashtag.length <= 20;
    }
    
    // Update display
    function updateDisplay() {
        if (!hashtagList || !hashtagCounter) return;
        
        // Update counter
        hashtagCounter.textContent = `${selectedHashtags.length}/${maxHashtags}`;
        
        // Update list
        hashtagList.innerHTML = selectedHashtags.map(hashtag => 
            `<span class="badge bg-primary me-1 mb-1">
                #${hashtag}
                <button type="button" class="btn-close btn-close-white ms-1" 
                        onclick="HashtagManager.remove('${hashtag}')" 
                        aria-label="Xóa hashtag">
                </button>
            </span>`
        ).join('');
    }
    
    // Show toast notification
    function showToast(message, type = 'info') {
        const toastId = 'hashtag-toast-' + Date.now();
        const toastHtml = `
            <div id="${toastId}" class="toast align-items-center text-white bg-${type === 'error' ? 'danger' : type === 'warning' ? 'warning' : type === 'success' ? 'success' : 'info'} border-0" role="alert">
                <div class="d-flex">
                    <div class="toast-body">${message}</div>
                    <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button>
                </div>
            </div>
        `;
        
        let toastContainer = document.getElementById('toast-container');
        if (!toastContainer) {
            toastContainer = document.createElement('div');
            toastContainer.id = 'toast-container';
            toastContainer.className = 'toast-container position-fixed top-0 end-0 p-3';
            toastContainer.style.zIndex = '9999';
            document.body.appendChild(toastContainer);
        }
        
        toastContainer.insertAdjacentHTML('beforeend', toastHtml);
        
        const toastElement = document.getElementById(toastId);
        const toast = new bootstrap.Toast(toastElement);
        toast.show();
        
        // Auto remove after hide
        toastElement.addEventListener('hidden.bs.toast', () => {
            toastElement.remove();
        });
    }
    
    // Public API
    return {
        // Initialize the hashtag manager
        init: function() {
            return initializeDOMReferences();
        },
        
        // Re-initialize DOM references (for dynamic content)
        reinit: function() {
            return initializeDOMReferences();
        },
        
        // Add a hashtag
        add: function(hashtag) {
            return addHashtag(hashtag);
        },
        
        // Remove a hashtag
        remove: function(hashtag) {
            removeHashtag(hashtag);
        },
        
        // Get selected hashtags
        getSelected: function() {
            return [...selectedHashtags];
        },
        
        // Set hashtags (for editing)
        setHashtags: function(hashtags) {
            // Ensure DOM is ready
            if (!this.isReady()) {
                this.reinit();
            }
            selectedHashtags = Array.isArray(hashtags) ? [...hashtags] : [];
            updateDisplay();
        },
        
        // Clear all hashtags
        reset: function() {
            selectedHashtags = [];
            updateDisplay();
            if (hashtagInput) {
                hashtagInput.value = '';
            }
        },
        
        // Check if manager is ready
        isReady: function() {
            return hashtagInput !== null && hashtagList !== null && hashtagCounter !== null;
        }
    };
})();

// Auto-initialize when DOM is ready
document.addEventListener('DOMContentLoaded', function() {
    if (!HashtagManager.init()) {
        console.warn('HashtagManager: Failed to initialize. Required elements may not be present.');
    }
});
