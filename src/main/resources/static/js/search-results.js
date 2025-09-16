// Search Results Page Enhancement Script
console.log('Search Results Enhancement Script loaded');

document.addEventListener('DOMContentLoaded', function() {
    console.log('DOM loaded, initializing search results enhancements...');
    
    // Initialize search results enhancements
    initSearchResultsEnhancements();
    
    // Add smooth scrolling for better UX
    addSmoothScrolling();
    
    // Add keyboard navigation
    addKeyboardNavigation();
    
    // Add result card animations
    addResultCardAnimations();
    
    // Add search suggestions functionality
    addSearchSuggestions();
    
    console.log('Search results enhancements initialized successfully');
});

function initSearchResultsEnhancements() {
    // Add loading states for better UX
    addLoadingStates();
    
    // Add result filtering
    addResultFiltering();
    
    // Add infinite scroll (if needed)
    addInfiniteScroll();
    
    // Add search analytics
    addSearchAnalytics();
}

function addSmoothScrolling() {
    // Smooth scroll for anchor links
    document.querySelectorAll('a[href^="#"]').forEach(anchor => {
        anchor.addEventListener('click', function (e) {
            e.preventDefault();
            const target = document.querySelector(this.getAttribute('href'));
            if (target) {
                target.scrollIntoView({
                    behavior: 'smooth',
                    block: 'start'
                });
            }
        });
    });
}

function addKeyboardNavigation() {
    // Add keyboard shortcuts
    document.addEventListener('keydown', function(e) {
        // Ctrl/Cmd + F to focus search
        if ((e.ctrlKey || e.metaKey) && e.key === 'f') {
            e.preventDefault();
            const searchInput = document.getElementById('smartSearchInput');
            if (searchInput) {
                searchInput.focus();
                searchInput.select();
            }
        }
        
        // Escape to clear search
        if (e.key === 'Escape') {
            const searchInput = document.getElementById('smartSearchInput');
            if (searchInput && searchInput.value) {
                searchInput.value = '';
                searchInput.blur();
            }
        }
    });
}

function addResultCardAnimations() {
    // Intersection Observer for fade-in animations
    const observerOptions = {
        threshold: 0.1,
        rootMargin: '0px 0px -50px 0px'
    };
    
    const observer = new IntersectionObserver((entries) => {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                entry.target.style.opacity = '1';
                entry.target.style.transform = 'translateY(0)';
            }
        });
    }, observerOptions);
    
    // Observe all result cards
    document.querySelectorAll('.result-card').forEach((card, index) => {
        card.style.opacity = '0';
        card.style.transform = 'translateY(20px)';
        card.style.transition = `opacity 0.6s ease ${index * 0.1}s, transform 0.6s ease ${index * 0.1}s`;
        observer.observe(card);
    });
}

function addSearchSuggestions() {
    const suggestionTags = document.querySelectorAll('.suggestion-tag');
    
    suggestionTags.forEach(tag => {
        tag.addEventListener('click', function(e) {
            e.preventDefault();
            const suggestion = this.textContent.trim();
            
            // Add loading state
            this.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Đang tìm kiếm...';
            this.style.pointerEvents = 'none';
            
            // Navigate to search results
            setTimeout(() => {
                window.location.href = `/search?q=${encodeURIComponent(suggestion)}&category=all`;
            }, 500);
        });
        
        // Add hover effects
        tag.addEventListener('mouseenter', function() {
            this.style.transform = 'translateY(-2px) scale(1.05)';
        });
        
        tag.addEventListener('mouseleave', function() {
            this.style.transform = 'translateY(0) scale(1)';
        });
    });
}

function addLoadingStates() {
    // Add loading state to filter tabs
    const filterTabs = document.querySelectorAll('.filter-tab');
    
    filterTabs.forEach(tab => {
        tab.addEventListener('click', function(e) {
            // Don't prevent default for active tab
            if (this.classList.contains('active')) {
                return;
            }
            
            // Add loading state
            const originalContent = this.innerHTML;
            this.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Đang tải...';
            this.style.pointerEvents = 'none';
            
            // Reset after navigation
            setTimeout(() => {
                this.innerHTML = originalContent;
                this.style.pointerEvents = 'auto';
            }, 1000);
        });
    });
}

