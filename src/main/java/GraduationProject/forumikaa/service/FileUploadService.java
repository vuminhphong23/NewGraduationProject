package GraduationProject.forumikaa.service;

import GraduationProject.forumikaa.dto.FileUploadResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface FileUploadService {
    
    FileUploadResponse uploadFile(MultipartFile file, Long postId, Long userId) throws IOException;
    
    List<FileUploadResponse> getFilesByPostId(Long postId);
    
    FileUploadResponse getFileById(Long fileId);
    
    void deleteFile(Long fileId, Long userId);
    
    byte[] downloadFile(Long fileId) throws IOException;
    
    String getFilePreviewUrl(Long fileId);
    
    byte[] downloadAllFilesAsZip(Long postId, Long userId) throws IOException;
}
