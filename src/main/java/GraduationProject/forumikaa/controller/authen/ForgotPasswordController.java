package GraduationProject.forumikaa.controller.authen;

import GraduationProject.forumikaa.service.PasswordResetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ForgotPasswordController {

    private PasswordResetService passwordResetService;

    @Autowired
    public void setPasswordResetService(PasswordResetService passwordResetService) {
        this.passwordResetService = passwordResetService;
    }

    @GetMapping("/forgot-password")
    public String showForgotPasswordForm() {
        return "user/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String requestOtp(@RequestParam("email") String email,
                             RedirectAttributes redirectAttributes) {
        boolean success = passwordResetService.sendOtp(email);

        if (success) {
            redirectAttributes.addFlashAttribute("successMessage", "OTP đã được gửi đến email của bạn.");
            redirectAttributes.addFlashAttribute("email", email);
            return "redirect:/reset-password";
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Email không tồn tại. Hãy thử lại!");
            return "redirect:/forgot-password";
        }
    }

    @GetMapping("/reset-password")
    public String showResetPasswordForm() {
        return "user/reset-password";
    }

    @PostMapping("/reset-password")
    public String resetPassword(@RequestParam("email") String email,
                                @RequestParam("otp") String otp,
                                @RequestParam("newPassword") String newPassword,
                                @RequestParam("confirmPassword") String confirmPassword,
                                RedirectAttributes redirectAttributes) {

        redirectAttributes.addFlashAttribute("email", email);

        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Mật khẩu mới và xác nhận mật khẩu không khớp.");
            return "redirect:/reset-password";
        }

        if (newPassword.length() < 6) {
            redirectAttributes.addFlashAttribute("errorMessage", "Mật khẩu phải có ít nhất 6 ký tự.");
            return "redirect:/reset-password";
        }

        boolean success = passwordResetService.resetPassword(email, otp, newPassword);

        if (success) {
            redirectAttributes.addFlashAttribute("successMessage", "Mật khẩu đã được đặt lại thành công. Hãy đăng nhập.");
            return "redirect:/login";
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "OTP đã hết hạn hoặc không chính xác. Hãy thử lại!");
            return "redirect:/reset-password";
        }
    }
} 