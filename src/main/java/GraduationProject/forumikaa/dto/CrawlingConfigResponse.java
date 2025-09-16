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
    private Boolean enabled;
    private String status;
    private LocalDateTime lastCrawledAt;
    private Integer totalCrawled;
    private LocalDateTime createdAt;
    private String createdBy;
    private java.util.List<Long> groupIds;
}


