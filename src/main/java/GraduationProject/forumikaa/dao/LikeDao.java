package GraduationProject.forumikaa.dao;

import GraduationProject.forumikaa.entity.Like;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LikeDao extends JpaRepository<Like, Long> {
    
    Optional<Like> findByUserIdAndPostId(Long userId, Long postId);
    
    boolean existsByUserIdAndPostId(Long userId, Long postId);
    
    @Modifying
    @Query("DELETE FROM Like l WHERE l.user.id = :userId AND l.post.id = :postId")
    void deleteByUserIdAndPostId(@Param("userId") Long userId, @Param("postId") Long postId);
    
    @Query("SELECT COUNT(l) FROM Like l WHERE l.post.id = :postId")
    Long countByPostId(@Param("postId") Long postId);

    // SQL Server conditional insert to avoid unique key violation
    @Modifying
    @Query(value = "IF NOT EXISTS (SELECT 1 FROM likes WHERE user_id = :userId AND post_id = :postId) INSERT INTO likes (user_id, post_id, created_at) VALUES (:userId, :postId, GETDATE())", nativeQuery = true)
    int insertIfNotExists(@Param("userId") Long userId, @Param("postId") Long postId);
}