function addResultFiltering() {
    // Add client-side filtering for better UX
    const searchInput = document.getElementById('smartSearchInput');
    if (!searchInput) return;
    
    // Add real-time filtering
    searchInput.addEventListener('input', function() {
        const query = this.value.toLowerCase().trim();
        if (query.length < 2) return;
        
        filterResults(query);
    });
}

function filterResults(query) {
    const resultCards = document.querySelectorAll('.result-card');
    
    resultCards.forEach(card => {
        const title = card.querySelector('.result-title')?.textContent.toLowerCase() || '';
        const content = card.querySelector('.result-content')?.textContent.toLowerCase() || '';
        const meta = card.querySelector('.result-meta')?.textContent.toLowerCase() || '';
        
        const matches = title.includes(query) || content.includes(query) || meta.includes(query);
        
        if (matches) {
            card.style.display = 'block';
            card.style.opacity = '1';
        } else {
            card.style.opacity = '0.3';
            card.style.transform = 'scale(0.98)';
        }
    });
}

function addInfiniteScroll() {
    // Add infinite scroll for large result sets
    const observerOptions = {
        threshold: 0.1,
        rootMargin: '100px'
    };
    
    const observer = new IntersectionObserver((entries) => {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                // Load more results (implement based on backend pagination)
                loadMoreResults();
            }
        });
    }, observerOptions);
    
    // Observe the last result card
    const lastCard = document.querySelector('.result-card:last-child');
    if (lastCard) {
        observer.observe(lastCard);
    }
}

function loadMoreResults() {
    // Implement pagination loading
    console.log('Loading more results...');
    // This would typically make an AJAX request to load more results
}

function addSearchAnalytics() {
    // Track search interactions
    trackSearchInteractions();
    
    // Track result clicks
    trackResultClicks();
    
    // Track filter usage
    trackFilterUsage();
}

function trackSearchInteractions() {
    const searchInput = document.getElementById('smartSearchInput');
    if (!searchInput) return;
    
    let searchStartTime = null;
    
    searchInput.addEventListener('focus', function() {
        searchStartTime = Date.now();
    });
    
    searchInput.addEventListener('blur', function() {
        if (searchStartTime) {
            const searchDuration = Date.now() - searchStartTime;
            console.log(`Search session duration: ${searchDuration}ms`);
            // Send analytics data to server
        }
    });
}

function trackResultClicks() {
    const resultCards = document.querySelectorAll('.result-card');
    
    resultCards.forEach(card => {
        card.addEventListener('click', function() {
            const resultType = this.classList.contains('post-result') ? 'post' : 
                             this.classList.contains('user-result') ? 'user' : 'group';
            
            console.log(`Result clicked: ${resultType}`);
            // Send analytics data to server
        });
    });
}

function trackFilterUsage() {
    const filterTabs = document.querySelectorAll('.filter-tab');
    
    filterTabs.forEach(tab => {
        tab.addEventListener('click', function() {
            const category = this.dataset.category || this.textContent.trim();
            console.log(`Filter used: ${category}`);
            // Send analytics data to server
        });
    });
}

// Utility functions
function debounce(func, wait) {
    let timeout;
    return function executedFunction(...args) {
        const later = () => {
            clearTimeout(timeout);
            func(...args);
        };
        clearTimeout(timeout);
        timeout = setTimeout(later, wait);
    };
}

function throttle(func, limit) {
    let inThrottle;
    return function() {
        const args = arguments;
        const context = this;
        if (!inThrottle) {
            func.apply(context, args);
            inThrottle = true;
            setTimeout(() => inThrottle = false, limit);
        }
    };
}

// Export functions for global use
window.SearchResultsEnhancements = {
    filterResults,
    loadMoreResults,
    debounce,
    throttle
};
