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

    // Lấy top hashtags theo usage count
    @Query("SELECT t FROM Topic t ORDER BY t.usageCount DESC")
    List<Topic> findTopTopicsByUsage();

    // Lấy trending hashtags
    @Query("SELECT t FROM Topic t WHERE t.isTrending = true ORDER BY t.usageCount DESC")
    List<Topic> findTrendingTopics();

    // Tìm hashtags có tên chứa keyword
    @Query("SELECT t FROM Topic t WHERE t.name LIKE %:keyword% ORDER BY t.usageCount DESC")
    List<Topic> findByNameContaining(@Param("keyword") String keyword);

    // Lấy hashtags phổ biến nhất (top N)
    @Query("SELECT t FROM Topic t ORDER BY t.usageCount DESC")
    List<Topic> findTopTopics();

    // Lấy tất cả hashtags
    @Query("SELECT t FROM Topic t")
    List<Topic> getAllTopics();

} 