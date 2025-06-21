package GraduationProject.forumikaa.controller;

import GraduationProject.forumikaa.service.PasswordResetService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ForgotPasswordController {

    @Autowired
    private PasswordResetService passwordResetService;

    @GetMapping("/forgot-password")
    public String showForgotPasswordForm(Model model) {
        model.addAttribute("email", "");
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String sendOtp(@RequestParam("email") String email, 
                         RedirectAttributes redirectAttributes) {
        
        if (email == null || email.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please enter your email address.");
            return "redirect:/forgot-password";
        }

        boolean success = passwordResetService.sendOtp(email.trim());
        
        if (success) {
            redirectAttributes.addFlashAttribute("successMessage", 
                "OTP has been sent to your email address. Please check your inbox.");
            redirectAttributes.addFlashAttribute("email", email.trim());
            return "redirect:/reset-password";
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Email not found or failed to send OTP. Please try again.");
            return "redirect:/forgot-password";
        }
    }

    @GetMapping("/reset-password")
    public String showResetPasswordForm(Model model) {
        String email = (String) model.getAttribute("email");
        if (email == null) {
            return "redirect:/forgot-password";
        }
        model.addAttribute("email", email);
        model.addAttribute("otp", "");
        model.addAttribute("newPassword", "");
        model.addAttribute("confirmPassword", "");
        return "reset-password";
    }

    @PostMapping("/reset-password")
    public String resetPassword(@RequestParam("email") String email,
                               @RequestParam("otp") String otp,
                               @RequestParam("newPassword") String newPassword,
                               @RequestParam("confirmPassword") String confirmPassword,
                               RedirectAttributes redirectAttributes) {
        
        // A helper lambda to handle redirecting with errors
        // This avoids repeating the same lines of code.
        Runnable_with_String addErrorAndRedirect = (errorMessage) -> {
            redirectAttributes.addFlashAttribute("errorMessage", errorMessage);
            redirectAttributes.addFlashAttribute("email", email); // Always pass email back
        };

        // Validation
        if (email == null || email.trim().isEmpty()) {
            addErrorAndRedirect.run("An unexpected error occurred. Please try again from the beginning.");
            return "redirect:/forgot-password";
        }

        if (otp == null || otp.trim().isEmpty()) {
            addErrorAndRedirect.run("OTP is required.");
            return "redirect:/reset-password";
        }

        if (newPassword == null || newPassword.trim().isEmpty()) {
            addErrorAndRedirect.run("New password is required.");
            return "redirect:/reset-password";
        }

        if (newPassword.length() < 6) {
            addErrorAndRedirect.run("Password must be at least 6 characters long.");
            return "redirect:/reset-password";
        }

        if (!newPassword.equals(confirmPassword)) {
            addErrorAndRedirect.run("Passwords do not match.");
            return "redirect:/reset-password";
        }

        // Reset password
        boolean success = passwordResetService.resetPassword(email.trim(), otp.trim(), newPassword);
        
        if (success) {
            redirectAttributes.addFlashAttribute("successMessage", 
                "Password has been reset successfully. Please login with your new password.");
            return "redirect:/login";
        } else {
            addErrorAndRedirect.run("Invalid OTP or OTP has expired. Please try again.");
            return "redirect:/reset-password";
        }
    }

    // A simple functional interface for our helper lambda
    @FunctionalInterface
    interface Runnable_with_String {
        void run(String str);
    }
} 