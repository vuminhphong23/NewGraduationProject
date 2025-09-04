package GraduationProject.forumikaa.service;

import GraduationProject.forumikaa.dto.FileUploadResponse;
import GraduationProject.forumikaa.patterns.strategy.FileStorageStrategy;
import GraduationProject.forumikaa.patterns.strategy.FileStorageStrategyFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
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
    public FileUploadResponse uploadFile(MultipartFile file, Long postId, Long userId) throws IOException {
        try {
            FileStorageStrategy strategy = strategyFactory.getStorageStrategy();
            return strategy.uploadFile(file, postId, userId);
        } catch (Exception e) {
            throw new IOException("File upload failed: " + e.getMessage(), e);
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
    public void deleteFile(Long fileId, Long userId) {
        FileStorageStrategy strategy = strategyFactory.getStorageStrategy();
        strategy.deleteFile(fileId, userId);
    }

    @Override
    public byte[] downloadFile(Long fileId) throws IOException {
        try {
            FileStorageStrategy strategy = strategyFactory.getStorageStrategy();
            return strategy.downloadFile(fileId);
        } catch (Exception e) {
            throw new IOException("File download failed: " + e.getMessage(), e);
        }
    }

    @Override
    public String getFilePreviewUrl(Long fileId) {
        FileStorageStrategy strategy = strategyFactory.getStorageStrategy();
        return strategy.getFilePreviewUrl(fileId);
    }

    @Override
    public byte[] downloadAllFilesAsZip(Long postId, Long userId) throws IOException {
        try {
            // Get all files for the post
            List<FileUploadResponse> files = getFilesByPostId(postId);
            
            if (files.isEmpty()) {
                throw new IOException("No files found for post: " + postId);
            }
            
            // Create ZIP file in memory
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (ZipOutputStream zos = new ZipOutputStream(baos)) {
                
                for (FileUploadResponse file : files) {
                    try {
                        // Download file content
                        byte[] fileContent = downloadFile(file.getId());
                        
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
            
            return baos.toByteArray();
            
        } catch (Exception e) {
            throw new IOException("Failed to create ZIP file: " + e.getMessage(), e);
        }
    }
}
