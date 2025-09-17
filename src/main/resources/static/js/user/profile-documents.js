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
        const searchTerm = documentSearch ? documentSearch.value.toLowerCase().trim() : '';
        const typeFilter = documentTypeFilter ? documentTypeFilter.value : '';
        
        // Get fresh list of document items
        const items = Array.from(documentsList.querySelectorAll('.document-item'));
        let visibleCount = 0;
        
        // Enhanced type mappings for flexible matching
        const typeMappings = {
            'doc': ['doc', 'docx', '.doc', '.docx', 'word', 'msword'],
            'xls': ['xls', 'xlsx', '.xls', '.xlsx', 'excel'],
            'ppt': ['ppt', 'pptx', '.ppt', '.pptx', 'powerpoint'],
            'image': ['image', 'jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp', '.jpg', '.jpeg', '.png', '.gif', '.bmp', '.webp'],
            'video': ['video', 'mp4', 'avi', 'mov', 'wmv', '.mp4', '.avi', '.mov', '.wmv'],
            'text': ['text', 'txt', '.txt'],
            'pdf': ['pdf', '.pdf'],
            'document': ['document'],
            'other': ['other', 'unknown']
        };
        
        items.forEach(item => {
            const name = item.getAttribute('data-name') || '';
            const type = item.getAttribute('data-type') || '';
            const fileName = name.toLowerCase();
            const fileType = type.toLowerCase();
            
            let showItem = true;
            
            // Search filter - check both name and type
            if (searchTerm) {
                const matchesName = fileName.includes(searchTerm);
                const matchesType = fileType.includes(searchTerm);
                if (!matchesName && !matchesType) {
                    showItem = false;
                }
            }
            
            // Type filter - flexible matching
            if (typeFilter && showItem) {
                const validTypes = typeMappings[typeFilter] || [typeFilter];
                const matchesType = validTypes.some(validType => 
                    fileType === validType || 
                    fileType.includes(validType) || 
                    validType.includes(fileType)
                );
                if (!matchesType) {
                    showItem = false;
                }
            }
            
            // Apply filter with smooth transition
            if (showItem) {
                item.style.display = 'flex';
                item.style.opacity = '1';
                item.classList.remove('filtered-out');
                item.classList.add('filtered-in');
                visibleCount++;
            } else {
                item.style.opacity = '0';
                setTimeout(() => {
                    if (item.style.opacity === '0') {
                        item.style.display = 'none';
                    }
                }, 200);
                item.classList.remove('filtered-in');
                item.classList.add('filtered-out');
            }
        });
        
        // Update count with animation
        updateDocumentCount(visibleCount);
    }

    function sortDocuments() {
        const sortBy = documentSortFilter ? documentSortFilter.value : 'name';
        const container = documentsList;
        const items = Array.from(container.querySelectorAll('.document-item'));
        
        // Only sort visible items
        const visibleItems = items.filter(item => 
            item.style.display !== 'none' && 
            !item.classList.contains('filtered-out')
        );
        
        visibleItems.sort((a, b) => {
            switch (sortBy) {
                case 'name':
                    const nameA = (a.getAttribute('data-name') || '').toLowerCase();
                    const nameB = (b.getAttribute('data-name') || '').toLowerCase();
                    return nameA.localeCompare(nameB, 'vi', { numeric: true });
                    
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
        
        // Re-append sorted items with animation
        visibleItems.forEach((item, index) => {
            item.style.transition = 'all 0.3s ease';
            item.style.order = index;
            container.appendChild(item);
        });
    }

    function applyQuickFilter(filter) {
        // Remove active class from all quick filters
        quickDocFilters.forEach(btn => btn.classList.remove('active'));
        
        // Add active class to clicked button
        const activeButton = document.querySelector(`[data-filter="${filter}"]`);
        if (activeButton) {
            activeButton.classList.add('active');
        }
        
        // Clear search first
        if (documentSearch) {
            documentSearch.value = '';
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
                setTypeFilter(''); // Clear type filter for recent
                break;
            default:
                clearAllFilters();
        }
        
        // Apply filters
        filterDocuments();
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
            filterDocuments();
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
        
        // Show all documents with animation
        const items = Array.from(documentsList.querySelectorAll('.document-item'));
        items.forEach((item, index) => {
            item.style.display = 'flex';
            item.style.opacity = '1';
            item.style.transition = 'all 0.3s ease';
            item.style.order = index;
            item.classList.remove('filtered-out');
            item.classList.add('filtered-in');
        });
        
        // Reset sort and filter
        sortDocuments();
        filterDocuments();
        
        // Update count
        updateDocumentCount(items.length);
    }

    function updateDocumentCount(count = null) {
        let visibleCount = count;
        
        if (count === null) {
            const visibleItems = documentsList.querySelectorAll('.document-item[style*="flex"]');
            visibleCount = visibleItems.length;
        }
        
        const countElement = document.querySelector('.document-count .badge span');
        
        if (countElement) {
            // Animate count change
            const currentCount = parseInt(countElement.textContent) || 0;
            if (currentCount !== visibleCount) {
                countElement.style.transition = 'all 0.3s ease';
                countElement.textContent = visibleCount;
                
                // Add pulse animation
                countElement.style.transform = 'scale(1.1)';
                setTimeout(() => {
                    countElement.style.transform = 'scale(1)';
                }, 150);
            }
        }
    }

    // Download tracking
    function trackDownload(downloadUrl, fileName) {
        // Send download tracking request
        authenticatedFetch('/api/documents/track-download', {
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