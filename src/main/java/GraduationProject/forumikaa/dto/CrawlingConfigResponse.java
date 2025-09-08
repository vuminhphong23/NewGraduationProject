package GraduationProject.forumikaa.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CrawlingConfigResponse {
    private Long id;
    private String name;
    private String description;
    private String baseUrl;
    private String titleSelector;
    private String contentSelector;
    private String linkSelector;
    private String imageSelector;
    private String authorSelector;
    private String dateSelector;
    private String topicName;
    private String topicSelector;
    private Integer maxPosts;
    private Integer intervalMinutes;
    private Boolean enabled;
    private String userAgent;
    private Integer timeout;
    private String additionalHeaders;
    private String postProcessingRules;
    private String status;
    private String lastError;
    private LocalDateTime lastCrawledAt;
    private Integer totalCrawled;
    private Integer successCount;
    private Integer errorCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
}


