// Profile Documents Filter and Management
document.addEventListener('DOMContentLoaded', function() {
    const documentSearch = document.getElementById('documentSearch');
    const documentTypeFilter = document.getElementById('documentTypeFilter');
    const documentSortFilter = document.getElementById('documentSortFilter');
    const clearDocFilters = document.getElementById('clearDocFilters');
    const quickDocFilters = document.querySelectorAll('.quick-doc-filter');
    const documentsList = document.getElementById('documentsList');
    // documentItems will be queried fresh each time

    // Initialize filters
    initDocumentFilters();

    function initDocumentFilters() {
        // Search functionality
        if (documentSearch) {
            documentSearch.addEventListener('input', function() {
                filterDocuments();
            });
        }

        // Type filter
        if (documentTypeFilter) {
            documentTypeFilter.addEventListener('change', function() {
                filterDocuments();
            });
        }

        // Sort filter
        if (documentSortFilter) {
            documentSortFilter.addEventListener('change', function() {
                sortDocuments();
            });
        }

        // Clear filters
        if (clearDocFilters) {
            clearDocFilters.addEventListener('click', function() {
                clearAllFilters();
            });
        }

        // Quick filter buttons
        quickDocFilters.forEach(button => {
            button.addEventListener('click', function() {
                const filter = this.getAttribute('data-filter');
                applyQuickFilter(filter);
            });
        });
    }
    
    function filterDocuments() {
        const searchTerm = documentSearch ? documentSearch.value.toLowerCase() : '';
        const typeFilter = documentTypeFilter ? documentTypeFilter.value : '';
        
        // Get fresh list of document items
        const items = Array.from(documentsList.querySelectorAll('.document-item'));
        
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
        updateDocumentCount();
    }

    function sortDocuments() {
        const sortBy = documentSortFilter ? documentSortFilter.value : 'name';
        const container = documentsList;
        const items = Array.from(container.querySelectorAll('.document-item'));
        
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

    function applyQuickFilter(filter) {
        // Remove active class from all quick filters
        quickDocFilters.forEach(btn => btn.classList.remove('active'));
        
        // Add active class to clicked button
        const activeButton = document.querySelector(`[data-filter="${filter}"]`);
        if (activeButton) {
            activeButton.classList.add('active');
        }
        
        // Apply filter based on type
        switch (filter) {
            case 'pdf':
                setTypeFilter('pdf');
                break;
            case 'doc':
                setTypeFilter('doc');
                break;
            case 'xls':
                setTypeFilter('xls');
                break;
            case 'image':
                setTypeFilter('image');
                break;
            case 'video':
                setTypeFilter('video');
                break;
            case 'text':
                setTypeFilter('text');
                break;
            case 'document':
                setTypeFilter('document');
                break;
            case 'recent':
                setSortFilter('date');
                break;
            default:
                clearAllFilters();
        }
    }

    function setTypeFilter(type) {
        if (documentTypeFilter) {
            documentTypeFilter.value = type;
            filterDocuments();
        }
    }

    function setSortFilter(sort) {
        if (documentSortFilter) {
            documentSortFilter.value = sort;
            sortDocuments();
        }
    }

    function clearAllFilters() {
        // Clear search
        if (documentSearch) {
            documentSearch.value = '';
        }
        
        // Clear type filter
        if (documentTypeFilter) {
            documentTypeFilter.value = '';
        }
        
        // Clear sort filter
        if (documentSortFilter) {
            documentSortFilter.value = 'name';
        }
        
        // Remove active class from quick filters
        quickDocFilters.forEach(btn => btn.classList.remove('active'));
        
        // Show all documents
        const items = Array.from(documentsList.querySelectorAll('.document-item'));
        items.forEach(item => {
            item.classList.remove('filtered-out');
            item.classList.add('filtered-in');
            item.style.display = 'flex'; // Ensure visible
        });
        
        // Reset sort
        sortDocuments();
        
        // Update count
        updateDocumentCount();
    }

    function updateDocumentCount() {
        const visibleItems = documentsList.querySelectorAll('.document-item[style*="flex"], .document-item:not([style*="none"])');
        const countElement = document.querySelector('.document-count span');
        
        if (countElement) {
            countElement.textContent = visibleItems.length;
        }
    }

    // Download tracking
    function trackDownload(downloadUrl, fileName) {
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
        if (e.target.closest('.document-actions a[download]')) {
            const link = e.target.closest('.document-actions a[download]');
            const downloadUrl = link.getAttribute('href');
            const fileName = link.closest('.document-item').getAttribute('data-name');
            
            // Track download
            trackDownload(downloadUrl, fileName);
        }
    });

    // Initialize document count
    updateDocumentCount();
});

// Helper function to parse different date formats
function parseDate(dateString) {
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
window.ProfileDocuments = {
    filterByType: function(type) {
        const typeFilter = document.getElementById('documentTypeFilter');
        if (typeFilter) {
            typeFilter.value = type;
            document.dispatchEvent(new Event('change'));
        }
    },
    
    searchDocuments: function(term) {
        const searchInput = document.getElementById('documentSearch');
        if (searchInput) {
            searchInput.value = term;
            document.dispatchEvent(new Event('input'));
        }
    },
    
    clearAllFilters: function() {
        const clearBtn = document.getElementById('clearDocFilters');
        if (clearBtn) {
            clearBtn.click();
        }
    }
};