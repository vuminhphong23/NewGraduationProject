package GraduationProject.forumikaa.service;

import GraduationProject.forumikaa.dto.FileUploadResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface FileUploadService {
    
    CompletableFuture<FileUploadResponse> uploadFile(MultipartFile file, Long postId, Long userId);
    
    List<FileUploadResponse> getFilesByPostId(Long postId);
    
    FileUploadResponse getFileById(Long fileId);
    
    CompletableFuture<Void> deleteFile(Long fileId, Long userId);
    
    CompletableFuture<byte[]> downloadFile(Long fileId);
    
    String getFilePreviewUrl(Long fileId);
    
    CompletableFuture<byte[]> downloadAllFilesAsZip(Long postId, Long userId);
}
