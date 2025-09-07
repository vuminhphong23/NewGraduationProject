package GraduationProject.forumikaa.service;

import GraduationProject.forumikaa.dto.FileUploadResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface ChatAttachmentService {
    
    FileUploadResponse uploadChatAttachment(MultipartFile file, Long messageId, Long roomId, Long userId) throws IOException;
    
    List<FileUploadResponse> uploadMultipleChatAttachments(MultipartFile[] files, Long messageId, Long roomId, Long userId) throws IOException;
    
    List<FileUploadResponse> getChatRoomAttachments(Long roomId);
    
    FileUploadResponse getChatAttachmentById(Long attachmentId);
    
    void deleteChatAttachment(Long attachmentId, Long userId);
    
    byte[] downloadChatAttachment(Long attachmentId) throws IOException;
    
    String getChatAttachmentPreviewUrl(Long attachmentId);
    
    List<FileUploadResponse> getChatRoomMedia(Long roomId);
    
    List<FileUploadResponse> getChatRoomImages(Long roomId);
    
    List<FileUploadResponse> getChatRoomVideos(Long roomId);
    
    List<FileUploadResponse> getChatRoomAudios(Long roomId);
    
    List<FileUploadResponse> getChatRoomDocuments(Long roomId);
    
    List<FileUploadResponse> getAttachmentsByMessageId(Long messageId);
}
