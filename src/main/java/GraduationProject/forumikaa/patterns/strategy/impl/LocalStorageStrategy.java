package GraduationProject.forumikaa.patterns.strategy.impl;

import GraduationProject.forumikaa.dao.DocumentDao;
import GraduationProject.forumikaa.dao.PostDao;
import GraduationProject.forumikaa.dao.UserDao;
import GraduationProject.forumikaa.dto.FileUploadResponse;
import GraduationProject.forumikaa.entity.Document;
import GraduationProject.forumikaa.entity.Post;
import GraduationProject.forumikaa.entity.User;
import GraduationProject.forumikaa.patterns.strategy.FileStorageStrategy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.concurrent.CompletableFuture;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Local file storage strategy implementation
 * Stores files on local filesystem with organized directory structure
 */
@Component("localStorageStrategy")
public class LocalStorageStrategy implements FileStorageStrategy {

    @Value("${app.file.upload.local.path:uploads}")
    private String uploadPath;

    @Value("${app.file.upload.local.url-prefix:/files}")
    private String urlPrefix;

    private final DocumentDao documentDao;
    private final PostDao postDao;
    private final UserDao userDao;

    public LocalStorageStrategy(DocumentDao documentDao, PostDao postDao, UserDao userDao) {
        this.documentDao = documentDao;
        this.postDao = postDao;
        this.userDao = userDao;
    }

    @Override
    public FileUploadResponse uploadFile(MultipartFile file, Long postId, Long userId) throws Exception {
        // Synchronous fallback
        return uploadFileSync(file, postId, userId);
    }

