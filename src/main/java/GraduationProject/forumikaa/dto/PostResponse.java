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
    private String userAvatar;
    private List<String> topicNames;
    private PostStatus status;
    private PostPrivacy privacy;
    private LocalDateTime createdAt;
    private Long likeCount;
    private Long commentCount;
    private Long shareCount;
    private List<FileUploadResponse> documents;

} 