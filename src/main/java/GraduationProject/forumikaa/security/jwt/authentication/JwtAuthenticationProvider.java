package GraduationProject.forumikaa.security.jwt.authentication;

import GraduationProject.forumikaa.security.jwt.TokenProvider;
import org.jspecify.annotations.Nullable;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.AuthenticationConverter;

import java.util.Collection;
import java.util.Map;

//AuthenticationManager được cấu trúc từ ProviderManager, vì vậy sẽ tượng trưng cho một AuthenticationProvider
public class JwtAuthenticationProvider implements AuthenticationProvider {
    private final TokenProvider tokenProvider;

    private final UserDetailsService userDetailsService;

    public JwtAuthenticationProvider(TokenProvider tokenProvider, UserDetailsService userDetailsService) {
        this.tokenProvider = tokenProvider;
        this.userDetailsService = userDetailsService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String token = (String) authentication.getCredentials();
        boolean isValidToken = tokenProvider.validateToken(token);
        if (!isValidToken){
            return null;
        }
        Map<String, Object> payload = tokenProvider.getPayload(token);
        String username = payload.get("username").toString();
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        return new JwtAuthenticationToken(userDetails, token, userDetails.getAuthorities());
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return JwtAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
