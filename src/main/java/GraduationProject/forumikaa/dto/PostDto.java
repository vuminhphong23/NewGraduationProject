package GraduationProject.forumikaa.dto;

import GraduationProject.forumikaa.entity.PostPrivacy;
import GraduationProject.forumikaa.entity.PostStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PostDto {
    private Long id;
    private String title;
    private String content;
    private Long userId;
    private String userName;
    private Long topicId;
    private String topicName;
    private PostStatus status;
    private PostPrivacy privacy;
    private LocalDateTime createdAt;
    private Long likeCount;
    private Long commentCount;
} 