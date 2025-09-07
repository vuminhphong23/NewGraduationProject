package GraduationProject.forumikaa.service;

import GraduationProject.forumikaa.dto.FileUploadResponse;
import GraduationProject.forumikaa.patterns.strategy.ChatFileStorageStrategy;
import GraduationProject.forumikaa.patterns.strategy.ChatFileStorageStrategyFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class ChatAttachmentServiceImpl implements ChatAttachmentService {

    private final ChatFileStorageStrategyFactory chatFileStorageStrategyFactory;

    @Autowired
    public ChatAttachmentServiceImpl(ChatFileStorageStrategyFactory chatFileStorageStrategyFactory) {
        this.chatFileStorageStrategyFactory = chatFileStorageStrategyFactory;
    }

    @Override
    public FileUploadResponse uploadChatAttachment(MultipartFile file, Long messageId, Long roomId, Long userId) throws IOException {
        try {
            ChatFileStorageStrategy strategy = chatFileStorageStrategyFactory.getStorageStrategy();
            return strategy.uploadChatAttachment(file, messageId, roomId, userId);
        } catch (Exception e) {
            throw new IOException("Chat attachment upload failed: " + e.getMessage(), e);
        }
    }

    @Override
    public List<FileUploadResponse> uploadMultipleChatAttachments(MultipartFile[] files, Long messageId, Long roomId, Long userId) throws IOException {
        List<FileUploadResponse> responses = new ArrayList<>();
        
        for (MultipartFile file : files) {
            try {
                FileUploadResponse response = uploadChatAttachment(file, messageId, roomId, userId);
                responses.add(response);
            } catch (Exception e) {
                System.err.println("ChatAttachmentService: Failed to upload file: " + file.getOriginalFilename() + ", Error: " + e.getMessage());
                // Log error but continue with other files
            }
        }
        
        return responses;
    }

    @Override
    public List<FileUploadResponse> getChatRoomAttachments(Long roomId) {
        ChatFileStorageStrategy strategy = chatFileStorageStrategyFactory.getStorageStrategy();
        return strategy.getChatRoomAttachments(roomId);
    }

    @Override
    public FileUploadResponse getChatAttachmentById(Long attachmentId) {
        ChatFileStorageStrategy strategy = chatFileStorageStrategyFactory.getStorageStrategy();
        return strategy.getChatAttachmentById(attachmentId);
    }

    @Override
    public void deleteChatAttachment(Long attachmentId, Long userId) {
        ChatFileStorageStrategy strategy = chatFileStorageStrategyFactory.getStorageStrategy();
        strategy.deleteChatAttachment(attachmentId, userId);
    }

    @Override
    public byte[] downloadChatAttachment(Long attachmentId) throws IOException {
        try {
            ChatFileStorageStrategy strategy = chatFileStorageStrategyFactory.getStorageStrategy();
            return strategy.downloadChatAttachment(attachmentId);
        } catch (Exception e) {
            throw new IOException("Chat attachment download failed: " + e.getMessage(), e);
        }
    }

    @Override
    public String getChatAttachmentPreviewUrl(Long attachmentId) {
        ChatFileStorageStrategy strategy = chatFileStorageStrategyFactory.getStorageStrategy();
        return strategy.getChatAttachmentPreviewUrl(attachmentId);
    }

    @Override
    public List<FileUploadResponse> getChatRoomMedia(Long roomId) {
        ChatFileStorageStrategy strategy = chatFileStorageStrategyFactory.getStorageStrategy();
        return strategy.getChatRoomMedia(roomId);
    }

    @Override
    public List<FileUploadResponse> getChatRoomImages(Long roomId) {
        ChatFileStorageStrategy strategy = chatFileStorageStrategyFactory.getStorageStrategy();
        return strategy.getChatRoomImages(roomId);
    }

    @Override
    public List<FileUploadResponse> getChatRoomVideos(Long roomId) {
        ChatFileStorageStrategy strategy = chatFileStorageStrategyFactory.getStorageStrategy();
        return strategy.getChatRoomVideos(roomId);
    }

    @Override
    public List<FileUploadResponse> getChatRoomAudios(Long roomId) {
        ChatFileStorageStrategy strategy = chatFileStorageStrategyFactory.getStorageStrategy();
        return strategy.getChatRoomAudios(roomId);
    }

    @Override
    public List<FileUploadResponse> getChatRoomDocuments(Long roomId) {
        ChatFileStorageStrategy strategy = chatFileStorageStrategyFactory.getStorageStrategy();
        return strategy.getChatRoomDocuments(roomId);
    }

    @Override
    public List<FileUploadResponse> getAttachmentsByMessageId(Long messageId) {
        ChatFileStorageStrategy strategy = chatFileStorageStrategyFactory.getStorageStrategy();
        return strategy.getAttachmentsByMessageId(messageId);
    }
}
