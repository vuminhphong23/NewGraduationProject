package GraduationProject.forumikaa.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CrawlingConfigResponse {
    private Long id;
    private String name;
    private String description;
    private String baseUrl;
    private String topicName;
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
    private java.util.List<Long> groupIds;
}


