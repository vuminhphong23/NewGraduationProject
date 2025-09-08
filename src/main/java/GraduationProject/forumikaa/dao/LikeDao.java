package GraduationProject.forumikaa.dao;

import GraduationProject.forumikaa.entity.Like;
import GraduationProject.forumikaa.entity.LikeableType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
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
    
    // Methods for recommendation system
    @Query("SELECT l FROM Like l WHERE l.user.id = :userId AND l.likeableType = :likeableType")
    List<Like> findByUserIdAndLikeableType(@Param("userId") Long userId, @Param("likeableType") LikeableType likeableType);
    
    // Method to find posts liked by user
    @Query("SELECT p FROM Post p JOIN Like l ON p.id = l.likeableId WHERE l.user.id = :userId AND l.likeableType = 'POST'")
    List<GraduationProject.forumikaa.entity.Post> findPostsLikedByUser(@Param("userId") Long userId);
}
