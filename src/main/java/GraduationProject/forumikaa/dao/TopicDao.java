package GraduationProject.forumikaa.dao;

import GraduationProject.forumikaa.entity.Topic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TopicDao extends JpaRepository<Topic, Long> {

    // Tìm hashtag theo tên
    Optional<Topic> findByName(String name);

    // Tìm hashtags có tên chứa keyword
    @Query("SELECT t FROM Topic t WHERE t.name LIKE %:keyword% ORDER BY t.usageCount DESC")
    List<Topic> findByNameContaining(@Param("keyword") String keyword);

    // Lấy hashtags phổ biến nhất (giới hạn số lượng từ DB)
    @Query(value = "SELECT * FROM topics ORDER BY usage_count DESC LIMIT :limit", nativeQuery = true)
    List<Topic> findTopTopics(@Param("limit") int limit);
    
    // Lấy hashtags phổ biến nhất trong group cụ thể
    @Query(value = "SELECT DISTINCT t.* FROM topics t " +
                   "INNER JOIN post_topics pt ON t.id = pt.topic_id " +
                   "INNER JOIN posts p ON pt.post_id = p.id " +
                   "WHERE p.group_id = :groupId " +
                   "ORDER BY t.usage_count DESC LIMIT :limit", nativeQuery = true)
    List<Topic> findTopTopicsByGroup(@Param("groupId") Long groupId, @Param("limit") int limit);
    
    // Tìm topic theo tên (case insensitive)
    Optional<Topic> findByNameIgnoreCase(String name);
    
    // Lấy topics phổ biến nhất theo usage count
    @Query("SELECT t FROM Topic t ORDER BY t.usageCount DESC")
    List<Topic> findTopTopicsByUsage(@Param("limit") int limit);

}