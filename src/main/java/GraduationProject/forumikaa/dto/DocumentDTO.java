package GraduationProject.forumikaa.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentDTO {
    private Long id;
    private String name;
    private String size;
    private String type;
    private String url;
    private int downloadCount = 0;
    private String originalName;
    private String fileName;
    private String downloadUrl;
    private String previewUrl;
    private String mimeType;
    private String fileType;
    private Long fileSize;
    private String uploadDate;
}

