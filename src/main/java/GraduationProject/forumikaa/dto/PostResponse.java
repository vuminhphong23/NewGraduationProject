package GraduationProject.forumikaa.dto;

import GraduationProject.forumikaa.entity.PostPrivacy;
import GraduationProject.forumikaa.entity.PostStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class PostResponse {
    private Long id;
    private String title;
    private String content;
    private Long userId;
    private String userName;
    private String userAvatar; // Avatar của user đăng post
    private List<String> topicNames; // Chỉ dùng tên topic thay vì Entity
    private PostStatus status;
    private PostPrivacy privacy;
    private LocalDateTime createdAt;
    private Long likeCount;
    private Long commentCount;
    private Long shareCount;
    private Boolean isLiked = false; // Whether current user liked this post
    private List<FileUploadResponse> documents;
    
    // Thêm fields cho crawled content
    private Double recommendationScore;
    private Boolean isCrawledContent = false;
    private UserResponse user;
    
    // Thêm fields cho group information
    private Long groupId;
    private String groupName;
    private String groupAvatar;
    private String groupDescription;
    private Long groupMemberCount;
} 