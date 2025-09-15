// Gallery Grid Optimizer
// Automatically optimizes gallery layout based on number of items

document.addEventListener('DOMContentLoaded', function() {
    optimizeAllGalleries();
});

function optimizeAllGalleries() {
    const galleries = document.querySelectorAll('.gallery-grid');
    galleries.forEach(optimizeGallery);
}

function optimizeGallery(gallery) {
    const items = gallery.querySelectorAll('.gallery-item');
    const itemCount = items.length;
    
    // Remove existing count classes
    gallery.classList.remove('single-item', 'two-items', 'three-items', 'four-items', 
                            'five-items', 'six-items', 'seven-items', 'eight-items');
    
    // Apply appropriate class based on item count
    switch(itemCount) {
        case 1:
            gallery.classList.add('single-item');
            break;
        case 2:
            gallery.classList.add('two-items');
            break;
        case 3:
            gallery.classList.add('three-items');
            break;
        case 4:
            gallery.classList.add('four-items');
            break;
        case 5:
            gallery.classList.add('five-items');
            break;
        case 6:
            gallery.classList.add('six-items');
            break;
        case 7:
            gallery.classList.add('seven-items');
            break;
        case 8:
            gallery.classList.add('eight-items');
            break;
        default:
            // For more than 8 items, use default auto-fit layout
            break;
    }
    
    // Special handling for single image/video to make it larger
    if (itemCount === 1) {
        const singleItem = items[0];
        const isImage = singleItem.querySelector('.gallery-image');
        const isVideo = singleItem.querySelector('.gallery-video');
        
        if (isImage || isVideo) {
            // Make single media item larger
            singleItem.style.maxWidth = '400px';
            singleItem.style.width = '100%';
        }
    }
}

// Re-optimize when new content is loaded
function reoptimizeGalleries() {
    setTimeout(optimizeAllGalleries, 100);
}

// Export for use in other scripts
window.optimizeAllGalleries = optimizeAllGalleries;
window.reoptimizeGalleries = reoptimizeGalleries;
