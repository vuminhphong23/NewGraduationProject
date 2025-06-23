// register.js
// Slide-in effect for register page
if (document.body.classList.contains('register-page') || document.querySelector('.login-form h2')?.textContent?.toLowerCase().includes('register')) {
    document.body.classList.add('pre-slide-in-right');
    setTimeout(function() {
        document.body.classList.add('slide-in-right');
        document.body.classList.remove('pre-slide-in-right');
    }, 10);
} 