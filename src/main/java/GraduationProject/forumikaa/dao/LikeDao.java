package GraduationProject.forumikaa.dao;

import GraduationProject.forumikaa.entity.Like;
import GraduationProject.forumikaa.entity.LikeableType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LikeDao extends JpaRepository<Like, Long> {
    
    // Polymorphic methods
    Optional<Like> findByUserIdAndLikeableIdAndLikeableType(Long userId, Long likeableId, LikeableType likeableType);
    
    boolean existsByUserIdAndLikeableIdAndLikeableType(Long userId, Long likeableId, LikeableType likeableType);
    
    @Query("SELECT COUNT(l) FROM Like l WHERE l.likeableId = :likeableId AND l.likeableType = :likeableType")
    Long countByLikeableIdAndLikeableType(@Param("likeableId") Long likeableId, @Param("likeableType") LikeableType likeableType);
    
    // Legacy methods for backward compatibility (posts)
    @Deprecated
    default Optional<Like> findByUserIdAndPostId(Long userId, Long postId) {
        return findByUserIdAndLikeableIdAndLikeableType(userId, postId, LikeableType.POST);
    }
    
    @Deprecated
    default boolean existsByUserIdAndPostId(Long userId, Long postId) {
        return existsByUserIdAndLikeableIdAndLikeableType(userId, postId, LikeableType.POST);
    }
    
    @Deprecated
    default Long countByPostId(Long postId) {
        return countByLikeableIdAndLikeableType(postId, LikeableType.POST);
    }
    
    // SQL Server conditional insert to avoid unique key violation
    @Modifying
    @Query(value = "IF NOT EXISTS (SELECT 1 FROM likes WHERE user_id = :userId AND likeable_id = :likeableId AND likeable_type = :likeableType) INSERT INTO likes (user_id, likeable_id, likeable_type, created_at) VALUES (:userId, :likeableId, :likeableType, GETDATE())", nativeQuery = true)
    int insertIfNotExists(@Param("userId") Long userId, @Param("likeableId") Long likeableId, @Param("likeableType") String likeableType);
}
