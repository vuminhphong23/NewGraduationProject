package GraduationProject.forumikaa.dao;

import GraduationProject.forumikaa.entity.CrawlingConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CrawlingConfigDao extends JpaRepository<CrawlingConfig, Long> {
    
    // Tìm configs đang active
    List<CrawlingConfig> findByEnabledTrueAndStatusOrderByCreatedAtDesc(Boolean enabled, String status);
    
    // Tìm configs theo status
    List<CrawlingConfig> findByStatusOrderByCreatedAtDesc(String status);
    
    // Tìm configs cần crawl (enabled và đến thời gian)
    @Query("SELECT c FROM CrawlingConfig c WHERE c.enabled = true AND c.status = 'ACTIVE' AND " +
           "(c.lastCrawledAt IS NULL OR c.lastCrawledAt < :cutoffTime)")
    List<CrawlingConfig> findConfigsReadyForCrawling(@Param("cutoffTime") LocalDateTime cutoffTime);
    
    // Tìm configs theo tên
    List<CrawlingConfig> findByNameContainingIgnoreCaseOrderByCreatedAtDesc(String name);
    
    // Tìm configs theo topic
    List<CrawlingConfig> findByTopicNameContainingIgnoreCaseOrderByCreatedAtDesc(String topicName);
    
    // Tìm configs theo URL
    List<CrawlingConfig> findByBaseUrlContainingIgnoreCaseOrderByCreatedAtDesc(String baseUrl);
    
    // Thống kê
    @Query("SELECT COUNT(c) FROM CrawlingConfig c WHERE c.enabled = true")
    Long countActiveConfigs();
    
    @Query("SELECT COUNT(c) FROM CrawlingConfig c WHERE c.status = 'ERROR'")
    Long countErrorConfigs();
    
    @Query("SELECT SUM(c.totalCrawled) FROM CrawlingConfig c WHERE c.enabled = true")
    Long getTotalCrawledPosts();
    
    // Tìm configs cần test
    @Query("SELECT c FROM CrawlingConfig c WHERE c.status = 'ACTIVE' AND c.enabled = true")
    List<CrawlingConfig> findConfigsForTesting();
}


