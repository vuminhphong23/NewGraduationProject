package GraduationProject.forumikaa.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class FileUploadRequest {
    private MultipartFile file;
    private Long postId;
    private String description;
}


