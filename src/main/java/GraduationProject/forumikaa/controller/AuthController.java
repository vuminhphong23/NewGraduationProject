package GraduationProject.forumikaa.controller;

import GraduationProject.forumikaa.dto.LoginRequest;
import GraduationProject.forumikaa.security.jwt.JwtCookieService;
import GraduationProject.forumikaa.security.jwt.TokenProvider;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;
    private final JwtCookieService jwtCookieService;

    public AuthController(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder, TokenProvider tokenProvider, JwtCookieService jwtCookieService) {
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.jwtCookieService = jwtCookieService;
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> body, HttpServletResponse response) {
        String username = body.get("username");
        String password = body.get("password");
        // Verify username and password
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        if (!passwordEncoder.matches(password, userDetails.getPassword())) {
            throw new RuntimeException("Invalid username or password");
        }
        if (!userDetails.isEnabled()) {
            throw new RuntimeException("User is disabled");
        }
        // Generate token with roles
        java.util.List<String> roles = userDetails.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .toList();
        String token = tokenProvider.generateToken(java.util.Map.of(
                "username", username,
                "roles", roles
        ));
        
        // Tạo JWT cookie (7 ngày = 604800 giây)
        jwtCookieService.createJwtCookie(response, token, 604800);
        
        return java.util.Map.of("token", token, "roles", roles);
    }

    @PostMapping("/logout")
    public Map<String, Object> logout(HttpServletResponse response) {
        // Xóa JWT cookie
        jwtCookieService.removeJwtCookie(response);
        return Map.of("message", "Đăng xuất thành công");
    }
}
