/**
 * Source Links Processor
 * Xử lý hiển thị nguồn đẹp cho post được crawl
 */

// Function to process content and format source links
function processContentWithLinks(content) {
    if (!content) return '';
    
    // Skip if already processed (contains source-section HTML)
    if (content.includes('source-section')) {
        return content;
    }
    
    // Check if content has source section (--- separator)
    const sourceSeparator = '\n---\n';
    const hasSource = content.includes(sourceSeparator);
    
    if (hasSource) {
        // Split content and source
        const parts = content.split(sourceSeparator);
        const mainContent = parts[0].trim();
        const sourceSection = parts[1] ? parts[1].trim() : '';
        
        // Extract URL from source section
        const urlRegex = /(https?:\/\/[^\s]+)/g;
        const urls = sourceSection.match(urlRegex) || [];
        
        let processedContent = mainContent;
        
        // Add beautiful source link if URL exists
        if (urls.length > 0) {
            const url = urls[0]; // Take first URL
            try {
                const urlObj = new URL(url);
                const domain = urlObj.hostname.replace('www.', '');
                processedContent += `
                    <div class="mt-2">
                        <small class="text-muted">
                            <i class="fa fa-external-link-alt me-1"></i>
                            <a href="${url}" target="_blank" rel="noopener noreferrer" class="text-decoration-none">
                                ${domain}
                            </a>
                        </small>
                    </div>`;
            } catch (e) {
                processedContent += `
                    <div class="mt-2">
                        <small class="text-muted">
                            <i class="fa fa-external-link-alt me-1"></i>
                            <a href="${url}" target="_blank" rel="noopener noreferrer" class="text-decoration-none">
                                Nguồn
                            </a>
                        </small>
                    </div>`;
            }
        }
        
        return processedContent;
    }
    
    // Fallback for old format or content without source
    const urlRegex = /(https?:\/\/[^\s]+)/g;
    const urls = content.match(urlRegex) || [];
    
    if (urls.length > 0) {
        // Remove URLs from content
        let processedContent = content.replace(urlRegex, '').trim();
        processedContent = processedContent.replace(/\*\*Nguồn:\*\*/g, '').trim();
        
        // Remove extra whitespace and newlines
        processedContent = processedContent.replace(/\n\s*\n/g, '\n').trim();
        
        // Add beautiful source link (only first URL)
        const url = urls[0];
        try {
            const urlObj = new URL(url);
            const domain = urlObj.hostname.replace('www.', '');
            processedContent += `
                <div class="mt-2">
                    <small class="text-muted">
                        <i class="fa fa-external-link-alt me-1"></i>
                        <a href="${url}" target="_blank" rel="noopener noreferrer" class="text-decoration-none">
                            ${domain}
                        </a>
                    </small>
                </div>`;
        } catch (e) {
            processedContent += `
                <div class="mt-2">
                    <small class="text-muted">
                        <i class="fa fa-external-link-alt me-1"></i>
                        <a href="${url}" target="_blank" rel="noopener noreferrer" class="text-decoration-none">
                            Nguồn
                        </a>
                    </small>
                </div>`;
        }
        
        return processedContent;
    }
    
    return content;
}

// Function to process post content
function processPostContent() {
    const postContents = document.querySelectorAll('.card-text, .original-post-content .mb-2');
    postContents.forEach(function(element) {
        // Skip if already processed (has external link icon)
        if (element.querySelector('.fa-external-link-alt')) {
            return;
        }
        
        // Skip if content already has processed links
        if (element.innerHTML.includes('fa-external-link-alt')) {
            return;
        }
        
        if (element.innerHTML && typeof processContentWithLinks === 'function') {
            const processed = processContentWithLinks(element.innerHTML);
            if (processed !== element.innerHTML) {
                element.innerHTML = processed;
            }
        }
    });
}

// Initialize source links processing
function initSourceLinksProcessor() {
    // Process on DOM ready only
    document.addEventListener('DOMContentLoaded', processPostContent);
    
    // Process when content is dynamically loaded (with debounce)
    let timeoutId;
    const observer = new MutationObserver(function(mutations) {
        mutations.forEach(function(mutation) {
            if (mutation.type === 'childList' && mutation.addedNodes.length > 0) {
                // Check if new content has URLs that need processing
                const hasNewContent = Array.from(mutation.addedNodes).some(node => {
                    if (node.nodeType === Node.ELEMENT_NODE) {
                        const textContent = node.textContent || '';
                        return textContent.includes('http') && 
                               !textContent.includes('fa-external-link-alt') &&
                               !node.querySelector('.fa-external-link-alt');
                    }
                    return false;
                });
                
                if (hasNewContent) {
                    clearTimeout(timeoutId);
                    timeoutId = setTimeout(processPostContent, 500); // Increased delay
                }
            }
        });
    });
    
    // Start observing
    observer.observe(document.body, {
        childList: true,
        subtree: true
    });
}

// Auto-initialize when script loads
initSourceLinksProcessor();
