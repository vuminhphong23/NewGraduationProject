package GraduationProject.forumikaa.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@NoArgsConstructor
@Table(name = "crawling_configs")
public class CrawlingConfig {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 100)
    private String name;
    
    @Column(length = 500)
    private String description;
    
    @Column(nullable = false, length = 500)
    private String baseUrl;
    
    @Column(nullable = false, length = 100)
    private String topicName;
    
    @Column(nullable = false)
    private Integer maxPosts = 10;
    
    @Column(nullable = false)
    private Boolean enabled = true;
    
    @Column(length = 50)
    private String status = "ACTIVE";
    
    @Column
    private LocalDateTime lastCrawledAt;
    
    @Column
    private Integer totalCrawled = 0;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;
    
    @Column(length = 1000)
    private String groupIds; // JSON string of group IDs
    
}


