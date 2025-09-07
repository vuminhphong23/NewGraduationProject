package GraduationProject.forumikaa.patterns.strategy;

import GraduationProject.forumikaa.dto.FileUploadResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * Strategy interface for chat file storage operations
 * Includes methods for file management sidebar functionality
 */
public interface ChatFileStorageStrategy {
    
    /**
     * Upload a chat attachment
     */
    FileUploadResponse uploadChatAttachment(MultipartFile file, Long messageId, Long roomId, Long userId) throws IOException;
    
    /**
     * Get a chat attachment by ID
     */
    FileUploadResponse getChatAttachmentById(Long attachmentId);
    
    /**
     * Delete a chat attachment
     */
    void deleteChatAttachment(Long attachmentId, Long userId);
    
    /**
     * Download a chat attachment
     */
    byte[] downloadChatAttachment(Long attachmentId) throws IOException;
    
    /**
     * Get preview URL for a chat attachment
     */
    String getChatAttachmentPreviewUrl(Long attachmentId);
    
    /**
     * Get all attachments for a chat room (for file management sidebar)
     */
    List<FileUploadResponse> getChatRoomAttachments(Long roomId);
    
    /**
     * Get all media files for a chat room (images + videos + audios)
     */
    List<FileUploadResponse> getChatRoomMedia(Long roomId);
    
    /**
     * Get images for a chat room (for "File phương tiện" tab)
     */
    List<FileUploadResponse> getChatRoomImages(Long roomId);
    
    /**
     * Get videos for a chat room (for "File phương tiện" tab)
     */
    List<FileUploadResponse> getChatRoomVideos(Long roomId);
    
    /**
     * Get audios for a chat room (for "File phương tiện" tab)
     */
    List<FileUploadResponse> getChatRoomAudios(Long roomId);
    
    /**
     * Get documents for a chat room (for "File" tab)
     */
    List<FileUploadResponse> getChatRoomDocuments(Long roomId);
    
    /**
     * Get attachments by message ID
     */
    List<FileUploadResponse> getAttachmentsByMessageId(Long messageId);
}
