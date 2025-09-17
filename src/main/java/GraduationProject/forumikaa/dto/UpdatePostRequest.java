package GraduationProject.forumikaa.dto;

import GraduationProject.forumikaa.entity.PostPrivacy;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

@Data
public class UpdatePostRequest {
    @NotBlank(message = "Tiêu đề không được để trống")
    private String title;
    
    @NotBlank(message = "Nội dung không được để trống")
    private String content;
    
    // Legacy field for backward compatibility
    private Long topicId;
    
    // New field for hashtag system
    private List<String> topicNames;
    
    private PostPrivacy privacy;
    
    // Add groupId for group posts
    private Long groupId;
} 