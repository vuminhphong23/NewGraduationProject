package GraduationProject.forumikaa.service;

import GraduationProject.forumikaa.dto.ChatMessageDto;
import GraduationProject.forumikaa.dto.ChatRoomDto;

import java.util.List;

public interface ChatService {
    
    // Lấy danh sách chat rooms của user
    List<ChatRoomDto> getUserChatRooms(Long userId);
    
    // Tạo hoặc tìm chat room 1-1
    ChatRoomDto findOrCreatePrivateChat(Long userId1, Long userId2);
    
    // Tạo group chat
    ChatRoomDto createGroupChat(String groupName, Long createdById, List<Long> userIds);
    
    // Xóa chat room
    void deleteChatRoom(Long roomId, Long userId);
    
    // Kiểm tra quyền truy cập room
    boolean hasAccessToRoom(Long roomId, Long userId);
    
    // Lấy tin nhắn của room
    List<ChatMessageDto> getRoomMessages(Long roomId, int page, int size);
    
    // Gửi tin nhắn
    ChatMessageDto sendMessage(Long roomId, Long senderId, String content, String messageType);
    
    // Đánh dấu tin nhắn đã đọc
    void markMessageAsRead(Long messageId, Long userId);
    
    // Đánh dấu tất cả tin nhắn trong room đã đọc
    void markRoomAsRead(Long roomId, Long userId);
}