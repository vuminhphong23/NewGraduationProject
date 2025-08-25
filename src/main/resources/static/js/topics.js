// Hashtag Manager - Global object for managing hashtags
window.HashtagManager = (function() {
    // State management
    let selectedHashtags = [];
    const maxHashtags = 5;
    let trendingCache = [];

    // DOM references
    let hashtagInput = null;
    let hashtagList = null;
    let hashtagCounter = null;
    let suggestionBox = null;

    // Initialize DOM references
    function initializeDOMReferences() {
        hashtagInput = document.getElementById('topic');
        hashtagList = document.getElementById('hashtag-list');
        hashtagCounter = document.getElementById('hashtag-counter');
        suggestionBox = document.getElementById('hashtag-suggestions');
        
        if (!hashtagInput || !hashtagList || !hashtagCounter) {
            console.error('HashtagManager: Required DOM elements not found');
            return false;
        }
        
        setupEventListeners();
        updateDisplay();
        loadTrending();
        return true;
    }

    function setupEventListeners() {
        if (hashtagInput) {
            hashtagInput.addEventListener('keydown', function(e) {
                if (e.key === 'Enter' || e.key === ',') {
                    e.preventDefault();
                    const value = this.value.trim();
                    if (value) {
                        addHashtag(value);
                        this.value = '';
                        renderSuggestions([]);
                    }
                }
            });
            
            hashtagInput.addEventListener('input', function() {
                const q = this.value.trim().toLowerCase();
                if (!q) { renderSuggestions(trendingCache.slice(0,8)); return; }
                const filtered = trendingCache
                    .map(t => t.name || t)
                    .filter(name => name.toLowerCase().includes(q) && !selectedHashtags.includes(name.toLowerCase()))
                    .slice(0,8);
                renderSuggestions(filtered);
            });
            
            hashtagInput.addEventListener('focus', function() {
                if (trendingCache.length > 0 && this.value.trim() === '') {
                    renderSuggestions(trendingCache.slice(0,8));
                }
            });
            
            hashtagInput.addEventListener('blur', function() {
                setTimeout(() => renderSuggestions([]), 200);
            });
        }
    }

    async function loadTrending() {
        try {
            const res = await authenticatedFetch('/api/posts/trending-topics', { headers: { 'Accept': 'application/json' } });
            if (res.ok) {
                const topics = await res.json();
                displayTrendingTopics(topics);
            } else {
                console.error('Failed to load trending topics');
            }
        } catch (error) {
            console.error('Error loading trending topics:', error);
        }
    }

    function displayTrendingTopics(topics) {
        try {
            trendingCache = Array.isArray(topics) ? topics : [];
            // Nếu input đang trống, hiển thị gợi ý top 8
            if (hashtagInput && hashtagInput.value.trim() === '') {
                renderSuggestions(trendingCache.slice(0, 8));
            }
        } catch (e) {
            console.error('displayTrendingTopics error:', e);
        }
    }

    function renderSuggestions(items) {
        if (!suggestionBox) return;
        if (!items || items.length === 0) { suggestionBox.innerHTML = ''; return; }
        suggestionBox.innerHTML = `
            <div class="list-group shadow-sm">
                ${items.map(i => {
                    const name = typeof i === 'string' ? i : i.name;
                    const count = typeof i === 'string' ? '' : (i.count ? ` <span class="text-muted small">(${i.count})</span>` : '');
                    return `<button type="button" class="list-group-item list-group-item-action d-flex justify-content-between align-items-center" data-name="${name}">
                                <span>#${name}</span>
                                ${count}
                            </button>`;
                }).join('')}
            </div>
        `;
        suggestionBox.querySelectorAll('[data-name]').forEach(btn => {
            btn.addEventListener('click', () => {
                const name = btn.getAttribute('data-name');
                addHashtag(name);
                if (hashtagInput) hashtagInput.value = '';
                renderSuggestions([]);
            });
        });
    }

    function addHashtag(value) {
        const cleanValue = value.trim().toLowerCase();
        
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
        
        selectedHashtags.push(cleanValue);
        updateDisplay();
        showToast('Đã thêm hashtag thành công!', 'success');
        return true;
    }

    function removeHashtag(hashtag) {
        selectedHashtags = selectedHashtags.filter(h => h !== hashtag);
        updateDisplay();
        showToast('Đã xóa hashtag!', 'info');
    }

    function isValidHashtag(hashtag) {
        return /^[a-zA-Z0-9_]+$/.test(hashtag) && hashtag.length >= 2 && hashtag.length <= 20;
    }

    function updateDisplay() {
        if (!hashtagList || !hashtagCounter) return;
        hashtagCounter.textContent = `${selectedHashtags.length}/${maxHashtags}`;
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
        toastElement.addEventListener('hidden.bs.toast', () => { toastElement.remove(); });
    }

    return {
        init: function() { return initializeDOMReferences(); },
        reinit: function() { return initializeDOMReferences(); },
        add: function(hashtag) { return addHashtag(hashtag); },
        remove: function(hashtag) { removeHashtag(hashtag); },
        getSelected: function() { return [...selectedHashtags]; },
        setHashtags: function(hashtags) {
            if (!this.isReady()) this.reinit();
            selectedHashtags = Array.isArray(hashtags) ? [...hashtags] : [];
            updateDisplay();
        },
        reset: function() {
            selectedHashtags = [];
            updateDisplay();
            if (hashtagInput) hashtagInput.value = '';
            renderSuggestions([]);
        },
        isReady: function() { return hashtagInput !== null && hashtagList !== null && hashtagCounter !== null; }
    };
})();

// Auto-initialize when DOM is ready
document.addEventListener('DOMContentLoaded', function() {
    if (!HashtagManager.init()) {
        console.warn('HashtagManager: Failed to initialize. Required elements may not be present.');
    }
});
