/**
 * Admin Toast Handler - Xử lý toast notification cho admin pages
 * Tự động hiển thị toast từ server flash messages
 */

(function() {
    'use strict';

    // Đợi DOM load xong
    document.addEventListener('DOMContentLoaded', function() {
        // Kiểm tra nếu có toast-manager
        if (typeof window.toastManager === 'undefined') {
            console.warn('ToastManager not found. Loading toast-manager.js...');
            loadToastManager();
            return;
        }
        
        initializeAdminToasts();
    });

    function loadToastManager() {
        const script = document.createElement('script');
        script.src = '/js/toast-manager.js';
        script.onload = function() {
            initializeAdminToasts();
        };
        script.onerror = function() {
            console.error('Failed to load toast-manager.js');
        };
        document.head.appendChild(script);
    }

    function initializeAdminToasts() {
        // Kiểm tra success message từ server
        const successMessage = getServerMessage('successMessage');
        if (successMessage) {
            window.toastManager.success(successMessage, 4000);
        }
        
        // Kiểm tra error message từ server
        const errorMessage = getServerMessage('errorMessage');
        if (errorMessage) {
            window.toastManager.error(errorMessage, 5000);
        }

        // Kiểm tra warning message từ server
        const warningMessage = getServerMessage('warningMessage');
        if (warningMessage) {
            window.toastManager.warning(warningMessage, 4000);
        }

        // Kiểm tra info message từ server
        const infoMessage = getServerMessage('infoMessage');
        if (infoMessage) {
            window.toastManager.info(infoMessage, 3000);
        }
    }

    function getServerMessage(messageName) {
        // Tìm element chứa message từ server (Thymeleaf)
        const messageElement = document.querySelector(`[data-server-message="${messageName}"]`);
        if (messageElement) {
            return messageElement.textContent.trim();
        }

        // Fallback: tìm trong meta tags
        const metaElement = document.querySelector(`meta[name="${messageName}"]`);
        if (metaElement) {
            return metaElement.getAttribute('content');
        }

        // Fallback: tìm trong hidden input
        const inputElement = document.querySelector(`input[name="${messageName}"]`);
        if (inputElement) {
            return inputElement.value;
        }

        return null;
    }

    // Tạo helper functions cho admin pages
    window.AdminToast = {
        success: function(message, duration = 4000) {
            if (window.toastManager) {
                window.toastManager.success(message, duration);
            }
        },
        
        error: function(message, duration = 5000) {
            if (window.toastManager) {
                window.toastManager.error(message, duration);
            }
        },
        
        warning: function(message, duration = 4000) {
            if (window.toastManager) {
                window.toastManager.warning(message, duration);
            }
        },
        
        info: function(message, duration = 3000) {
            if (window.toastManager) {
                window.toastManager.info(message, duration);
            }
        },

        // Hiển thị toast từ AJAX response
        showFromResponse: function(response) {
            if (response && response.message) {
                const type = response.success ? 'success' : 'error';
                const duration = response.success ? 4000 : 5000;
                this[type](response.message, duration);
            }
        }
    };

})();
