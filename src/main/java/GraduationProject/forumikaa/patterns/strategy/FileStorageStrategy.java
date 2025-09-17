package GraduationProject.forumikaa.patterns.strategy;

import GraduationProject.forumikaa.dto.FileUploadResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Strategy interface for file storage operations
 * Allows switching between different storage implementations (Local, Cloudinary, AWS S3, etc.)
 */
public interface FileStorageStrategy {



    FileUploadResponse uploadFile(MultipartFile file, Long postId, Long userId) throws Exception;

    List<FileUploadResponse> getFilesByPostId(Long postId);

    FileUploadResponse getFileById(Long fileId);

    void deleteFile(Long fileId, Long userId);

    byte[] downloadFile(Long fileId) throws Exception;

    String getFilePreviewUrl(Long fileId);

    String getStorageType();
}
