/**
 * JWT Token và Cookie Management Utility
 * Hỗ trợ cả localStorage và cookie để duy trì authentication state
 */

class JwtUtils {
    constructor() {
        this.TOKEN_KEY = 'jwt_token';
        this.COOKIE_NAME = 'jwt_token';
        this.isReady = true;
    }

    /**
     * Lưu JWT token vào cả localStorage và cookie
     */
    saveToken(token, expiresInDays = 7) {
        try {
            // Lưu vào localStorage
            localStorage.setItem(this.TOKEN_KEY, token);
            
            // Lưu vào cookie
            this.setCookie(this.COOKIE_NAME, token, expiresInDays);
        } catch (error) {
            console.error('Error saving token:', error);
        }
    }

    /**
     * Lấy JWT token từ localStorage hoặc cookie
     */
    getToken() {
        try {
            // Ưu tiên localStorage trước
            let token = localStorage.getItem(this.TOKEN_KEY);
            // console.log('JWT Debug - Token from localStorage:', !!token);
            
            if (!token) {
                // Nếu không có trong localStorage, lấy từ cookie
                token = this.getCookie(this.COOKIE_NAME);
                // console.log('JWT Debug - Token from cookie:', !!token);
            }
            
            return token;
        } catch (error) {
            console.error('Error getting token:', error);
            return null;
        }
    }

    /**
     * Xóa JWT token khỏi cả localStorage và cookie
     */
    removeToken() {
        try {
            localStorage.removeItem(this.TOKEN_KEY);
            this.deleteCookie(this.COOKIE_NAME);
        } catch (error) {
            console.error('Error removing token:', error);
        }
    }

    /**
     * Kiểm tra xem có token hay không
     */
    hasToken() {
        return !!this.getToken();
    }

    /**
     * Lấy token để sử dụng trong Authorization header
     */
    getAuthHeader() {
        const token = this.getToken();
        return token ? `Bearer ${token}` : null;
    }

    /**
     * Tự động thêm Authorization header vào fetch request
     */
    async authenticatedFetch(url, options = {}) {
        const token = this.getToken();
        
        // console.log('JWT Debug - URL:', url);
        // console.log('JWT Debug - Token exists:', !!token);
        // console.log('JWT Debug - Token preview:', token ? token.substring(0, 20) + '...' : 'null');
        
        if (!token) {
            console.error('JWT Debug - No token found');
            throw new Error('Không có JWT token');
        }

        // Don't set Content-Type for FormData - let browser set it automatically
        const headers = {
            'Authorization': `Bearer ${token}`,
            ...options.headers
        };

        // Only set Content-Type to application/json if body is not FormData
        if (!(options.body instanceof FormData)) {
            headers['Content-Type'] = 'application/json';
        }
        
        // console.log('JWT Debug - Headers:', headers);

        try {
            const response = await fetch(url, {
                ...options,
                headers
            });

            // Nếu token hết hạn, xóa token và redirect về login
            if (response.status === 401) {
                this.removeToken();
                if (window.location.pathname !== '/login') {
                    window.location.href = '/login';
                }
                return;
            }

            return response;
        } catch (error) {
            console.error('Authenticated fetch error:', error);
            throw error;
        }
    }

    /**
     * Chuyển trang với authentication state được duy trì
     */
    navigateTo(url) {
        try {
            // Lưu token vào cookie trước khi chuyển trang
            const token = this.getToken();
            if (token) {
                this.setCookie(this.COOKIE_NAME, token, 7);
            }
            
            // Sử dụng window.location.href
            window.location.href = url;
        } catch (error) {
            console.error('Navigation error:', error);
            // Fallback to normal navigation
            window.location.href = url;
        }
    }

    /**
     * Cookie utilities
     */
    setCookie(name, value, days) {
        try {
            const expires = new Date();
            expires.setTime(expires.getTime() + (days * 24 * 60 * 60 * 1000));
            document.cookie = `${name}=${value};expires=${expires.toUTCString()};path=/`;
        } catch (error) {
            console.error('Error setting cookie:', error);
        }
    }

    getCookie(name) {
        try {
            const nameEQ = name + "=";
            const ca = document.cookie.split(';');
            for (let i = 0; i < ca.length; i++) {
                let c = ca[i];
                while (c.charAt(0) === ' ') c = c.substring(1, c.length);
                if (c.indexOf(nameEQ) === 0) return c.substring(nameEQ.length, c.length);
            }
            return null;
        } catch (error) {
            console.error('Error getting cookie:', error);
            return null;
        }
    }

    deleteCookie(name) {
        try {
            document.cookie = `${name}=;expires=Thu, 01 Jan 1970 00:00:00 UTC;path=/`;
        } catch (error) {
            console.error('Error deleting cookie:', error);
        }
    }

    /**
     * Kiểm tra token có hợp lệ không (basic validation)
     */
    isTokenValid() {
        try {
            const token = this.getToken();
            if (!token) return false;
            
            // Decode JWT payload (không verify signature)
            const payload = JSON.parse(atob(token.split('.')[1]));
            const currentTime = Math.floor(Date.now() / 1000);
            
            return payload.exp > currentTime;
        } catch (e) {
            return false;
        }
    }

    getPayload() {
        try {
            const token = this.getToken();
            if (!token) return null;
            const payload = JSON.parse(atob(token.split('.')[1]));
            return payload || null;
        } catch (e) {
            return null;
        }
    }

    getRoles() {
        const payload = this.getPayload();
        const roles = payload?.roles;
        if (Array.isArray(roles)) return roles;
        if (typeof roles === 'string') return [roles];
        return [];
    }

    hasRole(role) {
        const roles = this.getRoles();
        return roles.includes(role) || roles.includes(`ROLE_${role}`);
    }
}

// Tạo global instance ngay lập tức
window.jwtUtils = new JwtUtils();

// Fallback function để sử dụng khi JWT utility chưa sẵn sàng
window.authenticatedFetch = async function(url, options = {}) {
    if (window.jwtUtils && window.jwtUtils.authenticatedFetch) {
        return await window.jwtUtils.authenticatedFetch(url, options);
    } else {
        // Fallback: lấy token từ localStorage
        const token = localStorage.getItem('jwt_token');
        if (token) {
            options.headers = {
                'Authorization': `Bearer ${token}`,
                ...options.headers
            };
            
            // Only set Content-Type to application/json if body is not FormData
            if (!(options.body instanceof FormData)) {
                options.headers['Content-Type'] = 'application/json';
            }
        }
        return await fetch(url, options);
    }
};

// Auto-check token validity every 5 minutes
setInterval(() => {
    if (window.jwtUtils && window.jwtUtils.hasToken && window.jwtUtils.isTokenValid) {
        if (window.jwtUtils.hasToken() && !window.jwtUtils.isTokenValid()) {
            window.jwtUtils.removeToken();
            if (window.location.pathname !== '/login') {
                window.location.href = '/login';
            }
        }
    }
}, 5 * 60 * 1000);

// Log khi JWT utility đã sẵn sàng
console.log('JWT Utility đã sẵn sàng:', window.jwtUtils);
