package GraduationProject.forumikaa.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

@Data
public class CrawlingConfigRequest {
    
    @NotBlank(message = "Tên config không được để trống")
    private String name;
    
    private String description;
    
    @NotBlank(message = "URL không được để trống")
    private String baseUrl;
    
    private String topicName; // Will be set automatically from selected group
    
    @NotNull(message = "Max posts không được để trống")
    @Min(value = 1, message = "Max posts phải lớn hơn 0")
    @Max(value = 100, message = "Max posts không được vượt quá 100")
    private Integer maxPosts = 10;
    
    private Boolean enabled = true;
    
    private java.util.List<Long> groupIds;
}


