package GraduationProject.forumikaa.dao;

import GraduationProject.forumikaa.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentDao extends JpaRepository<Document, Long> {
    
    List<Document> findByPostId(Long postId);
    
    List<Document> findByUserId(Long userId);
    
    @Query("SELECT d FROM Document d WHERE d.post.id = :postId ORDER BY d.uploadedAt ASC")
    List<Document> findDocumentsByPostId(@Param("postId") Long postId);
    
    @Query("SELECT COUNT(d) FROM Document d WHERE d.post.id = :postId")
    Long countDocumentsByPostId(@Param("postId") Long postId);
    
}


