package GraduationProject.forumikaa.service;

import GraduationProject.forumikaa.dao.UserDao;
import GraduationProject.forumikaa.entity.User;
import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;


@Service
public class PasswordResetService {

    private UserDao userDao;

    @Autowired
    public void setUserDao(UserDao userDao) {
        this.userDao = userDao;
    }

    private EmailService emailService;

    @Autowired
    public void setEmailService(EmailService emailService) {
        this.emailService = emailService;
    }

    private PasswordEncoder passwordEncoder;

    @Autowired
    public void setPasswordEncoder(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    // Lưu trữ OTP
    private final Map<String, OtpData> otpStorage = new HashMap<>();

    private static final int OTP_EXPIRY_MINUTES = 10;

    public boolean sendOtp(String email) {
        Optional<User> userOpt = userDao.findByEmail(email);
        if (userOpt.isEmpty()) {
            return false;
        }

        String otp = generateOtp();
        LocalDateTime expiryTime = LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES);

        otpStorage.put(email, new OtpData(otp, expiryTime));

        return sendOtpEmail(email, otp);
    }

    private boolean sendOtpEmail(String email, String otp) {
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

        return otpData.otp.equals(otp);
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
        SecureRandom random = new SecureRandom();
        StringBuilder otp = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            otp.append(random.nextInt(10)); // Tạo OTP gồm 6 chữ số
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
