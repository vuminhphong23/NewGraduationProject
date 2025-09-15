// login.js
// Toggle password visibility
var toggle = document.getElementById('togglePassword');
if (toggle) {
    toggle.addEventListener('click', function() {
        var password = document.getElementById('password');
        if (password.type === 'password') {
            password.type = 'text';
            this.classList.remove('fa-eye');
            this.classList.add('fa-eye-slash');
        } else {
            password.type = 'password';
            this.classList.remove('fa-eye-slash');
            this.classList.add('fa-eye');
        }
    });
}


document.addEventListener("DOMContentLoaded", function() {
    // Hiển thị thông báo từ server nếu có
    const serverErrorAlert = document.querySelector('.alert-danger');
    const serverSuccessAlert = document.querySelector('.alert-success');
    
    if (serverErrorAlert || serverSuccessAlert) {
        // Hiển thị thông báo ngay lập tức
        if (serverErrorAlert) {
            serverErrorAlert.style.display = 'block';
            serverErrorAlert.style.opacity = '1';
        }
        if (serverSuccessAlert) {
            serverSuccessAlert.style.display = 'block';
            serverSuccessAlert.style.opacity = '1';
        }
    }

    // Chọn tất cả các phần tử muốn hiệu ứng (trừ alert)
    const selectors = [
        '.icon-animated',
        '.left-section .logo span',
        '.welcome-content h1',
        '.welcome-content p',
        '.feature-list li',
        '.login-form h2',
        '.alert',
        '.login-form'
    ];
    const elements = [];
    selectors.forEach(sel => {
        document.querySelectorAll(sel).forEach(el => {
            elements.push(el);
        });
    });

    // Thêm class animated-fadein cho các phần tử
    elements.forEach(el => el.classList.add('animated-fadein'));

    // Hiện lần lượt từng phần tử
    elements.forEach((el, idx) => {
        setTimeout(() => {
            el.style.opacity = 1;
            el.style.transform = 'translateY(0)';
        }, 200 + idx * 120);
    });

    const loginForm = document.getElementById("loginForm");

    if (!loginForm) {
        return; // Không có form đăng nhập ở trang này
    }

    loginForm.addEventListener("submit", async function(e) {
        e.preventDefault();
        const username = document.getElementById("username").value;
        const password = document.getElementById("password").value;

        try {
            const response = await fetch("/api/auth/login", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                },
                body: JSON.stringify({
                    username: username,
                    password: password,
                }),
            });

            if (response.ok) {
                const data = await response.json();
                const token = data.token;
                const roles = data.roles || [];
                
                // Lưu token vào cả localStorage và cookie
                if (window.jwtUtils && window.jwtUtils.saveToken) {
                    window.jwtUtils.saveToken(token, 7);
                } else {
                    // Fallback nếu JWT utility chưa load
                    localStorage.setItem('jwt_token', token);
                    localStorage.setItem('roles', roles);
                }
                
                const isAdmin = (Array.isArray(roles) && roles.some(r => r === 'ROLE_ADMIN' || r === 'ADMIN'))
                    || (window.jwtUtils && window.jwtUtils.hasRole && window.jwtUtils.hasRole('ADMIN'));
                
                showSuccessMessage("Đăng nhập thành công!");
                setTimeout(() => {
                    window.location.href = isAdmin ? '/admin' : '/';
                }, 800);
            } else {
                const errorData = await response.json();
                showErrorMessage(errorData.message || "Đăng nhập thất bại!");
            }
        } catch (error) {
            console.error("Login error:", error);
            showErrorMessage("Có lỗi xảy ra khi đăng nhập!");
        }
    });
});

// Helper functions - Use toastManager for notifications
function showSuccessMessage(message) {
    // Use toastManager if available
    if (window.toastManager) {
        window.toastManager.success(message);
    } else {
        // Fallback to existing alert system
        const successDiv = document.getElementById('success-message');
        if (successDiv) {
            successDiv.textContent = message;
            successDiv.style.display = 'block';
            successDiv.className = 'alert alert-success';
            
            // Auto-hide after 3 seconds
            setTimeout(() => {
                successDiv.style.display = 'none';
            }, 3000);
        }
    }
}

function showErrorMessage(message) {
    // Use toastManager if available
    if (window.toastManager) {
        window.toastManager.error(message);
    } else {
        // Fallback to existing alert system
        const errorDiv = document.getElementById('error-message');
        if (errorDiv) {
            errorDiv.textContent = message;
            errorDiv.style.display = 'block';
            errorDiv.className = 'alert alert-danger';
            
            // Auto-hide after 5 seconds
            setTimeout(() => {
                errorDiv.style.display = 'none';
            }, 5000);
        }
    }
}


