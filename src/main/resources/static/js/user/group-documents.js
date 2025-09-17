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
        const searchTerm = groupDocumentSearch ? groupDocumentSearch.value.toLowerCase().trim() : '';
        const typeFilter = groupDocumentTypeFilter ? groupDocumentTypeFilter.value : '';
        
        // Get fresh list of document items
        const items = Array.from(groupDocumentsList.querySelectorAll('.group-document-item'));
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
        updateGroupDocumentCount(visibleCount);
    }

    function sortGroupDocuments() {
        const sortBy = groupDocumentSortFilter ? groupDocumentSortFilter.value : 'name';
        const container = groupDocumentsList;
        const items = Array.from(container.querySelectorAll('.group-document-item'));
        
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
                    const dateA = parseGroupDate(a.getAttribute('data-date') || '');
                    const dateB = parseGroupDate(b.getAttribute('data-date') || '');
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

    function applyGroupQuickFilter(filter) {
        // Remove active class from all quick filters
        groupQuickDocFilters.forEach(btn => btn.classList.remove('active'));
        
        // Add active class to clicked button
        const activeButton = document.querySelector(`[data-filter="${filter}"]`);
        if (activeButton) {
            activeButton.classList.add('active');
        }
        
        // Clear search first
        if (groupDocumentSearch) {
            groupDocumentSearch.value = '';
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
                setGroupTypeFilter(''); // Clear type filter for recent
                break;
            default:
                clearAllGroupDocFilters();
        }
        
        // Apply filters
        filterGroupDocuments();
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
            filterGroupDocuments();
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
        
        // Show all documents with animation
        const items = Array.from(groupDocumentsList.querySelectorAll('.group-document-item'));
        items.forEach((item, index) => {
            item.style.display = 'flex';
            item.style.opacity = '1';
            item.style.transition = 'all 0.3s ease';
            item.style.order = index;
            item.classList.remove('filtered-out');
            item.classList.add('filtered-in');
        });
        
        // Reset sort and filter
        sortGroupDocuments();
        filterGroupDocuments();
        
        // Update count
        updateGroupDocumentCount(items.length);
    }

    function updateGroupDocumentCount(count = null) {
        let visibleCount = count;
        
        if (count === null) {
            const visibleItems = groupDocumentsList.querySelectorAll('.group-document-item[style*="flex"]');
            visibleCount = visibleItems.length;
        }
        
        const countElement = document.querySelector('.group-document-count .badge span');
        
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
    function trackGroupDownload(downloadUrl, fileName) {
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
