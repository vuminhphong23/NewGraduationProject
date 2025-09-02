package GraduationProject.forumikaa.dao;

import GraduationProject.forumikaa.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommentDao extends JpaRepository<Comment, Long> {
    
    List<Comment> findByPostIdOrderByCreatedAtAsc(Long postId);
    
    Page<Comment> findByPostIdOrderByCreatedAtDesc(Long postId, Pageable pageable);
    
    @Query("SELECT COUNT(c) FROM Comment c WHERE c.post.id = :postId")
    Long countByPostId(@Param("postId") Long postId);
    
    boolean existsByIdAndUserId(Long commentId, Long userId);
    
    @Query("SELECT c FROM Comment c WHERE c.id = :commentId")
    Optional<Comment> findById(@Param("commentId") Long commentId);
    
    @Query("SELECT c.post.id FROM Comment c WHERE c.id = :commentId")
    Optional<Long> findPostIdByCommentId(@Param("commentId") Long commentId);
    
    // Methods for recommendation system
    List<Comment> findByUserId(Long userId);
}

