/**
 * Toast Manager - Quản lý toast notifications tập trung
 * Sử dụng cho toàn bộ ứng dụng để đảm bảo tính đồng bộ
 */

class ToastManager {
    constructor() {
        this.toastContainer = null;
        this.toastQueue = [];
        this.isProcessing = false;
        this.maxToasts = 5; // Số lượng toast tối đa hiển thị cùng lúc
        this.init();
    }

    init() {
        // Tạo container cho toast nếu chưa có
        if (!this.toastContainer) {
            this.createToastContainer();
        }
    }

    createToastContainer() {
        this.toastContainer = document.createElement('div');
        this.toastContainer.id = 'toast-container';
        this.toastContainer.className = 'toast-container';
        this.toastContainer.style.cssText = `
            position: fixed;
            top: 20px;
            right: 20px;
            z-index: 9999;
            max-width: 350px;
            pointer-events: none;
        `;
        
        // Thêm CSS animation cho pulse effect
        if (!document.getElementById('toast-animations')) {
            const style = document.createElement('style');
            style.id = 'toast-animations';
            style.textContent = `
                @keyframes pulse {
                    0% { box-shadow: 0 4px 12px rgba(0,0,0,0.15); }
                    50% { box-shadow: 0 4px 20px rgba(220, 38, 38, 0.4); }
                    100% { box-shadow: 0 4px 12px rgba(0,0,0,0.15); }
                }
            `;
            document.head.appendChild(style);
        }
        
        document.body.appendChild(this.toastContainer);
    }

    /**
     * Hiển thị toast notification
     * @param {string} message - Nội dung thông báo
     * @param {string} type - Loại toast: 'success', 'error', 'warning', 'info'
     * @param {number} duration - Thời gian hiển thị (ms), mặc định 3000ms
     * @param {Object} options - Tùy chọn bổ sung
     */
    show(message, type = 'info', duration = 3000, options = {}) {
        const toast = this.createToast(message, type, options);
        
        // Thêm vào queue
        this.toastQueue.push(toast);
        
        // Xử lý queue
        this.processQueue();
        
        // Tự động ẩn sau thời gian chỉ định
        if (duration > 0) {
            setTimeout(() => {
                this.hide(toast);
            }, duration);
        }

        return toast;
    }

    /**
     * Hiển thị toast thành công
     */
    success(message, duration = 3000, options = {}) {
        return this.show(message, 'success', duration, options);
    }

    /**
     * Hiển thị toast lỗi
     */
    error(message, duration = 4000, options = {}) {
        return this.show(message, 'error', duration, options);
    }

    /**
     * Hiển thị toast cảnh báo
     */
    warning(message, duration = 3500, options = {}) {
        return this.show(message, 'warning', duration, options);
    }

    /**
     * Hiển thị toast thông tin
     */
    info(message, duration = 3000, options = {}) {
        return this.show(message, 'info', duration, options);
    }

    createToast(message, type, options) {
        const toast = document.createElement('div');
        toast.className = `toast-item toast-${type}`;
        toast.style.cssText = `
            background: ${this.getToastColor(type)};
            color: white;
            padding: 12px 16px;
            margin-bottom: 8px;
            border-radius: 8px;
            box-shadow: 0 4px 12px rgba(0,0,0,0.15);
            transform: translateX(100%);
            transition: all 0.3s ease;
            pointer-events: auto;
            display: flex;
            align-items: center;
            gap: 12px;
            max-width: 100%;
            word-wrap: break-word;
            ${type === 'error' ? 'animation: pulse 2s infinite;' : ''}
        `;

        // Icon cho từng loại toast
        const icon = this.getToastIcon(type);
        
        toast.innerHTML = `
            <div class="toast-icon" style="flex-shrink: 0; font-size: 18px;">
                ${icon}
            </div>
            <div class="toast-message" style="flex-grow: 1; font-size: 14px; line-height: 1.4;">
                ${this.escapeHtml(message)}
            </div>
            <button class="toast-close" style="
                background: none;
                border: none;
                color: white;
                cursor: pointer;
                padding: 0;
                font-size: 16px;
                opacity: 0.7;
                transition: opacity 0.2s ease;
                flex-shrink: 0;
            " onclick="window.toastManager.hide(this.parentElement)">
                ×
            </button>
        `;

        // Thêm event listener cho nút đóng
        const closeBtn = toast.querySelector('.toast-close');
        closeBtn.addEventListener('mouseenter', () => {
            closeBtn.style.opacity = '1';
        });
        closeBtn.addEventListener('mouseleave', () => {
            closeBtn.style.opacity = '0.7';
        });

        return toast;
    }

