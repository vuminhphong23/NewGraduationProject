package GraduationProject.forumikaa.dao;

import GraduationProject.forumikaa.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.query.QueryByExampleExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserDao extends JpaRepository<User, Long>, JpaSpecificationExecutor<User>, QueryByExampleExecutor<User> {
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    Optional<User> findByUsername(String username);
    Optional<User> findByPhone(String phone);
    
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

//JpaSpecificationExecutor là một interface cung cấp các phương thức cho việc sử dụng
// JPA Criteria API để thực hiện các truy vấn động và phức tạp mà không cần phải viết SQL
// thủ công. Với JpaSpecificationExecutor, bạn có thể sử dụng Specification để xây dựng
// các truy vấn tùy chỉnh dựa trên điều kiện động. Bạn cũng không cần phải cài đặt những
// phương thức này, chúng sẽ được tự động sinh ra khi bạn sử dụng JpaSpecificationExecutor.

//findByEmail(String email): Spring Data JPA sẽ tự động tạo truy vấn SQL SELECT * FROM users WHERE email = ? tương ứng.