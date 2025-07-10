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

// Slide-in effect for login page
if (document.body.classList.contains('login-page') || document.querySelector('.login-form h2')?.textContent?.toLowerCase().includes('login')) {
    document.body.classList.add('pre-slide-in-left');
    setTimeout(function() {
        document.body.classList.add('slide-in-left');
        document.body.classList.remove('pre-slide-in-left');
    }, 10);
}

document.addEventListener("DOMContentLoaded", function() {
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
});


