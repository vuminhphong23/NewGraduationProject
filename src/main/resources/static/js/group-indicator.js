// Group popup functionality
function initializeGroupPopups() {
    // Handle group badge clicks to navigate to group
    document.addEventListener('click', function(e) {
        if (e.target.closest('.group-badge')) {
            e.preventDefault();
            const groupId = e.target.closest('.group-badge').getAttribute('data-group-id');
            if (groupId) {
                window.location.href = '/groups/' + groupId;
            }
        }
    });
}

// Initialize when DOM is loaded
document.addEventListener('DOMContentLoaded', function() {
    initializeGroupPopups();
});
