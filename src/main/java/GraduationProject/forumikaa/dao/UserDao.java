package GraduationProject.forumikaa.dao;

import GraduationProject.forumikaa.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserDao extends JpaRepository<User, Long>, JpaSpecificationExecutor<User>{
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    Optional<User> findByUsername(String username);
    Optional<User> findByPhone(String phone);
    
    // Tìm kiếm user theo username và loại trừ user hiện tại
    List<User> findByUsernameContainingIgnoreCaseAndIdNot(String username, Long userId);
    
    @Query("""
           SELECT u FROM User u
           LEFT JOIN FETCH u.userProfile
           WHERE u.id != :currentUserId
             AND u.id NOT IN (
                 SELECT CASE WHEN f.user.id = :currentUserId THEN f.friend.id ELSE f.user.id END
                 FROM Friendship f
                 WHERE (f.user.id = :currentUserId OR f.friend.id = :currentUserId)
             )
           ORDER BY RAND()
           """)
    List<User> findSuggestedUsers(@Param("currentUserId") Long currentUserId, Pageable pageable);
}
