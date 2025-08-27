package GraduationProject.forumikaa.service;

import GraduationProject.forumikaa.dto.UserExample;
import GraduationProject.forumikaa.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import java.util.Optional;

public interface UserService {
    
    @PreAuthorize("hasRole('ADMIN')")
    Page<User> findPaginated(String keyword, String status, String roleName, Pageable pageable);
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    
    @PreAuthorize("hasRole('ADMIN')")
    void updateUserEnabledStatus(Long userId, boolean enabled);
    
    @PreAuthorize("hasRole('ADMIN')")
    void deleteUser(Long userId);
    
    Optional<User> findById(Long userId);
    
    void save(User user);
    
    @PreAuthorize("hasRole('ADMIN')")
    List<User> findAll();

    boolean existsByUsername(String username, Long userId) ;

    boolean existsByEmail(String email, Long userId);

    boolean existsPhone(String phone, Long userId);
    boolean checkPassword(String password);
    
    // QBE methods
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    List<User> findByExample(UserExample example);
    
    @PreAuthorize("hasRole('ADMIN')")
    List<User> findByExampleExact(UserExample example);
    
    @PreAuthorize("hasRole('ADMIN')")
    Page<User> findByExample(UserExample example, Pageable pageable);
    
    @PreAuthorize("hasRole('ADMIN')")
    long countByExample(UserExample example);
    
    @PreAuthorize("hasRole('ADMIN')")
    boolean existsByExample(UserExample example);

} 