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
        
        if (auth != null && auth.getPrincipal() instanceof UserDetails) {
            return (UserDetails) auth.getPrincipal();
        }
        throw new RuntimeException("User not authenticated - auth: " + auth + ", principal: " + (auth != null ? auth.getPrincipal() : "null"));
    }
    public User getCurrentUser() {
        String username = getCurrentUsername();
        return userDao.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found with username: " + username));
    }

    public String getCurrentUsername() {
        UserDetails userDetails = getUserDetails();
        String username = userDetails.getUsername();
        return username;
    }

    public Long getCurrentUserId() {
        try {
            User user = getCurrentUser();
            System.out.println("SecurityUtil.getCurrentUserId() - User ID: " + user.getId());
            return user.getId();
        } catch (Exception e) {
            System.err.println("SecurityUtil.getCurrentUserId() - Error: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}
