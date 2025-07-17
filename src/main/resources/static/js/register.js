// register.js
// Slide-in effect for register page

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