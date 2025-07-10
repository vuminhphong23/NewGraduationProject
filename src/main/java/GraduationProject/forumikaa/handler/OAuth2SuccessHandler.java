package GraduationProject.forumikaa.handler;

import GraduationProject.forumikaa.dao.RoleDao;
import GraduationProject.forumikaa.dao.UserDao;
import GraduationProject.forumikaa.entity.Role;
import GraduationProject.forumikaa.entity.User;
import GraduationProject.forumikaa.service.CustomUserDetailsService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Set;

@Component
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private UserDao userDao;

    @Autowired
    public void setUserDao(UserDao userDao) {
        this.userDao = userDao;
    }

    private RoleDao roleDao;

    @Autowired
    public void setRoleDao(RoleDao roleDao) {
        this.roleDao = roleDao;
    }

    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                      HttpServletResponse response,
                                      Authentication authentication) throws IOException, ServletException {

        OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
        User user = processOAuth2User(oauth2User);

        // After creating/finding the user, create a new Authentication token
        // with our UserDetails, so that authentication.getName() returns our DB username.
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(user.getUsername());

        UsernamePasswordAuthenticationToken newAuthentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());

        // Update the security context with the new authentication token
        SecurityContextHolder.getContext().setAuthentication(newAuthentication);

        // Redirect user to the default target URL
        setDefaultTargetUrl("/");
        super.onAuthenticationSuccess(request, response, newAuthentication);
    }

    private User processOAuth2User(OAuth2User oauth2User) {
        String email = oauth2User.getAttribute("email");
        return userDao.findByEmail(email)
                .map(user -> {
                    updateExistingUser(user, oauth2User);
                    return user;
                })
                .orElseGet(() -> registerNewUser(oauth2User));
    }

    private User registerNewUser(OAuth2User oauth2User) {
        String email = oauth2User.getAttribute("email");
        String name = oauth2User.getAttribute("name");

        User newUser = new User();
        newUser.setEmail(email);

        // Create a unique username to avoid conflicts
        String baseUsername = email.split("@")[0];
        String finalUsername = baseUsername;
        int counter = 1;
        while (userDao.existsByUsername(finalUsername)) {
            finalUsername = baseUsername + (counter++);
        }
        newUser.setUsername(finalUsername);

        if (name != null && !name.isBlank()) {
            String[] nameParts = name.split(" ", 2);
            newUser.setFirstName(nameParts[0]);
            newUser.setLastName(nameParts.length > 1 ? nameParts[1] : "");
        } else {
            newUser.setFirstName("User");
            newUser.setLastName("");
        }

        newUser.setPassword("OAUTH2_USER"); // This password won't be used for login

        Role userRole = roleDao.findByName("ROLE_USER")
                .orElseThrow(() -> new RuntimeException("Default role 'ROLE_USER' not found."));
        newUser.setRoles(Set.of(userRole));

        return userDao.save(newUser);
    }

    private void updateExistingUser(User existingUser, OAuth2User oauth2User) {
        String name = oauth2User.getAttribute("name");
        if (name != null && !name.isBlank()) {
            String[] nameParts = name.split(" ", 2);
            String firstName = nameParts[0];
            String lastName = nameParts.length > 1 ? nameParts[1] : "";

            // Check if name needs updating to reduce unnecessary DB writes
            if (!firstName.equals(existingUser.getFirstName()) || !lastName.equals(existingUser.getLastName())) {
                existingUser.setFirstName(firstName);
                existingUser.setLastName(lastName);
                userDao.save(existingUser);
            }
        }
    }
} 