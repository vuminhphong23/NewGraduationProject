// Group Documents Filter and Management
document.addEventListener('DOMContentLoaded', function() {
    const groupDocumentSearch = document.getElementById('groupDocumentSearch');
    const groupDocumentTypeFilter = document.getElementById('groupDocumentTypeFilter');
    const groupDocumentSortFilter = document.getElementById('groupDocumentSortFilter');
    const clearGroupDocFilters = document.getElementById('clearGroupDocFilters');
    const groupQuickDocFilters = document.querySelectorAll('.group-quick-doc-filter');
    const groupDocumentsList = document.getElementById('groupDocumentsList');
    // groupDocumentItems will be queried fresh each time

    // Initialize filters
    initGroupDocumentFilters();

    function initGroupDocumentFilters() {
        // Search functionality
        if (groupDocumentSearch) {
            groupDocumentSearch.addEventListener('input', function() {
                filterGroupDocuments();
            });
        }

        // Type filter
        if (groupDocumentTypeFilter) {
            groupDocumentTypeFilter.addEventListener('change', function() {
                filterGroupDocuments();
            });
        }

        // Sort filter
        if (groupDocumentSortFilter) {
            groupDocumentSortFilter.addEventListener('change', function() {
                sortGroupDocuments();
            });
        }

        // Clear filters
        if (clearGroupDocFilters) {
            clearGroupDocFilters.addEventListener('click', function() {
                clearAllGroupDocFilters();
            });
        }

        // Quick filter buttons
        groupQuickDocFilters.forEach(button => {
            button.addEventListener('click', function() {
                const filter = this.getAttribute('data-filter');
                applyGroupQuickFilter(filter);
            });
        });
    }
    
    function filterGroupDocuments() {
        const searchTerm = groupDocumentSearch ? groupDocumentSearch.value.toLowerCase() : '';
        const typeFilter = groupDocumentTypeFilter ? groupDocumentTypeFilter.value : '';
        
        // Get fresh list of document items
        const items = Array.from(groupDocumentsList.querySelectorAll('.group-document-item'));
        
        items.forEach(item => {
            const name = item.getAttribute('data-name') || '';
            const type = item.getAttribute('data-type') || '';
            
            let showItem = true;
            
            // Search filter
            if (searchTerm && !name.toLowerCase().includes(searchTerm)) {
                showItem = false;
            }
            
            // Type filter
            if (typeFilter && type !== typeFilter) {
                showItem = false;
            }
            
            // Apply filter
            if (showItem) {
                item.classList.remove('filtered-out');
                item.classList.add('filtered-in');
                item.style.display = 'flex'; // Ensure visible
            } else {
                item.classList.remove('filtered-in');
                item.classList.add('filtered-out');
                item.style.display = 'none'; // Hide completely
            }
        });
        
        // Update document count
        updateGroupDocumentCount();
    }

    function sortGroupDocuments() {
        const sortBy = groupDocumentSortFilter ? groupDocumentSortFilter.value : 'name';
        const container = groupDocumentsList;
        const items = Array.from(container.querySelectorAll('.group-document-item'));
        
        items.sort((a, b) => {
            switch (sortBy) {
                case 'name':
                    const nameA = a.getAttribute('data-name') || '';
                    const nameB = b.getAttribute('data-name') || '';
                    return nameA.localeCompare(nameB);
                    
                case 'date':
                    const dateA = parseDate(a.getAttribute('data-date') || '');
                    const dateB = parseDate(b.getAttribute('data-date') || '');
                    return dateB - dateA; // Newest first
                    
                case 'size':
                    const sizeA = parseInt(a.getAttribute('data-size') || '0');
                    const sizeB = parseInt(b.getAttribute('data-size') || '0');
                    return sizeB - sizeA; // Largest first
                    
                case 'downloads':
                    const downloadsA = parseInt(a.getAttribute('data-downloads') || '0');
                    const downloadsB = parseInt(b.getAttribute('data-downloads') || '0');
                    return downloadsB - downloadsA; // Most downloaded first
                    
                default:
                    return 0;
            }
        });
        
        // Re-append sorted items
        items.forEach(item => container.appendChild(item));
    }

    function applyGroupQuickFilter(filter) {
        // Remove active class from all quick filters
        groupQuickDocFilters.forEach(btn => btn.classList.remove('active'));
        
        // Add active class to clicked button
        const activeButton = document.querySelector(`[data-filter="${filter}"]`);
        if (activeButton) {
            activeButton.classList.add('active');
        }
        
        // Apply filter based on type
        switch (filter) {
            case 'pdf':
                setGroupTypeFilter('pdf');
                break;
            case 'doc':
                setGroupTypeFilter('doc');
                break;
            case 'xls':
                setGroupTypeFilter('xls');
                break;
            case 'image':
                setGroupTypeFilter('image');
                break;
            case 'video':
                setGroupTypeFilter('video');
                break;
            case 'text':
                setGroupTypeFilter('text');
                break;
            case 'document':
                setGroupTypeFilter('document');
                break;
            case 'recent':
                setGroupSortFilter('date');
                break;
            default:
                clearAllGroupDocFilters();
        }
    }

    function setGroupTypeFilter(type) {
        if (groupDocumentTypeFilter) {
            groupDocumentTypeFilter.value = type;
            filterGroupDocuments();
        }
    }

    function setGroupSortFilter(sort) {
        if (groupDocumentSortFilter) {
            groupDocumentSortFilter.value = sort;
            sortGroupDocuments();
        }
    }

    function clearAllGroupDocFilters() {
        // Clear search
        if (groupDocumentSearch) {
            groupDocumentSearch.value = '';
        }
        
        // Clear type filter
        if (groupDocumentTypeFilter) {
            groupDocumentTypeFilter.value = '';
        }
        
        // Clear sort filter
        if (groupDocumentSortFilter) {
            groupDocumentSortFilter.value = 'name';
        }
        
        // Remove active class from quick filters
        groupQuickDocFilters.forEach(btn => btn.classList.remove('active'));
        
        // Show all documents
        const items = Array.from(groupDocumentsList.querySelectorAll('.group-document-item'));
        items.forEach(item => {
            item.classList.remove('filtered-out');
            item.classList.add('filtered-in');
            item.style.display = 'flex'; // Ensure visible
        });
        
        // Reset sort
        sortGroupDocuments();
        
        // Update count
        updateGroupDocumentCount();
    }

    function updateGroupDocumentCount() {
        const visibleItems = groupDocumentsList.querySelectorAll('.group-document-item[style*="flex"], .group-document-item:not([style*="none"])');
        const countElement = document.querySelector('.group-document-count span');
        
        if (countElement) {
            countElement.textContent = visibleItems.length;
        }
    }

    // Download tracking
    function trackGroupDownload(downloadUrl, fileName) {
        // Send download tracking request
        fetch('/api/documents/track-download', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                downloadUrl: downloadUrl,
                fileName: fileName
            })
        }).catch(error => {
            console.log('Download tracking failed:', error);
        });
    }

    // Add download tracking to all download links
    document.addEventListener('click', function(e) {
        if (e.target.closest('.group-document-actions a[download]')) {
            const link = e.target.closest('.group-document-actions a[download]');
            const downloadUrl = link.getAttribute('href');
            const fileName = link.closest('.group-document-item').getAttribute('data-name');
            
            // Track download
            trackGroupDownload(downloadUrl, fileName);
        }
    });

    // Initialize document count
    updateGroupDocumentCount();
});

