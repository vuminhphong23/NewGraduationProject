package GraduationProject.forumikaa.service;

import GraduationProject.forumikaa.dao.UserDao;
import GraduationProject.forumikaa.entity.User;
import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

@Service
public class PasswordResetService {

    @Autowired
    private UserDao userDao;

    @Autowired
    private EmailService emailService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Lưu trữ OTP tạm thời (trong production nên dùng Redis)
    private final Map<String, OtpData> otpStorage = new HashMap<>();

    public boolean sendOtp(String email) {
        Optional<User> userOpt = userDao.findByEmail(email);
        if (userOpt.isEmpty()) {
            return false; // Email không tồn tại
        }

        String otp = generateOtp();
        LocalDateTime expiryTime = LocalDateTime.now().plusMinutes(10);

        // Lưu OTP
        otpStorage.put(email, new OtpData(otp, expiryTime));

        try {
            emailService.sendOtpEmail(email, otp);
            return true;
        } catch (MessagingException e) {
            otpStorage.remove(email); // Xóa OTP nếu gửi email thất bại
            return false;
        }
    }

    public boolean validateOtp(String email, String otp) {
        OtpData otpData = otpStorage.get(email);
        if (otpData == null) {
            return false;
        }

        if (LocalDateTime.now().isAfter(otpData.expiryTime)) {
            otpStorage.remove(email); // Xóa OTP hết hạn
            return false;
        }

        if (!otpData.otp.equals(otp)) {
            return false;
        }

        return true;
    }

    public boolean resetPassword(String email, String otp, String newPassword) {
        if (!validateOtp(email, otp)) {
            return false;
        }

        Optional<User> userOpt = userDao.findByEmail(email);
        if (userOpt.isEmpty()) {
            return false;
        }

        User user = userOpt.get();
        user.setPassword(passwordEncoder.encode(newPassword));
        userDao.save(user);

        // Xóa OTP sau khi reset thành công
        otpStorage.remove(email);
        return true;
    }

    private String generateOtp() {
        Random random = new Random();
        StringBuilder otp = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            otp.append(random.nextInt(10));
        }
        return otp.toString();
    }

    // Inner class để lưu trữ OTP và thời gian hết hạn
    private static class OtpData {
        final String otp;
        final LocalDateTime expiryTime;

        OtpData(String otp, LocalDateTime expiryTime) {
            this.otp = otp;
            this.expiryTime = expiryTime;
        }
    }
} 