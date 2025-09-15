package GraduationProject.forumikaa.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadResponse {
    private Long id;
    private String fileName;
    private String originalName;
    private String filePath;
    private Long fileSize;
    private String mimeType;
    private String downloadUrl;
    private String previewUrl;
    private String thumbnailUrl;
    private String cloudinaryPublicId;
    private String cloudinaryUrl;
    private boolean cloudStorage;
    private String fileType; // image, video, document
    
    // Thêm các thuộc tính cần thiết cho documents tab
    private String uploadDate; // Upload date
    private int downloadCount = 0; // Download count
}