    getToastColor(type) {
        const colors = {
            success: 'linear-gradient(135deg, #10b981 0%, #059669 100%)',
            error: 'linear-gradient(135deg, #dc2626 0%, #b91c1c 100%)',
            warning: 'linear-gradient(135deg, #f59e0b 0%, #d97706 100%)',
            info: 'linear-gradient(135deg, #3b82f6 0%, #2563eb 100%)'
        };
        return colors[type] || colors.info;
    }

    getToastIcon(type) {
        const icons = {
            success: '<i class="fa fa-check-circle"></i>',
            error: '<i class="fa fa-exclamation-circle"></i>',
            warning: '<i class="fa fa-exclamation-triangle"></i>',
            info: '<i class="fa fa-info-circle"></i>'
        };
        return icons[type] || icons.info;
    }

    processQueue() {
        if (this.isProcessing || this.toastQueue.length === 0) {
            return;
        }

        this.isProcessing = true;

        // Giới hạn số lượng toast hiển thị
        const currentToasts = this.toastContainer.children.length;
        const canShow = this.maxToasts - currentToasts;

        if (canShow > 0) {
            const toast = this.toastQueue.shift();
            this.toastContainer.appendChild(toast);
            
            // Hiệu ứng slide in
            requestAnimationFrame(() => {
                toast.style.transform = 'translateX(0)';
            });
        }

        this.isProcessing = false;

        // Xử lý tiếp nếu còn trong queue
        if (this.toastQueue.length > 0) {
            setTimeout(() => this.processQueue(), 100);
        }
    }

    /**
     * Ẩn toast
     */
    hide(toast) {
        if (!toast || !toast.parentElement) return;

        // Hiệu ứng slide out
        toast.style.transform = 'translateX(100%)';
        
        setTimeout(() => {
            if (toast.parentElement) {
                toast.parentElement.removeChild(toast);
            }
        }, 300);
    }

    /**
     * Ẩn tất cả toast
     */
    hideAll() {
        const toasts = this.toastContainer.querySelectorAll('.toast-item');
        toasts.forEach(toast => this.hide(toast));
        
        // Xóa queue
        this.toastQueue = [];
    }

    /**
     * Escape HTML để tránh XSS
     */
    escapeHtml(str) {
        const div = document.createElement('div');
        div.textContent = str;
        return div.innerHTML;
    }

    /**
     * Cập nhật cấu hình
     */
    configure(config) {
        if (config.maxToasts !== undefined) {
            this.maxToasts = config.maxToasts;
        }
        if (config.position !== undefined) {
            this.updatePosition(config.position);
        }
    }

    updatePosition(position) {
        if (!this.toastContainer) return;

        const positions = {
            'top-right': { top: '20px', right: '20px', left: 'auto', bottom: 'auto' },
            'top-left': { top: '20px', left: '20px', right: 'auto', bottom: 'auto' },
            'bottom-right': { bottom: '20px', right: '20px', top: 'auto', left: 'auto' },
            'bottom-left': { bottom: '20px', left: '20px', top: 'auto', right: 'auto' },
            'top-center': { top: '20px', left: '50%', transform: 'translateX(-50%)', right: 'auto', bottom: 'auto' },
            'bottom-center': { bottom: '20px', left: '50%', transform: 'translateX(-50%)', top: 'auto', right: 'auto' }
        };

        const pos = positions[position] || positions['top-right'];
        Object.assign(this.toastContainer.style, pos);
    }
}

// Khởi tạo ToastManager toàn cục
window.toastManager = new ToastManager();

// Tạo alias ngắn gọn
window.showToast = (message, type = 'info', duration = 3000) => {
    return window.toastManager.show(message, type, duration);
};

// Export cho module systems
if (typeof module !== 'undefined' && module.exports) {
    module.exports = ToastManager;
}