    @Async("fileUploadExecutor")
    public CompletableFuture<FileUploadResponse> uploadFileAsync(MultipartFile file, Long postId, Long userId) {
        try {
            FileUploadResponse response = uploadFileSync(file, postId, userId);
            return CompletableFuture.completedFuture(response);
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    @Async("fileBatchExecutor")
    public CompletableFuture<List<FileUploadResponse>> uploadMultipleFilesAsync(List<MultipartFile> files, Long postId, Long userId) {
        try {
            List<CompletableFuture<FileUploadResponse>> uploadFutures = files.stream()
                .map(file -> uploadFileAsync(file, postId, userId))
                .collect(Collectors.toList());

            // Wait for all uploads to complete
            CompletableFuture<Void> allUploads = CompletableFuture.allOf(
                uploadFutures.toArray(new CompletableFuture[0])
            );

            return allUploads.thenApply(v -> 
                uploadFutures.stream()
                    .map(CompletableFuture::join)
                    .toList()
            );
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    private FileUploadResponse uploadFileSync(MultipartFile file, Long postId, Long userId) throws Exception {
        // Validate file
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File không được để trống");
        }

        // Get post and user
        Post post = postDao.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bài viết"));
        User user = userDao.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));

        // Create upload directory using NIO
        Path uploadDir = createUploadDirectory();
        
        // Generate unique filename
        String originalName = file.getOriginalFilename();
        String fileExtension = getFileExtension(originalName);
        String uniqueFileName = UUID.randomUUID().toString() + fileExtension;
        
        // Create year/month subdirectories
        String yearMonth = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
        Path yearMonthDir = createYearMonthDirectory(uploadDir, yearMonth);

        // Save file using NIO with atomic operation
        Path filePath = yearMonthDir.resolve(uniqueFileName);
        saveFileWithNIO(file, filePath);

        // Create and save document
        Document document = createDocument(file, post, user, uniqueFileName, originalName, 
                                        yearMonth, fileExtension);
        
        Document savedDocument = documentDao.save(document);
        return createFileUploadResponse(savedDocument);
    }

    @Override
    public List<FileUploadResponse> getFilesByPostId(Long postId) {
        try {
            List<Document> documents = documentDao.findByPostId(postId);
            
            if (documents == null || documents.isEmpty()) {
                return new ArrayList<>();
            }
            
            return documents.stream()
                    .map(this::createFileUploadResponse)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Error in getFilesByPostId for postId " + postId + ": " + e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public FileUploadResponse getFileById(Long fileId) {
        Document document = documentDao.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy file"));
        return createFileUploadResponse(document);
    }

    @Override
    public void deleteFile(Long fileId, Long userId) {
        Document document = documentDao.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy file"));

        if (!document.getUser().getId().equals(userId) && !document.getPost().getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Không có quyền xóa file này");
        }

        // Delete physical file
        try {
            Path filePath = getFilePathFromDocument(document);
            if (Files.exists(filePath)) {
                Files.deleteIfExists(filePath);
            }
        } catch (IOException e) {
            System.err.println("Failed to delete physical file: " + e.getMessage());
        }

        // Delete from database
        documentDao.delete(document);
    }


    @Override
    public byte[] downloadFile(Long fileId) throws Exception {
        Document document = documentDao.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy file"));

        Path filePath = getFilePathFromDocument(document);
        
        // Validate file exists and is readable
        if (!Files.exists(filePath)) {
            throw new FileNotFoundException("File không tồn tại trên server: " + filePath);
        }
        
        if (!Files.isReadable(filePath)) {
            throw new IOException("File không thể đọc được: " + filePath);
        }

        // Read file using NIO
        return Files.readAllBytes(filePath);
    }

    @Override
    public String getFilePreviewUrl(Long fileId) {
        Document document = documentDao.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy file"));
        return urlPrefix + "/" + document.getFilePath();
    }

    @Override
    public String getStorageType() {
        return "async-local";
    }

    // ========== Helper Methods ==========

    private Path createUploadDirectory() throws IOException {
        Path uploadDir = Paths.get(System.getProperty("user.dir"), uploadPath);
        
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }
        
        return uploadDir;
    }

    private Path createYearMonthDirectory(Path uploadDir, String yearMonth) throws IOException {
        Path yearMonthDir = uploadDir.resolve(yearMonth);
        
        if (!Files.exists(yearMonthDir)) {
            Files.createDirectories(yearMonthDir);
        }
        
        return yearMonthDir;
    }

    private void saveFileWithNIO(MultipartFile file, Path filePath) throws IOException {
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        
        // Verify file was saved
        if (!Files.exists(filePath)) {
            throw new IOException("File không được lưu thành công: " + filePath);
        }
        
        // Get file size for verification
        long savedSize = Files.size(filePath);
        if (savedSize != file.getSize()) {
            throw new IOException("File size mismatch. Expected: " + file.getSize() + ", Actual: " + savedSize);
        }
    }

    private Document createDocument(MultipartFile file, Post post, User user, 
                                 String uniqueFileName, String originalName, 
                                 String yearMonth, String fileExtension) {
        Document document = new Document();
        document.setFileName(uniqueFileName);
        document.setOriginalName(originalName);
        document.setFilePath(yearMonth + "/" + uniqueFileName);
        document.setFileSize(file.getSize());
        document.setMimeType(file.getContentType());
        document.setFileExtension(fileExtension);
        document.setPost(post);
        document.setUser(user);
        document.setUploadedAt(LocalDateTime.now());
        
        return document;
    }

    private Path getFilePathFromDocument(Document document) {
        return Paths.get(System.getProperty("user.dir"), uploadPath, document.getFilePath());
    }

    private FileUploadResponse createFileUploadResponse(Document document) {
        FileUploadResponse response = new FileUploadResponse();
        response.setId(document.getId());
        response.setFileName(document.getFileName());
        response.setOriginalName(document.getOriginalName());
        response.setFilePath(document.getFilePath());
        response.setFileSize(document.getFileSize());
        response.setMimeType(document.getMimeType());
        response.setDownloadUrl("/api/files/download/" + document.getId());
        response.setPreviewUrl(urlPrefix + "/" + document.getFilePath());
        response.setFileType(getFileType(document.getMimeType()));
        return response;
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.lastIndexOf(".") == -1) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf("."));
    }

    private String getFileType(String mimeType) {
        if (mimeType == null) return "unknown";
        if (mimeType.startsWith("image/")) return "image";
        if (mimeType.startsWith("video/")) return "video";
        if (mimeType.startsWith("application/") || mimeType.startsWith("text/")) return "document";
        return "other";
    }
}
