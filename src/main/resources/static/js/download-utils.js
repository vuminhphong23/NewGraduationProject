/**
 * Global Download Utilities
 * Handles file download and download count update for all components
 */

// Global download function
async function downloadFile(url, fileId) {
    try {
        
        // Update download count if fileId is provided
        if (fileId) {
            await updateDownloadCount(fileId);
        }
        
        // Extract filename from URL
        let filename = 'download';
        try {
            const urlPath = new URL(url).pathname;
            const urlFilename = urlPath.split('/').pop();
            if (urlFilename && urlFilename.includes('.')) {
                filename = urlFilename;
            }
        } catch (e) {
            // Ignore parsing errors
        }
        
        
        // For Cloudinary files, use fetch to download
        if (url && url.includes('cloudinary.com')) {
            try {
                const response = await fetch(url);
                const blob = await response.blob();
                
                const downloadUrl = window.URL.createObjectURL(blob);
                const link = document.createElement('a');
                link.href = downloadUrl;
                link.download = filename;
                document.body.appendChild(link);
                link.click();
                document.body.removeChild(link);
                window.URL.revokeObjectURL(downloadUrl);
            } catch (error) {
                console.error('Error downloading Cloudinary file:', error);
                // Fallback to opening in new tab
                window.open(url, '_blank');
            }
        } else {
            // Regular download
            const link = document.createElement('a');
            link.href = url;
            link.download = filename;
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
        }
        
        
    } catch (error) {
        console.error('Error downloading file:', error);
    }
}

// Global update download count function
async function updateDownloadCount(fileId) {
    try {
        const response = await fetch(`/api/files/${fileId}/download`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            }
        });
        
        if (!response.ok) {
            const errorText = await response.text();
            console.error('Failed to update download count:', response.status, errorText);
        }
    } catch (error) {
        console.error('Error updating download count:', error);
    }
}

// Make functions globally available
window.downloadFile = downloadFile;
window.updateDownloadCount = updateDownloadCount;

