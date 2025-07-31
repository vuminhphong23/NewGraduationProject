package GraduationProject.forumikaa.dto;

import GraduationProject.forumikaa.entity.PostPrivacy;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
public class CreatePostRequest {
    @NotBlank(message = "Tiêu đề không được để trống")
    private String title;
    
    @NotBlank(message = "Nội dung không được để trống")
    private String content;
    
    @NotNull(message = "Topic không được để trống")
    private Long topicId;
    
    private PostPrivacy privacy = PostPrivacy.PUBLIC; // Mặc định là public
} 