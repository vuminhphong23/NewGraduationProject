package GraduationProject.forumikaa.service;

import GraduationProject.forumikaa.dao.ChatMessageDao;
import GraduationProject.forumikaa.dao.ChatRoomDao;
import GraduationProject.forumikaa.dao.ChatRoomMemberDao;
import GraduationProject.forumikaa.dto.ChatMessageDto;
import GraduationProject.forumikaa.dto.ChatRoomDto;
import GraduationProject.forumikaa.entity.ChatMessage;
import GraduationProject.forumikaa.entity.ChatRoom;
import GraduationProject.forumikaa.entity.ChatRoomMember;
import GraduationProject.forumikaa.entity.User;
import GraduationProject.forumikaa.exception.ResourceNotFoundException;
import GraduationProject.forumikaa.util.ChatMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class ChatServiceImpl implements ChatService {

    @Autowired
    private ChatRoomDao chatRoomDao;
    
    @Autowired
    private ChatMessageDao chatMessageDao;
    
    @Autowired
    private ChatRoomMemberDao chatRoomMemberDao;
    
    @Override
    @Transactional(readOnly = true)
    public List<ChatRoomDto> getUserChatRooms(Long userId) {
        List<ChatRoom> rooms = chatRoomDao.findByUserIdOrderByLastMessageDesc(userId);
        return rooms.stream()
                .map(room -> {
                    ChatRoomDto dto = ChatMapper.toChatRoomDto(room);
                    
                    // Lấy tin nhắn cuối cùng chưa đọc
                    List<ChatMessage> unreadMessages = chatMessageDao.findUnreadMessages(room.getId(), userId);
                    if (!unreadMessages.isEmpty()) {
                        ChatMessage lastUnreadMessage = unreadMessages.get(unreadMessages.size() - 1);
                        dto.setLastUnreadMessage(lastUnreadMessage.getContent());
                        dto.setUnreadCount(unreadMessages.size());
                    } else {
                        dto.setUnreadCount(0);
                    }
                    
                    // Lấy tin nhắn cuối cùng
                    Page<ChatMessage> lastMessages = chatMessageDao.findByRoomIdOrderByCreatedAtDesc(room.getId(), PageRequest.of(0, 1));
                    if (!lastMessages.isEmpty()) {
                        ChatMessageDto lastMessageDto = ChatMapper.toChatMessageDto(lastMessages.getContent().get(0));
                        dto.setLastMessage(lastMessageDto);
                    }
                    
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    public ChatRoomDto findOrCreatePrivateChat(Long userId1, Long userId2) {
        // Tìm chat room 1-1 đã tồn tại
        Optional<ChatRoom> existingRoom = chatRoomDao.findPrivateChatBetweenUsers(userId1, userId2);
        
        if (existingRoom.isPresent()) {
            return ChatMapper.toChatRoomDto(existingRoom.get());
        }
        
        // Tạo chat room mới
        ChatRoom room = new ChatRoom();
        room.setRoomName("Private Chat");
        room.setGroup(false);
        room.setCreatedAt(LocalDateTime.now());
        room.setUpdatedAt(LocalDateTime.now());
        
        room = chatRoomDao.save(room);
        
        // Thêm 2 user vào room
        ChatRoomMember member1 = new ChatRoomMember();
        member1.setRoom(room);
        member1.setUser(new User(userId1));
        member1.setJoinedAt(LocalDateTime.now());
        chatRoomMemberDao.save(member1);
        
        ChatRoomMember member2 = new ChatRoomMember();
        member2.setRoom(room);
        member2.setUser(new User(userId2));
        member2.setJoinedAt(LocalDateTime.now());
        chatRoomMemberDao.save(member2);
        
        return ChatMapper.toChatRoomDto(room);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasAccessToRoom(Long roomId, Long userId) {
        return chatRoomMemberDao.existsByRoomIdAndUserId(roomId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatMessageDto> getRoomMessages(Long roomId, int page, int size) {
        try {
            System.out.println("ChatServiceImpl.getRoomMessages() - roomId: " + roomId);
            List<ChatMessage> messages = chatMessageDao.findByRoomIdOrderByCreatedAtAsc(roomId);
            System.out.println("ChatServiceImpl.getRoomMessages() - Found " + messages.size() + " messages");
            return ChatMapper.toChatMessageDtoList(messages);
        } catch (Exception e) {
            System.err.println("ChatServiceImpl.getRoomMessages() - Error: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @Override
    public ChatMessageDto sendMessage(Long roomId, Long senderId, String content, String messageType) {
        ChatRoom room = chatRoomDao.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Chat room not found: " + roomId));
        
        ChatMessage message = new ChatMessage();
        message.setRoom(room);
        message.setSender(new User(senderId));
        message.setContent(content);
        message.setMessageType(ChatMessage.MessageType.valueOf(messageType));
        message.setRead(false);
        message.setCreatedAt(LocalDateTime.now());
        
        message = chatMessageDao.save(message);
        return ChatMapper.toChatMessageDto(message);
    }

    @Override
    public void markMessageAsRead(Long messageId, Long userId) {
        ChatMessage message = chatMessageDao.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found: " + messageId));
        
        // Chỉ đánh dấu đã đọc nếu không phải tin nhắn của chính user đó
        if (!message.getSender().getId().equals(userId)) {
            message.setRead(true);
            message.setReadAt(LocalDateTime.now());
            chatMessageDao.save(message);
        }
    }

    @Override
    public void markRoomAsRead(Long roomId, Long userId) {
        // Lấy tất cả tin nhắn chưa đọc của user trong room (đã filter sẵn tin nhắn của người khác)
        List<ChatMessage> unreadMessages = chatMessageDao.findUnreadMessages(roomId, userId);
        
        // Đánh dấu tất cả đã đọc
        for (ChatMessage message : unreadMessages) {
            message.setRead(true);
            message.setReadAt(LocalDateTime.now());
        }
        
        if (!unreadMessages.isEmpty()) {
            chatMessageDao.saveAll(unreadMessages);
        }
    }
}