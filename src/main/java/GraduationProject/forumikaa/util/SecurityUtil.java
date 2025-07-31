package GraduationProject.forumikaa.util;

import GraduationProject.forumikaa.dao.UserDao;
import GraduationProject.forumikaa.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
public class SecurityUtil {

    @Autowired
    private UserDao userDao;

    private UserDetails getUserDetails() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        System.out.println("Authentication object: " + auth);
        System.out.println("Is authenticated: " + (auth != null ? auth.isAuthenticated() : "null"));
        System.out.println("Principal: " + (auth != null ? auth.getPrincipal() : "null"));
        System.out.println("Principal class: " + (auth != null && auth.getPrincipal() != null ? auth.getPrincipal().getClass() : "null"));
        
        if (auth != null && auth.getPrincipal() instanceof UserDetails) {
            return (UserDetails) auth.getPrincipal();
        }
        throw new RuntimeException("User not authenticated - auth: " + auth + ", principal: " + (auth != null ? auth.getPrincipal() : "null"));
    }
    public User getCurrentUser() {
        String username = getCurrentUsername();
        System.out.println("Looking for user with username: " + username);
        return userDao.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found with username: " + username));
    }

    public String getCurrentUsername() {
        UserDetails userDetails = getUserDetails();
        String username = userDetails.getUsername();
        System.out.println("Current username from UserDetails: " + username);
        return username;
    }

    public Long getCurrentUserId() {
        User user = getCurrentUser();
        System.out.println("Current user ID: " + user.getId());
        return user.getId();
    }
}
