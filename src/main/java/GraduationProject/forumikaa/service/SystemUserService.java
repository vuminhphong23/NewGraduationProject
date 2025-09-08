package GraduationProject.forumikaa.service;

import GraduationProject.forumikaa.dao.UserDao;
import GraduationProject.forumikaa.dao.RoleDao;
import GraduationProject.forumikaa.entity.User;
import GraduationProject.forumikaa.entity.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
public class SystemUserService {
    
    @Autowired
    private UserDao userDao;
    
    @Autowired
    private RoleDao roleDao;
    
    @Transactional
    public User getOrCreateAdminUser() {
        // Tìm admin user có sẵn
        return userDao.findByUsername("admin")
                .orElseGet(() -> createAdminUser());
    }
    
    private User createAdminUser() {
        User adminUser = new User();
        adminUser.setUsername("admin");
        adminUser.setEmail("admin@forumikaa.com");
        adminUser.setPassword("$2a$10$dummy.password.hash"); // Dummy password
        adminUser.setFirstName("Forumikaa");
        adminUser.setLastName("Admin");
        adminUser.setProfileInfo("🔧 Quản trị viên hệ thống - Tự động thu thập nội dung");
        adminUser.setEnabled(true);
        
        // Gán role ADMIN
        Role adminRole = roleDao.findByName("ROLE_ADMIN")
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setName("ROLE_ADMIN");
                    return roleDao.save(role);
                });
        
        adminUser.setRoles(Set.of(adminRole));
        
        return userDao.save(adminUser);
    }
    
    public User getAdminUser() {
        return getOrCreateAdminUser();
    }
}


