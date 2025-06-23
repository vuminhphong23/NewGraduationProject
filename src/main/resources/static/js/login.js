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