// Helper function to parse different date formats
function parseGroupDate(dateString) {
    if (!dateString || dateString === '01/01/2024') {
        return new Date(0); // Return epoch for invalid dates
    }
    
    // Try DD/MM/YYYY format first (most common in our app)
    const ddMMyyyyMatch = /^(\d{1,2})\/(\d{1,2})\/(\d{4})$/.exec(dateString);
    if (ddMMyyyyMatch) {
        const [, day, month, year] = ddMMyyyyMatch;
        return new Date(year, month - 1, day);
    }
    
    // Try YYYY-MM-DD format
    const yyyyMMddMatch = /^(\d{4})-(\d{1,2})-(\d{1,2})$/.exec(dateString);
    if (yyyyMMddMatch) {
        const [, year, month, day] = yyyyMMddMatch;
        return new Date(year, month - 1, day);
    }
    
    // Try MM/DD/YYYY format
    const mmDDyyyyMatch = /^(\d{1,2})\/(\d{1,2})\/(\d{4})$/.exec(dateString);
    if (mmDDyyyyMatch) {
        const [, month, day, year] = mmDDyyyyMatch;
        return new Date(year, month - 1, day);
    }
    
    // Fallback to native Date parsing
    const parsed = new Date(dateString);
    return isNaN(parsed) ? new Date(0) : parsed;
}

// Utility functions for external use
window.GroupDocuments = {
    filterByType: function(type) {
        const typeFilter = document.getElementById('groupDocumentTypeFilter');
        if (typeFilter) {
            typeFilter.value = type;
            document.dispatchEvent(new Event('change'));
        }
    },
    
    searchDocuments: function(term) {
        const searchInput = document.getElementById('groupDocumentSearch');
        if (searchInput) {
            searchInput.value = term;
            document.dispatchEvent(new Event('input'));
        }
    },
    
    clearAllFilters: function() {
        const clearBtn = document.getElementById('clearGroupDocFilters');
        if (clearBtn) {
            clearBtn.click();
        }
    }
};
