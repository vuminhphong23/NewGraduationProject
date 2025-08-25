package GraduationProject.forumikaa.security.jwt;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtCookieService {
    
    private static final String JWT_COOKIE_NAME = "jwt_token";
    
    @Value("${jwt.cookie.domain:}")
    private String cookieDomain;
    
    @Value("${jwt.cookie.path:/}")
    private String cookiePath;
    
    @Value("${jwt.cookie.secure:false}")
    private boolean cookieSecure;
    
    @Value("${jwt.cookie.httpOnly:true}")
    private boolean cookieHttpOnly;
    
    /**
     * Tạo JWT cookie
     */
    public void createJwtCookie(HttpServletResponse response, String token, int maxAgeSeconds) {
        Cookie cookie = new Cookie(JWT_COOKIE_NAME, token);
        cookie.setPath(cookiePath);
        cookie.setMaxAge(maxAgeSeconds);
        cookie.setHttpOnly(cookieHttpOnly);
        cookie.setSecure(cookieSecure);
        
        if (cookieDomain != null && !cookieDomain.trim().isEmpty()) {
            cookie.setDomain(cookieDomain);
        }
        
        response.addCookie(cookie);
    }
    
    /**
     * Xóa JWT cookie
     */
    public void removeJwtCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(JWT_COOKIE_NAME, "");
        cookie.setPath(cookiePath);
        cookie.setMaxAge(0);
        cookie.setHttpOnly(cookieHttpOnly);
        cookie.setSecure(cookieSecure);
        
        if (cookieDomain != null && !cookieDomain.trim().isEmpty()) {
            cookie.setDomain(cookieDomain);
        }
        
        response.addCookie(cookie);
    }
    
    /**
     * Lấy tên cookie
     */
    public String getCookieName() {
        return JWT_COOKIE_NAME;
    }
}
