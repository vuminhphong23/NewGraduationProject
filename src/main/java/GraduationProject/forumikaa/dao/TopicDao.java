package GraduationProject.forumikaa.dao;

import GraduationProject.forumikaa.entity.Topic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface TopicDao extends JpaRepository<Topic, Long> {

    // Tìm hashtag theo tên
    Optional<Topic> findByName(String name);

    // Tìm hashtags có tên chứa keyword
    @Query("SELECT t FROM Topic t WHERE t.name LIKE %:keyword% ORDER BY t.usageCount DESC")
    List<Topic> findByNameContaining(@Param("keyword") String keyword);

    // Lấy hashtags phổ biến nhất (top N)
    @Query("SELECT t FROM Topic t ORDER BY t.usageCount DESC")
    List<Topic> findTopTopics();

    
    // Tìm topic theo tên (case insensitive)
    Optional<Topic> findByNameIgnoreCase(String name);

}