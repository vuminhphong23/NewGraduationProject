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
    
    @NotBlank(message = "Topic name không được để trống")
    private String topicName;
    
    @NotNull(message = "Max posts không được để trống")
    @Min(value = 1, message = "Max posts phải lớn hơn 0")
    @Max(value = 100, message = "Max posts không được vượt quá 100")
    private Integer maxPosts = 10;
    
    @NotNull(message = "Interval không được để trống")
    @Min(value = 5, message = "Interval phải lớn hơn 5 phút")
    @Max(value = 1440, message = "Interval không được vượt quá 1440 phút")
    private Integer intervalMinutes = 60;
    
    private Boolean enabled = true;
    private String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36";
    private Integer timeout = 10000;
    private String additionalHeaders;
    private String postProcessingRules;
    
    private java.util.List<Long> groupIds;
}


