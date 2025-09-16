// Enhanced Smart Search Script
console.log('Enhanced Smart Search Script loaded');

document.addEventListener('DOMContentLoaded', function() {
    console.log('DOM loaded, initializing enhanced smart search...');
    
    const searchInput = document.getElementById('smartSearchInput');
    const searchDropdown = document.getElementById('smartSearchDropdown');
    const searchResults = document.getElementById('searchResults');
    const recentSearchesList = document.getElementById('recentSearchesList');
    const trendingSearchesList = document.getElementById('trendingSearchesList');
    const liveResultsList = document.getElementById('liveResultsList');
    const noResults = document.getElementById('noResults');
    const searchClearBtn = document.getElementById('searchClearBtn');
    const clearRecentBtn = document.getElementById('clearRecentBtn');
    
    if (!searchInput) {
        console.error('Search input not found!');
        return;
    }

    let searchTimeout;
    let currentCategory = 'all';
    let searchHistory = JSON.parse(localStorage.getItem('searchHistory') || '[]');
    let trendingSearches = ['Spring Boot', 'Java', 'JavaScript', 'React', 'Vue.js', 'Angular', 'Node.js', 'Python'];

    // Initialize search interface
    initSearchInterface();

    // Event listeners
    searchInput.addEventListener('input', handleSearchInput);
    searchInput.addEventListener('keydown', handleKeyDown);
    searchInput.addEventListener('focus', showDropdown);
    searchInput.addEventListener('blur', hideDropdown);
    
    if (searchClearBtn) {
        searchClearBtn.addEventListener('click', clearSearch);
    }

    // Category selection
    document.querySelectorAll('.category-item').forEach(item => {
        item.addEventListener('click', function(e) {
            e.preventDefault();
            selectCategory(this.dataset.category);
        });
    });

    // Clear recent searches
    if (clearRecentBtn) {
        clearRecentBtn.addEventListener('click', clearRecentSearches);
    }

    function initSearchInterface() {
        loadRecentSearches();
        loadTrendingSearches();
        updateClearButton();
    }

    function handleSearchInput(e) {
        const query = e.target.value.trim();
        updateClearButton();
        
        if (query.length === 0) {
            showDefaultContent();
            return;
        }

        if (query.length < 2) {
            hideLiveResults();
            return;
        }

        // Debounce search
        clearTimeout(searchTimeout);
        searchTimeout = setTimeout(() => {
            performLiveSearch(query);
        }, 300);
    }

    function handleKeyDown(e) {
        if (e.key === 'Enter') {
            e.preventDefault();
            const query = searchInput.value.trim();
            if (query.length >= 2) {
                performSearch(query);
            } else {
                showMessage('Vui lòng nhập ít nhất 2 ký tự để tìm kiếm', 'warning');
            }
        } else if (e.key === 'Escape') {
            hideDropdown();
            searchInput.blur();
        }
    }

    function showDropdown() {
        if (searchDropdown) {
            searchDropdown.classList.add('show');
            loadRecentSearches();
        }
    }

    function hideDropdown() {
        setTimeout(() => {
            if (searchDropdown) {
                searchDropdown.classList.remove('show');
            }
        }, 200);
    }

    function selectCategory(category) {
        currentCategory = category;
        
        // Update active category
        document.querySelectorAll('.category-item').forEach(item => {
            item.classList.remove('active');
        });
        document.querySelector(`[data-category="${category}"]`).classList.add('active');

        // Perform search if there's a query
        const query = searchInput.value.trim();
        if (query.length >= 2) {
            performLiveSearch(query);
        }
    }

    function performLiveSearch(query) {
        showLoading();
        
        fetch(`/api/search/live?q=${encodeURIComponent(query)}&category=${currentCategory}`)
            .then(response => response.json())
            .then(data => {
                displayLiveResults(data);
            })
            .catch(error => {
                console.error('Live search error:', error);
                hideLiveResults();
            });
    }

    function performSearch(query) {
        // Add to search history
        addToSearchHistory(query);
        
        // Navigate to search results page
        window.location.href = `/search?q=${encodeURIComponent(query)}&category=${currentCategory}`;
    }

    function displayLiveResults(data) {
        hideDefaultContent();
        showLiveResults();
        
        if (!data || data.length === 0) {
            showNoResults();
            return;
        }

        const resultsHtml = data.map(item => {
            if (item.type === 'post') {
                return `
                    <div class="search-item" onclick="selectSearchResult('${item.title}', 'post')">
                        <div class="search-item-icon">
                            <i class="fas fa-newspaper text-primary"></i>
                        </div>
                        <div class="search-item-content">
                            <div class="search-item-title">${item.title}</div>
                            <div class="search-item-subtitle">${item.content.substring(0, 100)}...</div>
                            <div class="search-item-meta">
                                <span><i class="fas fa-user"></i> ${item.author}</span>
                                <span><i class="fas fa-heart"></i> ${item.likeCount}</span>
                            </div>
                        </div>
                    </div>
                `;
            } else if (item.type === 'user') {
                return `
                    <div class="search-item" onclick="selectSearchResult('${item.username}', 'user')">
                        <div class="search-item-icon">
                            <img src="${item.avatar || 'https://cdn.pixabay.com/photo/2023/02/18/11/00/icon-7797704_640.png'}" class="search-item-avatar" alt="Avatar">
                        </div>
                        <div class="search-item-content">
                            <div class="search-item-title">${item.fullName}</div>
                            <div class="search-item-subtitle">@${item.username}</div>
                        </div>
                    </div>
                `;
            } else if (item.type === 'group') {
                return `
                    <div class="search-item" onclick="selectSearchResult('${item.name}', 'group')">
                        <div class="search-item-icon">
                            <i class="fas fa-layer-group text-warning"></i>
                        </div>
                        <div class="search-item-content">
                            <div class="search-item-title">${item.name}</div>
                            <div class="search-item-subtitle">${item.description.substring(0, 100)}...</div>
                            <div class="search-item-meta">
                                <span><i class="fas fa-users"></i> ${item.memberCount} thành viên</span>
                            </div>
                        </div>
                    </div>
                `;
            }
        }).join('');

        liveResultsList.innerHTML = resultsHtml;
    }

    function showDefaultContent() {
        hideLiveResults();
        hideNoResults();
        if (recentSearchesList) recentSearchesList.style.display = 'block';
        if (trendingSearchesList) trendingSearchesList.style.display = 'block';
    }

    function hideDefaultContent() {
        if (recentSearchesList) recentSearchesList.style.display = 'none';
        if (trendingSearchesList) trendingSearchesList.style.display = 'none';
    }

    function showLiveResults() {
        if (liveResultsList) liveResultsList.style.display = 'block';
    }

    function hideLiveResults() {
        if (liveResultsList) liveResultsList.style.display = 'none';
    }

    function showNoResults() {
        hideLiveResults();
        if (noResults) noResults.style.display = 'block';
    }

    function hideNoResults() {
        if (noResults) noResults.style.display = 'none';
    }

    function showLoading() {
        hideNoResults();
        if (liveResultsList) {
            liveResultsList.innerHTML = '<div class="search-loading"><i class="fas fa-spinner fa-spin"></i> Đang tìm kiếm...</div>';
            liveResultsList.style.display = 'block';
        }
    }

    function loadRecentSearches() {
        if (!recentSearchesList) return;
        
        if (searchHistory.length === 0) {
            recentSearchesList.innerHTML = '<div class="no-recent-searches">Chưa có tìm kiếm gần đây</div>';
            return;
        }

        const recentHtml = searchHistory.slice(0, 5).map(item => `
            <div class="search-item" onclick="selectRecentSearch('${item}')">
                <div class="search-item-icon">
                    <i class="fas fa-clock text-muted"></i>
                </div>
                <div class="search-item-content">
                    <div class="search-item-title">${item}</div>
                </div>
            </div>
        `).join('');

        recentSearchesList.innerHTML = recentHtml;
    }

    function loadTrendingSearches() {
        if (!trendingSearchesList) return;
        
        const trendingHtml = trendingSearches.map(item => `
            <div class="search-item" onclick="selectTrendingSearch('${item}')">
                <div class="search-item-icon">
                    <i class="fas fa-fire text-danger"></i>
                </div>
                <div class="search-item-content">
                    <div class="search-item-title">${item}</div>
                </div>
            </div>
        `).join('');

        trendingSearchesList.innerHTML = trendingHtml;
    }

    function addToSearchHistory(query) {
        // Remove if already exists
        searchHistory = searchHistory.filter(item => item !== query);
        // Add to beginning
        searchHistory.unshift(query);
        // Keep only last 10
        searchHistory = searchHistory.slice(0, 10);
        // Save to localStorage
        localStorage.setItem('searchHistory', JSON.stringify(searchHistory));
    }

    function clearRecentSearches() {
        searchHistory = [];
        localStorage.removeItem('searchHistory');
        loadRecentSearches();
    }

    function updateClearButton() {
        if (searchClearBtn) {
            searchClearBtn.style.display = searchInput.value.length > 0 ? 'block' : 'none';
        }
    }

    function clearSearch() {
        searchInput.value = '';
        searchInput.focus();
        showDefaultContent();
        updateClearButton();
    }

    function showMessage(message, type = 'info') {
        // Create a simple toast notification
        const toast = document.createElement('div');
        toast.className = `alert alert-${type} alert-dismissible fade show position-fixed`;
        toast.style.cssText = 'top: 20px; right: 20px; z-index: 9999; min-width: 300px;';
        toast.innerHTML = `
            ${message}
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        `;
        document.body.appendChild(toast);
        
        // Auto remove after 3 seconds
        setTimeout(() => {
            if (toast.parentNode) {
                toast.parentNode.removeChild(toast);
            }
        }, 3000);
    }

    // Global functions for onclick handlers
    window.selectSearchResult = function(query, type) {
        searchInput.value = query;
        performSearch(query);
    };

    window.selectRecentSearch = function(query) {
        searchInput.value = query;
        performSearch(query);
    };

    window.selectTrendingSearch = function(query) {
        searchInput.value = query;
        performSearch(query);
    };
    
    console.log('Enhanced smart search initialized successfully');
});