/**
 * Logout Utility
 * Xử lý logout cho ứng dụng JWT
 */

class LogoutManager {
    constructor() {
        this.init();
    }

    init() {
        // Tìm tất cả các form logout và button logout
        this.setupLogoutForms();
        this.setupLogoutButtons();
    }

    setupLogoutForms() {
        // Xử lý các form logout có sẵn
        const logoutForms = document.querySelectorAll('form[action*="logout"]');
        logoutForms.forEach(form => {
            form.addEventListener('submit', (e) => {
                e.preventDefault();
                this.performLogout();
            });
        });
    }

    setupLogoutButtons() {
        // Xử lý các button logout
        const logoutButtons = document.querySelectorAll('button[onclick*="logout"], .logout-btn, [data-action="logout"]');
        logoutButtons.forEach(button => {
            button.addEventListener('click', (e) => {
                e.preventDefault();
                this.performLogout();
            });
        });

        // Xử lý các link logout
        const logoutLinks = document.querySelectorAll('a[href*="logout"], .logout-link');
        logoutLinks.forEach(link => {
            link.addEventListener('click', (e) => {
                e.preventDefault();
                this.performLogout();
            });
        });
    }

    async performLogout() {
        try {
            // Hiển thị loading nếu có
            this.showLoading();

            // Gọi API logout để xóa JWT cookie ở server
            const response = await fetch('/api/auth/logout', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                credentials: 'include' // Để gửi cookie
            });

            // Xóa token ở phía client
            if (window.jwtUtils && window.jwtUtils.removeToken) {
                window.jwtUtils.removeToken();
            } else {
                // Fallback: xóa localStorage và cookie
                localStorage.removeItem('jwt_token');
                localStorage.removeItem('roles');
                this.deleteCookie('jwt_token');
            }

            // Hiển thị thông báo thành công
            this.showSuccessMessage('Đăng xuất thành công!');

            // Chuyển hướng về trang login sau 1 giây
            setTimeout(() => {
                window.location.href = '/login';
            }, 1000);

        } catch (error) {
            console.error('Logout error:', error);
            
            // Vẫn xóa token ở client ngay cả khi API fail
            if (window.jwtUtils && window.jwtUtils.removeToken) {
                window.jwtUtils.removeToken();
            } else {
                localStorage.removeItem('jwt_token');
                localStorage.removeItem('roles');
                this.deleteCookie('jwt_token');
            }

            // Chuyển hướng về login
            window.location.href = '/login';
        }
    }

    showLoading() {
        // Tạo loading overlay nếu chưa có
        if (!document.getElementById('logout-loading')) {
            const loading = document.createElement('div');
            loading.id = 'logout-loading';
            loading.innerHTML = `
                <div style="position: fixed; top: 0; left: 0; width: 100%; height: 100%; 
                     background: rgba(0,0,0,0.5); z-index: 9999; display: flex; 
                     align-items: center; justify-content: center;">
                    <div style="background: white; padding: 20px; border-radius: 8px; text-align: center;">
                        <div class="spinner-border text-primary" role="status">
                            <span class="visually-hidden">Loading...</span>
                        </div>
                        <div class="mt-2">Đang đăng xuất...</div>
                    </div>
                </div>
            `;
            document.body.appendChild(loading);
        }
    }

    showSuccessMessage(message) {
        // Xóa loading
        const loading = document.getElementById('logout-loading');
        if (loading) {
            loading.remove();
        }

        // Hiển thị toast message using toastManager
        if (window.toastManager) {
            window.toastManager.success(message);
        } else {
            // Fallback: console log nếu không có toast
            console.log(`Success: ${message}`);
        }
    }

    deleteCookie(name) {
        document.cookie = `${name}=;expires=Thu, 01 Jan 1970 00:00:00 UTC;path=/`;
    }
}

// Khởi tạo logout manager khi DOM ready
document.addEventListener('DOMContentLoaded', () => {
    window.logoutManager = new LogoutManager();
});

// Cũng khởi tạo ngay lập tức nếu DOM đã sẵn sàng
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', () => {
        window.logoutManager = new LogoutManager();
    });
} else {
    window.logoutManager = new LogoutManager();
}

// Export function để sử dụng từ bên ngoài
window.performLogout = function() {
    if (window.logoutManager) {
        window.logoutManager.performLogout();
    }
};
