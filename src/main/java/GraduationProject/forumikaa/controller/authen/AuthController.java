package GraduationProject.forumikaa.controller.authen;

import GraduationProject.forumikaa.dto.LoginRequest;
import GraduationProject.forumikaa.dto.LoginResponse;
import GraduationProject.forumikaa.dto.UserBasicDto;
import GraduationProject.forumikaa.security.jwt.JwtCookieService;
import GraduationProject.forumikaa.security.jwt.TokenProvider;
import GraduationProject.forumikaa.util.SecurityUtil;
import GraduationProject.forumikaa.entity.User;
import GraduationProject.forumikaa.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;
    private final JwtCookieService jwtCookieService;
    
    @Autowired
    private SecurityUtil securityUtil;

    @Autowired
    private UserService userService;

    public AuthController(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder, TokenProvider tokenProvider, JwtCookieService jwtCookieService) {
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.jwtCookieService = jwtCookieService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request, HttpServletResponse response) {
        String username = request.getUsername();
        String password = request.getPassword();

        // Verify username and password
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        if (!passwordEncoder.matches(password, userDetails.getPassword())) {
            throw new RuntimeException("Invalid username or password");
        }
        if (!userDetails.isEnabled()) {
            throw new RuntimeException("User is disabled");
        }

        // Generate token with roles and userId
        List<String> roles = userDetails.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .toList();
        
        // Lấy userId từ User entity
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Map<String, Object> payload = new HashMap<>();
        payload.put("username", username);
        payload.put("userId", user.getId());
        payload.put("roles", roles);

        
        String token = tokenProvider.generateToken(payload);
        
        // Tạo JWT cookie (7 ngày = 604800 giây)
        jwtCookieService.createJwtCookie(response, token, 604800);
        
        return ResponseEntity.ok(new LoginResponse(token, roles));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        // Xóa JWT cookie
        jwtCookieService.removeJwtCookie(response);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser() {
        Long userId = securityUtil.getCurrentUserId();
        // Lấy thông tin user từ database
        User user = userService.findById(userId).orElse(null);

        // Xử lý UserProfile có thể null hoặc LazyInitializationException
        String avatar = null;
        String socialLinks = null;
        try {
            if (user.getUserProfile() != null) {
                avatar = user.getUserProfile().getAvatar();
                socialLinks = user.getUserProfile().getSocialLinks();
            }
        } catch (Exception e) {
            // LazyInitializationException hoặc lỗi khác
            avatar = null;
            socialLinks = null;
        }
        
        UserBasicDto userInfoResponse = new UserBasicDto(
                user.getId(),
                user.getUsername(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                avatar,
                socialLinks
        );

        return ResponseEntity.ok(userInfoResponse);
    }

}
