package GraduationProject.forumikaa.security.jwt.authentication;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationConverter;

public class JwtAuthenticationConvertor implements AuthenticationConverter {
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String AUTHORIZATION = "Authorization";
    private static final String JWT_COOKIE_NAME = "jwt_token";
    
    @Override
    public Authentication convert(HttpServletRequest request) {
        // Ưu tiên đọc từ Authorization header trước
        String authorizationHeader = request.getHeader(AUTHORIZATION);
        if (authorizationHeader != null && authorizationHeader.startsWith(BEARER_PREFIX)) {
            String token = authorizationHeader.substring(BEARER_PREFIX.length());
            return new JwtAuthenticationToken(token);
        }
        
        // Nếu không có header, đọc từ cookie
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (JWT_COOKIE_NAME.equals(cookie.getName())) {
                    String token = cookie.getValue();
                    if (token != null && !token.trim().isEmpty()) {
                        return new JwtAuthenticationToken(token);
                    }
                }
            }
        }
        
        return null;
    }
}
