package GraduationProject.forumikaa.config;

import GraduationProject.forumikaa.security.jwt.JwtTokenProvider;
import GraduationProject.forumikaa.security.jwt.TokenProvider;
import GraduationProject.forumikaa.security.jwt.authentication.JwtAuthenticationConvertor;
import GraduationProject.forumikaa.security.jwt.authentication.JwtAuthenticationFilter;
import GraduationProject.forumikaa.security.jwt.authentication.JwtAuthenticationProvider;
import GraduationProject.forumikaa.service.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }


    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, CustomUserDetailsService customUserDetailsService, JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/auth/login").permitAll()
                        .requestMatchers("/login", "/register", "/forgot-password", "/reset-password").permitAll()
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/fonts/**", "/static/**", "/favicon.ico").permitAll()
                        .requestMatchers("/ws/**").authenticated()
                        .requestMatchers("/api/**").authenticated()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )

                .logout(Customizer.withDefaults())
                .exceptionHandling(exceptions -> exceptions
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            // Redirect to login page for 403 errors
                            response.sendRedirect("/login?error=access_denied");
                        })
                        .authenticationEntryPoint((request, response, authException) -> {
                            // Redirect to login page for unauthenticated users
                            response.sendRedirect("/login?error=unauthorized");
                        })
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .userDetailsService(customUserDetailsService)
                .addFilterAfter(jwtAuthenticationFilter, LogoutFilter.class);

        return http.build();
    }

    @Bean
    public TokenProvider tokenProvider(@Value("${jwt.secret:defaultSecretKey}") String secret,
                                       @Value("${jwt.issuer:forumikaa}") String issuer,
                                       @Value("${jwt.expiration:86400}") long expirationSeconds) {
        return new JwtTokenProvider(secret, issuer, expirationSeconds);
    }

    @Bean
    public JwtAuthenticationProvider jwtAuthenticationProvider(TokenProvider tokenProvider, UserDetailsService userDetailsService) {
        return new JwtAuthenticationProvider(tokenProvider, userDetailsService);
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtAuthenticationProvider jwtAuthenticationProvider) {
        AuthenticationManager authenticationManager = new ProviderManager(jwtAuthenticationProvider);
        return new JwtAuthenticationFilter(authenticationManager, new JwtAuthenticationConvertor());
    }
}
