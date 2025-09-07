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
import java.io.InputStream;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Local file storage strategy implementation for Chat files
 * Stores files on local filesystem with organized directory structure
 */
@Component
public class ChatLocalStorageStrategy implements ChatFileStorageStrategy {

    @Value("${app.file.upload.local.path:uploads}")
    private String uploadPath;

    @Value("${app.file.upload.local.url-prefix:/files}")
    private String urlPrefix;

    private final ChatAttachmentRepository chatAttachmentRepository;
    private final ChatRoomDao chatRoomDao;
    private final UserDao userDao;

    public ChatLocalStorageStrategy(ChatAttachmentRepository chatAttachmentRepository, ChatRoomDao chatRoomDao, UserDao userDao) {
        this.chatAttachmentRepository = chatAttachmentRepository;
        this.chatRoomDao = chatRoomDao;
        this.userDao = userDao;
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

        // Create and save chat attachment
        ChatAttachment attachment = createChatAttachment(file, message, room, user, uniqueFileName, originalName, 
                                        yearMonth, fileExtension);
        
        ChatAttachment savedAttachment = chatAttachmentRepository.save(attachment);
        return createFileUploadResponse(savedAttachment);
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
            // Delete file from filesystem
            Path filePath = Paths.get(attachment.getFilePath());
            if (Files.exists(filePath)) {
                Files.delete(filePath);
            }
        } catch (IOException e) {
            // Log error but continue with database deletion
        }

        // Delete from database
        chatAttachmentRepository.delete(attachment);
    }




    // ========== Helper Methods ==========

    private Path createUploadDirectory() throws IOException {
        Path uploadDir = Paths.get(uploadPath);
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
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private ChatAttachment createChatAttachment(MultipartFile file, ChatMessage message, ChatRoom room, User user, 
                                       String uniqueFileName, String originalName, 
                                       String yearMonth, String fileExtension) {
        ChatAttachment attachment = ChatAttachment.builder()
                .fileName(uniqueFileName)
                .originalName(originalName)
                .filePath(yearMonth + "/" + uniqueFileName)
                .fileSize(file.getSize())
                .mimeType(file.getContentType())
                .fileExtension(fileExtension)
                .attachmentType(determineAttachmentType(file.getContentType()))
                .isCloudStorage(false)
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
        response.setFilePath(attachment.getFilePath());
        response.setFileSize(attachment.getFileSize());
        response.setMimeType(attachment.getMimeType());
        response.setDownloadUrl(urlPrefix + "/" + attachment.getFilePath().replace("\\", "/"));
        response.setPreviewUrl(urlPrefix + "/" + attachment.getFilePath().replace("\\", "/"));
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
        
        Path filePath = Paths.get(uploadPath, attachment.getFilePath());
        if (!Files.exists(filePath)) {
            throw new IOException("File không tồn tại: " + filePath);
        }
        
        return Files.readAllBytes(filePath);
    }

    @Override
    public String getChatAttachmentPreviewUrl(Long attachmentId) {
        ChatAttachment attachment = chatAttachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy attachment"));
        return attachment.getDisplayUrl() != null ? attachment.getDisplayUrl() : urlPrefix + "/" + attachment.getFilePath().replace("\\", "/");
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
