package GraduationProject.forumikaa.controller;

import GraduationProject.forumikaa.dto.LoginRequest;
import GraduationProject.forumikaa.dto.LoginResponse;
import GraduationProject.forumikaa.dto.UserRegistrationDto;
import GraduationProject.forumikaa.dto.UserProfileDto;
import GraduationProject.forumikaa.security.jwt.JwtCookieService;
import GraduationProject.forumikaa.security.jwt.TokenProvider;
import GraduationProject.forumikaa.service.CustomUserDetailsService;
import GraduationProject.forumikaa.util.SecurityUtil;
import GraduationProject.forumikaa.entity.User;
import GraduationProject.forumikaa.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.ErrorResponse;
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
        
        Map<String, Object> payload = new HashMap<>();
        payload.put("username", username);
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

        UserProfileDto userInfoResponse = new UserProfileDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName()
        );

        return ResponseEntity.ok(userInfoResponse);
    }

}
