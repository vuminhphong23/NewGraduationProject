package GraduationProject.forumikaa.service;

import GraduationProject.forumikaa.dto.FileUploadResponse;
import GraduationProject.forumikaa.patterns.strategy.FileStorageStrategy;
import GraduationProject.forumikaa.patterns.strategy.FileStorageStrategyFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@Transactional
public class FileUploadServiceImpl implements FileUploadService {

    private final FileStorageStrategyFactory strategyFactory;

    @Autowired
    public FileUploadServiceImpl(FileStorageStrategyFactory strategyFactory) {
        this.strategyFactory = strategyFactory;
    }

    @Override
    @Async("fileUploadExecutor")
    public CompletableFuture<FileUploadResponse> uploadFile(MultipartFile file, Long postId, Long userId) {
        try {
            FileStorageStrategy strategy = strategyFactory.getStorageStrategy();
            FileUploadResponse response = strategy.uploadFile(file, postId, userId);
            return CompletableFuture.completedFuture(response);
        } catch (Exception e) {
            return CompletableFuture.failedFuture(new IOException("File upload failed: " + e.getMessage(), e));
        }
    }

    @Override
    public List<FileUploadResponse> getFilesByPostId(Long postId) {
        FileStorageStrategy strategy = strategyFactory.getStorageStrategy();
        return strategy.getFilesByPostId(postId);
    }

    @Override
    public FileUploadResponse getFileById(Long fileId) {
        FileStorageStrategy strategy = strategyFactory.getStorageStrategy();
        return strategy.getFileById(fileId);
    }

    @Override
    @Async("fileUploadExecutor")
    public CompletableFuture<Void> deleteFile(Long fileId, Long userId) {
        try {
            FileStorageStrategy strategy = strategyFactory.getStorageStrategy();
            strategy.deleteFile(fileId, userId);
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    @Override
    @Async("fileUploadExecutor")
    public CompletableFuture<byte[]> downloadFile(Long fileId) {
        try {
            FileStorageStrategy strategy = strategyFactory.getStorageStrategy();
            byte[] content = strategy.downloadFile(fileId);
            return CompletableFuture.completedFuture(content);
        } catch (Exception e) {
            return CompletableFuture.failedFuture(new IOException("File download failed: " + e.getMessage(), e));
        }
    }

    @Override
    public String getFilePreviewUrl(Long fileId) {
        FileStorageStrategy strategy = strategyFactory.getStorageStrategy();
        return strategy.getFilePreviewUrl(fileId);
    }

    @Override
    @Async("fileBatchExecutor")
    public CompletableFuture<byte[]> downloadAllFilesAsZip(Long postId, Long userId) {
        try {
            // Get all files for the post
            List<FileUploadResponse> files = getFilesByPostId(postId);
            
            if (files.isEmpty()) {
                return CompletableFuture.failedFuture(new IOException("No files found for post: " + postId));
            }
            
            // Create ZIP file in memory
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (ZipOutputStream zos = new ZipOutputStream(baos)) {
                
                for (FileUploadResponse file : files) {
                    try {
                        // Download file content
                        byte[] fileContent = strategyFactory.getStorageStrategy().downloadFile(file.getId());
                        
                        // Create ZIP entry
                        ZipEntry zipEntry = new ZipEntry(file.getOriginalName());
                        zos.putNextEntry(zipEntry);
                        zos.write(fileContent);
                        zos.closeEntry();
                        
                    } catch (Exception e) {
                        // Log error but continue with other files
                        System.err.println("Error adding file to ZIP: " + file.getOriginalName() + " - " + e.getMessage());
                    }
                }
            }
            
            return CompletableFuture.completedFuture(baos.toByteArray());
            
        } catch (Exception e) {
            return CompletableFuture.failedFuture(new IOException("Failed to create ZIP file: " + e.getMessage(), e));
        }
    }

}
