package GraduationProject.forumikaa.patterns.strategy.impl;

import GraduationProject.forumikaa.dao.ChatRoomDao;
import GraduationProject.forumikaa.dao.ChatAttachmentRepository;
import GraduationProject.forumikaa.dao.UserDao;
import GraduationProject.forumikaa.dto.FileUploadResponse;
import GraduationProject.forumikaa.entity.ChatRoom;
import GraduationProject.forumikaa.entity.ChatAttachment;
import GraduationProject.forumikaa.entity.ChatMessage;
import GraduationProject.forumikaa.entity.User;
import GraduationProject.forumikaa.patterns.strategy.ChatFileStorageStrategy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Cloudinary file storage strategy implementation for Chat files
 * Stores files on Cloudinary cloud storage with automatic optimization
 */
@Component
public class ChatCloudinaryStorageStrategy implements ChatFileStorageStrategy {

    @Value("${cloudinary.cloud-name:dsqkymrkm}")
    private String cloudName;

    @Value("${cloudinary.api-key:769412498641216}")
    private String apiKey;

    @Value("${cloudinary.api-secret:iCRSwCZ8p3j7NzIUm8pb3itcMB4}")
    private String apiSecret;

    @Value("${cloudinary.upload-folder:forumikaa/documents}")
    private String uploadFolder;

    @Value("${cloudinary.chat-upload-preset:forumikaa-chats}")
    private String chatUploadPreset;

    private final ChatAttachmentRepository chatAttachmentRepository;
    private final ChatRoomDao chatRoomDao;
    private final UserDao userDao;
    private final HttpClient httpClient;

    public ChatCloudinaryStorageStrategy(ChatAttachmentRepository chatAttachmentRepository, ChatRoomDao chatRoomDao, UserDao userDao) {
        this.chatAttachmentRepository = chatAttachmentRepository;
        this.chatRoomDao = chatRoomDao;
        this.userDao = userDao;
        this.httpClient = HttpClient.newHttpClient();
        System.out.println("ChatCloudinaryStorageStrategy: Initialized successfully");
    }

    @Override
    public FileUploadResponse uploadChatAttachment(MultipartFile file, Long messageId, Long roomId, Long userId) throws IOException {
        // Validate file
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File không được để trống");
        }

