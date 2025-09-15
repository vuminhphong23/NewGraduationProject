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
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Cloudinary file storage strategy implementation
 * Stores files on Cloudinary cloud storage with automatic optimization
 */
@Component("cloudinaryStorageStrategy")
public class CloudinaryStorageStrategy implements FileStorageStrategy {

    @Value("${cloudinary.cloud-name}")
    private String cloudName;

    @Value("${cloudinary.api-key}")
    private String apiKey;

    @Value("${cloudinary.api-secret}")
    private String apiSecret;

    @Value("${cloudinary.upload-folder:forumikaa/files}")
    private String uploadFolder;

    private final DocumentDao documentDao;
    private final PostDao postDao;
    private final UserDao userDao;
    private final HttpClient httpClient;

    public CloudinaryStorageStrategy(DocumentDao documentDao, PostDao postDao, UserDao userDao) {
        this.documentDao = documentDao;
        this.postDao = postDao;
        this.userDao = userDao;
        this.httpClient = HttpClient.newHttpClient();
    }

    @Override
    public FileUploadResponse uploadFile(MultipartFile file, Long postId, Long userId) throws Exception {
        // Validate file
        if (file.isEmpty() || file.getSize() == 0) {
            throw new IllegalArgumentException("File không được để trống hoặc có kích thước 0 bytes");
        }

        // Get post and user
        Post post = postDao.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bài viết"));
        User user = userDao.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));

        try {
            // Upload to Cloudinary using HTTP API
            String uploadResult = uploadToCloudinary(file);
            
            // Parse response to get URL and public_id from Cloudinary response
            String secureUrl = extractUrlFromResponse(uploadResult);
            String publicId = extractPublicIdFromResponse(uploadResult);
            String originalName = file.getOriginalFilename();
            String fileExtension = getFileExtension(originalName);

            // Create and save document
            Document document = createDocument(file, post, user, publicId, originalName, 
                                            secureUrl, fileExtension);
            
            Document savedDocument = documentDao.save(document);
            return createFileUploadResponse(savedDocument);

        } catch (Exception e) {
            throw new RuntimeException("Cloudinary upload failed: " + e.getMessage(), e);
        }
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

        try {
            // Delete from Cloudinary using the correct public_id (fileName field)
            deleteFromCloudinary(document.getFileName());
        } catch (Exception e) {
            System.err.println("Failed to delete file from Cloudinary: " + e.getMessage());
        }

        // Delete from database
        documentDao.delete(document);
    }


    @Override
    public byte[] downloadFile(Long fileId) throws Exception {
        Document document = documentDao.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy file"));

        try {
            // Download file content from Cloudinary URL
            String cloudinaryUrl = document.getFilePath();
            System.out.println("Downloading Cloudinary file: " + cloudinaryUrl);
            
            // Use Java's built-in HTTP client to download the file
            java.net.URL url = new java.net.URL(cloudinaryUrl);
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000); // 10 seconds
            connection.setReadTimeout(30000); // 30 seconds
            
            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                throw new IOException("Failed to download file from Cloudinary. Response code: " + responseCode);
            }
            
            // Read the file content
            try (java.io.InputStream inputStream = connection.getInputStream();
                 java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream()) {
                
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                
                byte[] fileContent = outputStream.toByteArray();
                System.out.println("Successfully downloaded Cloudinary file: " + document.getOriginalName() + " (" + fileContent.length + " bytes)");
                return fileContent;
            }
            
        } catch (Exception e) {
            System.err.println("Error downloading Cloudinary file " + document.getOriginalName() + ": " + e.getMessage());
            throw new IOException("Failed to download file from Cloudinary: " + e.getMessage(), e);
        }
    }

    @Override
    public String getFilePreviewUrl(Long fileId) {
        Document document = documentDao.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy file"));
        return document.getFilePath(); // This contains the Cloudinary URL
    }

    @Override
    public String getStorageType() {
        return "cloudinary";
    }

    // ========== Helper Methods ==========

    private String uploadToCloudinary(MultipartFile file) throws IOException, InterruptedException {
        String uploadUrl = "https://api.cloudinary.com/v1_1/" + cloudName + "/upload";
        
        // Create proper multipart form data with binary file content
        String boundary = "----WebKitFormBoundary" + UUID.randomUUID().toString();
        
        // Build multipart body
        byte[] fileBytes = file.getBytes();
        byte[] boundaryBytes = boundary.getBytes(StandardCharsets.UTF_8);
        byte[] newlineBytes = "\r\n".getBytes(StandardCharsets.UTF_8);
        
        // Calculate total size
        int totalSize = 0;
        totalSize += ("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8).length;
        totalSize += ("Content-Disposition: form-data; name=\"file\"; filename=\"" + file.getOriginalFilename() + "\"\r\n").getBytes(StandardCharsets.UTF_8).length;
        totalSize += ("Content-Type: " + file.getContentType() + "\r\n\r\n").getBytes(StandardCharsets.UTF_8).length;
        totalSize += fileBytes.length;
        totalSize += newlineBytes.length;
        totalSize += ("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8).length;
        totalSize += ("Content-Disposition: form-data; name=\"upload_preset\"\r\n\r\n").getBytes(StandardCharsets.UTF_8).length;
        totalSize += "forumikaa-documents".getBytes(StandardCharsets.UTF_8).length;
        totalSize += newlineBytes.length;
        totalSize += ("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8).length;
        
        // Build the complete multipart body
        byte[] multipartBody = new byte[totalSize];
        int offset = 0;
        
        // Add file field header
        byte[] fileHeader = ("--" + boundary + "\r\n" +
                "Content-Disposition: form-data; name=\"file\"; filename=\"" + file.getOriginalFilename() + "\"\r\n" +
                "Content-Type: " + file.getContentType() + "\r\n\r\n").getBytes(StandardCharsets.UTF_8);
        System.arraycopy(fileHeader, 0, multipartBody, offset, fileHeader.length);
        offset += fileHeader.length;
        
        // Add file content
        System.arraycopy(fileBytes, 0, multipartBody, offset, fileBytes.length);
        offset += fileBytes.length;
        
        // Add newline after file
        System.arraycopy(newlineBytes, 0, multipartBody, offset, newlineBytes.length);
        offset += newlineBytes.length;
        
        // Add upload preset field
        byte[] presetField = ("--" + boundary + "\r\n" +
                "Content-Disposition: form-data; name=\"upload_preset\"\r\n\r\n" +
                "forumikaa-documents\r\n").getBytes(StandardCharsets.UTF_8);
        System.arraycopy(presetField, 0, multipartBody, offset, presetField.length);
        offset += presetField.length;
        
        // Add closing boundary
        byte[] closingBoundary = ("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8);
        System.arraycopy(closingBoundary, 0, multipartBody, offset, closingBoundary.length);
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uploadUrl))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(multipartBody))
                .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        System.out.println("Cloudinary upload response status: " + response.statusCode());
        System.out.println("Cloudinary upload response body: " + response.body());
        
        if (response.statusCode() != 200) {
            throw new IOException("Cloudinary upload failed: " + response.body());
        }
        
        return response.body();
    }
    
    private String extractUrlFromResponse(String response) {
        // Simplified URL extraction - in real implementation, parse JSON properly
        // This is a placeholder - you should use proper JSON parsing
        if (response.contains("\"secure_url\"")) {
            int start = response.indexOf("\"secure_url\":\"") + 14;
            int end = response.indexOf("\"", start);
            return response.substring(start, end);
        }
        return "https://res.cloudinary.com/" + cloudName + "/image/upload/v1/placeholder.jpg";
    }
    
    private String extractPublicIdFromResponse(String response) {
        // Parse JSON response to extract public_id
        // Response format: {"public_id":"actual_public_id","secure_url":"https://..."}
        if (response.contains("\"public_id\"")) {
            int start = response.indexOf("\"public_id\":\"") + 13;
            int end = response.indexOf("\"", start);
            return response.substring(start, end);
        }
        // Fallback: generate UUID if parsing fails
        return UUID.randomUUID().toString();
    }
    
    
    private void deleteFromCloudinary(String publicId) throws IOException, InterruptedException {
        // Simplified delete - in real implementation, use proper Cloudinary delete API
        String deleteUrl = "https://api.cloudinary.com/v1_1/" + cloudName + "/image/destroy";
        
        String formData = "public_id=" + publicId + "&api_key=" + apiKey + "&api_secret=" + apiSecret;
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(deleteUrl))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(formData))
                .build();
        
        httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private Document createDocument(MultipartFile file, Post post, User user, 
                                 String publicId, String originalName, 
                                 String cloudinaryUrl, String fileExtension) {
        Document document = new Document();
        document.setFileName(publicId); // Store public_id as fileName
        document.setOriginalName(originalName);
        document.setFilePath(cloudinaryUrl); // Store Cloudinary URL as filePath
        document.setFileSize(file.getSize());
        document.setMimeType(file.getContentType());
        document.setFileExtension(fileExtension);
        document.setPost(post);
        document.setUser(user);
        document.setUploadedAt(LocalDateTime.now());
        
        return document;
    }

    private FileUploadResponse createFileUploadResponse(Document document) {
        FileUploadResponse response = new FileUploadResponse();
        response.setId(document.getId());
        response.setFileName(document.getFileName());
        response.setOriginalName(document.getOriginalName());
        response.setFilePath(document.getFilePath());
        response.setFileSize(document.getFileSize());
        response.setMimeType(document.getMimeType());
        response.setDownloadUrl(document.getFilePath()); // Cloudinary URL for download
        response.setPreviewUrl(document.getFilePath()); // Cloudinary URL for preview
        
        response.setFileType(getFileType(document.getMimeType()));
        response.setCloudStorage(true); // Mark as cloud storage
        
        // Set downloadCount và uploadDate từ database
        response.setDownloadCount(document.getDownloadCount() != null ? document.getDownloadCount() : 0);
        response.setUploadDate(document.getUploadedAt().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        
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
        if (mimeType.startsWith("text/")) return "text";
        
        // Specific application types
        if (mimeType.equals("application/pdf")) return "pdf";
        if (mimeType.equals("application/msword") || mimeType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document")) return "doc";
        if (mimeType.equals("application/vnd.ms-excel") || mimeType.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) return "xls";
        if (mimeType.equals("application/vnd.ms-powerpoint") || mimeType.equals("application/vnd.openxmlformats-officedocument.presentationml.presentation")) return "ppt";
        
        // Generic document for other application types
        if (mimeType.startsWith("application/")) return "document";
        return "other";
    }
}
