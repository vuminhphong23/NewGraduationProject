/**
 * Profile Navigation - JavaScript
 * Xử lý chuyển đổi tab trong profile
 */

// Initialize when DOM is loaded
document.addEventListener('DOMContentLoaded', function() {
    initializeProfileNavigation();
});

/**
 * Initialize profile navigation functionality
 */
function initializeProfileNavigation() {
    // Handle tab clicks
    document.addEventListener('click', function(e) {
        if (e.target.closest('.profile-nav .nav-link')) {
            e.preventDefault();
            const clickedTab = e.target.closest('.profile-nav .nav-link');
            const tabName = getTabNameFromHref(clickedTab.getAttribute('href'));
            
            if (tabName) {
                switchToTab(tabName);
            }
        }
    });
}

/**
 * Get tab name from href attribute
 */
function getTabNameFromHref(href) {
    if (!href) return null;
    
    // Extract tab name from URL parameter like /profile/username?tab=posts
    const url = new URL(href, window.location.origin);
    const tabParam = url.searchParams.get('tab');
    
    if (tabParam) {
        return tabParam;
    }
    
    // Fallback: extract from URL path like /profile/username/posts
    const parts = href.split('/');
    const lastPart = parts[parts.length - 1];
    
    // Map URL parts to tab names
    const tabMap = {
        'posts': 'posts',
        'friends': 'friends', 
        'groups': 'groups',
        'documents': 'documents'
    };
    
    return tabMap[lastPart] || null;
}

/**
 * Switch to specific tab
 */
function switchToTab(tabName) {
    // Remove active class from all tabs
    document.querySelectorAll('.profile-nav .nav-link').forEach(tab => {
        tab.classList.remove('active');
    });
    
    // Add active class to target tab
    const targetTab = document.querySelector(`.profile-nav .nav-link[href*="tab=${tabName}"]`);
    if (targetTab) {
        targetTab.classList.add('active');
    }
    
    // Hide all tab content
    document.querySelectorAll('.tab-content').forEach(content => {
        content.classList.remove('active');
    });
    
    // Show target tab content
    const targetContent = document.getElementById(tabName);
    if (targetContent) {
        targetContent.classList.add('active');
    }
    
    // Update URL with tab parameter
    const url = new URL(window.location);
    url.searchParams.set('tab', tabName);
    
    if (window.location.href !== url.href) {
        window.history.pushState({}, '', url);
    }
}