        // Get room, user, and message
        ChatRoom room = chatRoomDao.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy chat room"));
        User user = userDao.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));
        ChatMessage message = new ChatMessage();
        message.setId(messageId);

        try {
            // Upload to Cloudinary using HTTP API
            System.out.println("ChatCloudinaryStorageStrategy: Starting upload to Cloudinary");
            String uploadResult = uploadToCloudinary(file);
            System.out.println("ChatCloudinaryStorageStrategy: Upload result: " + uploadResult);
            
            // Parse response to get URL and public_id from Cloudinary response
            String secureUrl = extractUrlFromResponse(uploadResult);
            String publicId = extractPublicIdFromResponse(uploadResult);
            String originalName = file.getOriginalFilename();
            String fileExtension = getFileExtension(originalName);
            
            System.out.println("ChatCloudinaryStorageStrategy: Parsed URL: " + secureUrl);
            System.out.println("ChatCloudinaryStorageStrategy: Parsed publicId: " + publicId);

            // Create and save chat attachment
            ChatAttachment attachment = createChatAttachment(file, message, room, user, publicId, originalName, 
                                            secureUrl, fileExtension);
            
            System.out.println("ChatCloudinaryStorageStrategy: Saving attachment to database");
            ChatAttachment savedAttachment = chatAttachmentRepository.save(attachment);
            System.out.println("ChatCloudinaryStorageStrategy: Attachment saved with ID: " + savedAttachment.getId());
            return createFileUploadResponse(savedAttachment);

        } catch (Exception e) {
            throw new RuntimeException("Cloudinary upload failed: " + e.getMessage(), e);
        }
    }



    @Override
    public void deleteChatAttachment(Long attachmentId, Long userId) {
        ChatAttachment attachment = chatAttachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy attachment"));

        // Check if user has permission to delete (owner)
        if (!attachment.getUploadedBy().getId().equals(userId)) {
            throw new IllegalArgumentException("Không có quyền xóa attachment này");
        }

        try {
            // Delete from Cloudinary using the correct public_id (fileName field)
            deleteFromCloudinary(attachment.getFileName());
        } catch (Exception e) {
            // Log error but continue with database deletion
        }

        // Delete from database
        chatAttachmentRepository.delete(attachment);
    }




    // ========== Helper Methods ==========

    private String uploadToCloudinary(MultipartFile file) throws IOException, InterruptedException {
        String uploadUrl = "https://api.cloudinary.com/v1_1/" + cloudName + "/upload";
        
        // Create proper multipart form data with binary file content
        String boundary = "----WebKitFormBoundary" + UUID.randomUUID().toString();
        
        // Build multipart body
        byte[] fileBytes = file.getBytes();
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
        totalSize += chatUploadPreset.getBytes(StandardCharsets.UTF_8).length;
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
                chatUploadPreset + "\r\n").getBytes(StandardCharsets.UTF_8);
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
        
        if (response.statusCode() != 200) {
            throw new IOException("Cloudinary upload failed: " + response.body());
        }
        
        return response.body();
    }
    
    private String extractUrlFromResponse(String response) {
        // Simplified URL extraction - in real implementation, parse JSON properly
        if (response.contains("\"secure_url\"")) {
            int start = response.indexOf("\"secure_url\":\"") + 14;
            int end = response.indexOf("\"", start);
            return response.substring(start, end);
        }
        return "https://res.cloudinary.com/" + cloudName + "/image/upload/v1/placeholder.jpg";
    }
    
    private String extractPublicIdFromResponse(String response) {
        // Parse JSON response to extract public_id
        if (response.contains("\"public_id\"")) {
            int start = response.indexOf("\"public_id\":\"") + 13;
            int end = response.indexOf("\"", start);
            return response.substring(start, end);
        }
        // Fallback: generate UUID if parsing fails
        return UUID.randomUUID().toString();
    }
    
    private void deleteFromCloudinary(String publicId) throws IOException, InterruptedException {
        String deleteUrl = "https://api.cloudinary.com/v1_1/" + cloudName + "/image/destroy";
        
        String formData = "public_id=" + publicId + "&api_key=" + apiKey + "&api_secret=" + apiSecret;
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(deleteUrl))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(formData))
                .build();
        
        httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private ChatAttachment createChatAttachment(MultipartFile file, ChatMessage message, ChatRoom room, User user, 
                                      String publicId, String originalName, 
                                      String cloudinaryUrl, String fileExtension) {
        ChatAttachment attachment = ChatAttachment.builder()
                .fileName(publicId) // Store public_id as fileName
                .originalName(originalName)
                .filePath(cloudinaryUrl) // Store Cloudinary URL as filePath
                .fileSize(file.getSize())
                .mimeType(file.getContentType())
                .fileExtension(fileExtension)
                .attachmentType(determineAttachmentType(file.getContentType()))
                .cloudinaryPublicId(publicId)
                .cloudinaryUrl(cloudinaryUrl)
                .isCloudStorage(true)
                .message(message)
                .room(room)
                .uploadedBy(user)
                .build();
        
        return attachment;
    }

    private FileUploadResponse createFileUploadResponse(ChatAttachment attachment) {
        FileUploadResponse response = new FileUploadResponse();
        response.setId(attachment.getId());
        response.setFileName(attachment.getFileName());
        response.setOriginalName(attachment.getOriginalName());
        response.setFilePath(attachment.getDisplayUrl());
        response.setFileSize(attachment.getFileSize());
        response.setMimeType(attachment.getMimeType());
        response.setDownloadUrl(attachment.getDisplayUrl()); // Cloudinary URL for download
        response.setPreviewUrl(attachment.getDisplayUrl()); // Cloudinary URL for preview
        response.setThumbnailUrl(attachment.getDisplayThumbnailUrl());
        response.setCloudinaryPublicId(attachment.getCloudinaryPublicId());
        response.setCloudinaryUrl(attachment.getCloudinaryUrl());
        response.setCloudStorage(attachment.isCloudStorage());
        response.setFileType(attachment.getAttachmentType().name().toLowerCase());
        return response;
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.lastIndexOf(".") == -1) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf("."));
    }


    private ChatAttachment.AttachmentType determineAttachmentType(String mimeType) {
        if (mimeType == null) {
            return ChatAttachment.AttachmentType.OTHER;
        }
        
        if (mimeType.startsWith("image/")) {
            return ChatAttachment.AttachmentType.IMAGE;
        } else if (mimeType.startsWith("video/")) {
            return ChatAttachment.AttachmentType.VIDEO;
        } else if (mimeType.startsWith("audio/")) {
            return ChatAttachment.AttachmentType.AUDIO;
        } else if (mimeType.contains("pdf") || mimeType.contains("document") || 
                   mimeType.contains("text") || mimeType.contains("application")) {
            return ChatAttachment.AttachmentType.DOCUMENT;
        } else {
            return ChatAttachment.AttachmentType.OTHER;
        }
    }


    @Override
    public FileUploadResponse getChatAttachmentById(Long attachmentId) {
        ChatAttachment attachment = chatAttachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy attachment"));
        return createFileUploadResponse(attachment);
    }


    @Override
    public byte[] downloadChatAttachment(Long attachmentId) throws IOException {
        ChatAttachment attachment = chatAttachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy attachment"));
        
        // For Cloudinary, we return the URL as bytes (client will handle download)
        String downloadUrl = attachment.getDisplayUrl() != null ? attachment.getDisplayUrl() : attachment.getCloudinaryUrl();
        return downloadUrl.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public String getChatAttachmentPreviewUrl(Long attachmentId) {
        ChatAttachment attachment = chatAttachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy attachment"));
        return attachment.getDisplayUrl() != null ? attachment.getDisplayUrl() : attachment.getCloudinaryUrl();
    }

    @Override
    public List<FileUploadResponse> getChatRoomAttachments(Long roomId) {
        List<ChatAttachment> attachments = chatAttachmentRepository.findByRoomIdOrderByUploadedAtDesc(roomId);
        return attachments.stream()
                .map(this::createFileUploadResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<FileUploadResponse> getChatRoomMedia(Long roomId) {
        // Get all media files (images, videos, audios)
        List<ChatAttachment> mediaFiles = chatAttachmentRepository.findImagesByRoomId(roomId);
        mediaFiles.addAll(chatAttachmentRepository.findVideosByRoomId(roomId));
        mediaFiles.addAll(chatAttachmentRepository.findAudiosByRoomId(roomId));
        
        return mediaFiles.stream()
                .map(this::createFileUploadResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<FileUploadResponse> getChatRoomImages(Long roomId) {
        List<ChatAttachment> images = chatAttachmentRepository.findImagesByRoomId(roomId);
        return images.stream()
                .map(this::createFileUploadResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<FileUploadResponse> getChatRoomVideos(Long roomId) {
        List<ChatAttachment> videos = chatAttachmentRepository.findVideosByRoomId(roomId);
        return videos.stream()
                .map(this::createFileUploadResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<FileUploadResponse> getChatRoomAudios(Long roomId) {
        List<ChatAttachment> audios = chatAttachmentRepository.findAudiosByRoomId(roomId);
        return audios.stream()
                .map(this::createFileUploadResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<FileUploadResponse> getChatRoomDocuments(Long roomId) {
        List<ChatAttachment> documents = chatAttachmentRepository.findDocumentsByRoomId(roomId);
        return documents.stream()
                .map(this::createFileUploadResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<FileUploadResponse> getAttachmentsByMessageId(Long messageId) {
        List<ChatAttachment> attachments = chatAttachmentRepository.findByMessageIdOrderByUploadedAtAsc(messageId);
        return attachments.stream()
                .map(this::createFileUploadResponse)
                .collect(Collectors.toList());
    }
}
