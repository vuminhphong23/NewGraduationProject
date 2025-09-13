package GraduationProject.forumikaa.dto;

import GraduationProject.forumikaa.entity.PostPrivacy;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

@Data
public class CreatePostRequest {
    @NotBlank(message = "Tiêu đề không được để trống")
    private String title;
    
    @NotBlank(message = "Nội dung không được để trống")
    private String content;
    
    // Keep topicId for backward compatibility (optional)
    private Long topicId;
    
    // Add topicNames for hashtag functionality
    private List<String> topicNames;
    
    private PostPrivacy privacy = PostPrivacy.PUBLIC; // Mặc định là public
    
    // Add groupId for group posts
    private Long groupId;
} 