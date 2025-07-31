// Post Modal JavaScript
class PostModal {
    constructor() {
        this.currentPrivacy = 'PUBLIC';
        this.initializeElements();
        this.bindEvents();
    }

    initializeElements() {
        this.modal = document.getElementById('postModal');
        this.form = document.getElementById('postForm');
        this.titleInput = document.getElementById('postTitle');
        this.contentInput = document.getElementById('postContent');
        this.topicSelect = document.getElementById('postTopic');
        this.privacyDropdown = document.getElementById('privacyDropdown');
        this.privacyText = document.getElementById('privacyText');
        this.publishBtn = document.getElementById('publishBtn');
        this.publishSpinner = document.getElementById('publishSpinner');
        this.publishText = document.getElementById('publishText');
        this.titleCount = document.getElementById('titleCount');
        this.contentCount = document.getElementById('contentCount');
        this.previewArea = document.getElementById('postPreview');
        this.previewContent = document.getElementById('previewContent');

        // Toast elements
        this.successToast = new bootstrap.Toast(document.getElementById('successToast'));
        this.errorToast = new bootstrap.Toast(document.getElementById('errorToast'));
        this.errorMessage = document.getElementById('errorMessage');
    }

    bindEvents() {
        // Privacy dropdown
        document.querySelectorAll('[data-privacy]').forEach(item => {
            item.addEventListener('click', (e) => {
                e.preventDefault();
                const privacy = e.currentTarget.dataset.privacy;
                this.setPrivacy(privacy);
            });
        });

        // Character counters
        this.titleInput.addEventListener('input', () => {
            this.updateCharacterCount(this.titleInput, this.titleCount, 100);
            this.validateForm();
        });

        this.contentInput.addEventListener('input', () => {
            this.updateCharacterCount(this.contentInput, this.contentCount, 2000);
            this.validateForm();
            this.updatePreview();
        });

        // Topic selection
        this.topicSelect.addEventListener('change', () => {
            this.validateForm();
        });

        // Publish button
        this.publishBtn.addEventListener('click', () => {
            this.publishPost();
        });


        // Modal events
        this.modal.addEventListener('hidden.bs.modal', () => {
            this.resetForm();
        });

        // Preview toggle
        this.contentInput.addEventListener('focus', () => {
            if (this.contentInput.value.trim()) {
                this.showPreview();
            }
        });
    }

    setPrivacy(privacy) {
        this.currentPrivacy = privacy;
        const privacyMap = {
            'PUBLIC': { text: 'Công khai', icon: 'fa-globe-asia', color: 'text-primary' },
            'FRIENDS': { text: 'Bạn bè', icon: 'fa-users', color: 'text-success' },
            'PRIVATE': { text: 'Riêng tư', icon: 'fa-lock', color: 'text-warning' }
        };

        const config = privacyMap[privacy];
        this.privacyText.textContent = config.text;

        // Update dropdown button
        const icon = this.privacyDropdown.querySelector('i');
        icon.className = `fa ${config.icon} ${config.color}`;
    }

    updateCharacterCount(input, counter, maxLength) {
        const count = input.value.length;
        counter.textContent = count;

        if (count > maxLength * 0.9) {
            counter.classList.add('text-warning');
        } else {
            counter.classList.remove('text-warning');
        }

        if (count > maxLength) {
            counter.classList.add('text-danger');
        } else {
            counter.classList.remove('text-danger');
        }
    }

    validateForm() {
        const title = this.titleInput.value.trim();
        const content = this.contentInput.value.trim();
        const topic = this.topicSelect.value;

        const isValid = title.length > 0 && content.length > 0 && topic !== '';
        this.publishBtn.disabled = !isValid;
    }

    updatePreview() {
        const content = this.contentInput.value.trim();
        if (content) {
            this.previewContent.innerHTML = this.formatContent(content);
            this.showPreview();
        } else {
            this.hidePreview();
        }
    }

    formatContent(content) {
        // Convert line breaks to <br> tags
        return content.replace(/\n/g, '<br>');
    }

    showPreview() {
        this.previewArea.classList.remove('d-none');
    }

    hidePreview() {
        this.previewArea.classList.add('d-none');
    }


    async publishPost() {
        if (this.publishBtn.disabled) return;

        const postData = {
            title: this.titleInput.value.trim(),
            content: this.contentInput.value.trim(),
            topicId: parseInt(this.topicSelect.value),
            privacy: this.currentPrivacy
        };

        console.log('Publishing post:', postData);
        console.log('Current user authenticated:', document.body.dataset.authenticated);
        this.setLoading(true);

        try {
            console.log('Making request to /api/posts...');
            const response = await fetch('/api/posts', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Accept': 'application/json'
                },
                body: JSON.stringify(postData)
            });

            console.log('Response status:', response.status);
            console.log('Response headers:', [...response.headers.entries()]);
            console.log('Response ok:', response.ok);

            if (response.ok) {
                const result = await response.json();
                console.log('Success:', result);
                this.showSuccess();
                this.closeModal();
                // Optionally refresh the feed or add the new post to the page
                this.refreshFeed();
            } else {
                const responseText = await response.text();
                console.log('Error response text:', responseText);
                
                let errorData;
                try {
                    errorData = JSON.parse(responseText);
                } catch (e) {
                    errorData = { message: responseText };
                }
                
                console.log('Error response data:', errorData);
                this.showError(errorData.message || errorData.error || 'Có lỗi xảy ra khi đăng bài viết');
            }
        } catch (error) {
            console.error('Error publishing post:', error);
            this.showError('Không thể kết nối đến máy chủ');
        } finally {
            this.setLoading(false);
        }
    }

    setLoading(loading) {
        this.publishBtn.disabled = loading;
        this.publishSpinner.classList.toggle('d-none', !loading);
        this.publishText.textContent = loading ? 'Đang đăng...' : 'Đăng';
    }

    showSuccess() {
        this.successToast.show();
    }

    showError(message) {
        this.errorMessage.textContent = message;
        this.errorToast.show();
    }

    closeModal() {
        const modal = bootstrap.Modal.getInstance(this.modal);
        modal.hide();
    }

    resetForm() {
        this.form.reset();
        this.currentPrivacy = 'PUBLIC';
        this.setPrivacy('PUBLIC');
        this.publishBtn.disabled = true;
        this.titleCount.textContent = '0';
        this.contentCount.textContent = '0';
        this.hidePreview();
        this.titleCount.classList.remove('text-warning', 'text-danger');
        this.contentCount.classList.remove('text-warning', 'text-danger');
    }



    refreshFeed() {
        // TODO: Implement feed refresh logic
        // This could reload the page or make an AJAX call to get new posts
        console.log('Feed should be refreshed');
    }
}

// Initialize when DOM is loaded
document.addEventListener('DOMContentLoaded', function() {
    window.postModal = new PostModal();
});

// Export for use in other scripts
if (typeof module !== 'undefined' && module.exports) {
    module.exports = PostModal;
}
