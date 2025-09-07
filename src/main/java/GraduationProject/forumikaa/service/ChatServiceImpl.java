package GraduationProject.forumikaa.service;

import GraduationProject.forumikaa.dao.ChatMessageDao;
import GraduationProject.forumikaa.dao.ChatRoomDao;
import GraduationProject.forumikaa.dao.ChatRoomMemberDao;
import GraduationProject.forumikaa.dto.ChatMessageDto;
import GraduationProject.forumikaa.dto.ChatRoomDto;
import GraduationProject.forumikaa.dto.ChatRoomMemberDto;
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
                    // Load members riêng cho mỗi room
                    List<ChatRoomMember> members = chatRoomMemberDao.findByRoomId(room.getId());
                    System.out.println("🔍 ChatServiceImpl - Room " + room.getId() + " has " + members.size() + " members");
                    
                    // Tạo DTO với members riêng biệt
                    ChatRoomDto dto = ChatMapper.toChatRoomDto(room);
                    System.out.println("🔍 ChatServiceImpl.getUserChatRooms() - Room " + room.getId() + " Name: " + room.getRoomName() + ", IsGroup: " + room.isGroup() + ", DTO Name: " + dto.getRoomName() + ", DTO IsGroup: " + dto.isGroup());
                    
                    // Set members vào DTO
                    if (members != null) {
                        List<ChatRoomMemberDto> memberDtos = members.stream()
                                .map(member -> ChatRoomMemberDto.builder()
                                        .id(member.getId())
                                        .roomId(member.getRoom().getId())
                                        .userId(member.getUser().getId())
                                        .username(member.getUser().getUsername())
                                        .fullName(member.getUser().getFirstName() != null && member.getUser().getLastName() != null 
                                                ? member.getUser().getFirstName() + " " + member.getUser().getLastName()
                                                : member.getUser().getUsername())
                                        .avatar(member.getUser().getUserProfile() != null ? member.getUser().getUserProfile().getAvatar() : null)
                                        .isOnline(false) // TODO: implement online status
                                        .joinedAt(member.getJoinedAt())
                                        .build())
                                .collect(Collectors.toList());
                        dto.setMembers(memberDtos);
                        dto.setMemberCount(memberDtos.size());
                    }
                    
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
        System.out.println("🔍 ChatServiceImpl.findOrCreatePrivateChat() - User1: " + userId1 + ", User2: " + userId2);
        
        // Tìm chat room 1-1 đã tồn tại
        Optional<ChatRoom> existingRoom = chatRoomDao.findPrivateChatBetweenUsers(userId1, userId2);
        System.out.println("🔍 ChatServiceImpl.findOrCreatePrivateChat() - Existing room found: " + existingRoom.isPresent());
        
        if (existingRoom.isPresent()) {
            // Load members để trả về đầy đủ thông tin
            List<ChatRoomMember> members = chatRoomMemberDao.findByRoomId(existingRoom.get().getId());
            
            // Tạo DTO thủ công để tránh vòng lặp vô hạn
            ChatRoomDto dto = ChatMapper.toChatRoomDto(existingRoom.get());
            
            // Map members thủ công
            if (members != null) {
                List<ChatRoomMemberDto> memberDtos = members.stream()
                        .map(member -> ChatRoomMemberDto.builder()
                                .id(member.getId())
                                .roomId(existingRoom.get().getId())
                                .userId(member.getUser().getId())
                                .username(member.getUser().getUsername())
                                .fullName(member.getUser().getFirstName() != null && member.getUser().getLastName() != null 
                                        ? member.getUser().getFirstName() + " " + member.getUser().getLastName()
                                        : member.getUser().getUsername())
                                .avatar(member.getUser().getUserProfile() != null ? member.getUser().getUserProfile().getAvatar() : null)
                                .isOnline(false) // TODO: implement online status
                                .joinedAt(member.getJoinedAt())
                                .build())
                        .collect(Collectors.toList());
                dto.setMembers(memberDtos);
                dto.setMemberCount(memberDtos.size());
            }
            
            return dto;
        }
        
        System.out.println("🔍 ChatServiceImpl.findOrCreatePrivateChat() - Creating new private chat room");
        
        // Tạo chat room mới
        ChatRoom room = new ChatRoom();
        room.setRoomName("Private Chat");
        room.setGroup(false);
        room.setCreatedAt(LocalDateTime.now());
        room.setUpdatedAt(LocalDateTime.now());
        
        ChatRoom savedRoom = chatRoomDao.save(room);
        
        // Thêm 2 user vào room
        ChatRoomMember member1 = new ChatRoomMember();
        member1.setRoom(savedRoom);
        member1.setUser(new User(userId1));
        member1.setJoinedAt(LocalDateTime.now());
        chatRoomMemberDao.save(member1);
        
        ChatRoomMember member2 = new ChatRoomMember();
        member2.setRoom(savedRoom);
        member2.setUser(new User(userId2));
        member2.setJoinedAt(LocalDateTime.now());
        chatRoomMemberDao.save(member2);
        
        // Load members để trả về đầy đủ thông tin
        List<ChatRoomMember> members = chatRoomMemberDao.findByRoomId(savedRoom.getId());
        
        // Tạo DTO thủ công để tránh vòng lặp vô hạn
        ChatRoomDto dto = ChatMapper.toChatRoomDto(savedRoom);
        
        // Map members thủ công
        if (members != null) {
            List<ChatRoomMemberDto> memberDtos = members.stream()
                    .map(member -> ChatRoomMemberDto.builder()
                            .id(member.getId())
                            .roomId(savedRoom.getId())
                            .userId(member.getUser().getId())
                            .username(member.getUser().getUsername())
                            .fullName(member.getUser().getFirstName() != null && member.getUser().getLastName() != null 
                                    ? member.getUser().getFirstName() + " " + member.getUser().getLastName()
                                    : member.getUser().getUsername())
                            .avatar(member.getUser().getUserProfile() != null ? member.getUser().getUserProfile().getAvatar() : null)
                            .isOnline(false) // TODO: implement online status
                            .joinedAt(member.getJoinedAt())
                            .build())
                    .collect(Collectors.toList());
            dto.setMembers(memberDtos);
            dto.setMemberCount(memberDtos.size());
        }
        
        return dto;
    }

    @Override
    public ChatRoomDto createGroupChat(String groupName, Long createdById, List<Long> userIds) {
        System.out.println("🔍 ChatServiceImpl.createGroupChat() - Group Name: " + groupName + ", Created By: " + createdById + ", User IDs: " + userIds);
        
        // Tạo chat room mới
        ChatRoom room = new ChatRoom();
        room.setRoomName(groupName);
        room.setGroup(true);
        room.setCreatedAt(LocalDateTime.now());
        room.setUpdatedAt(LocalDateTime.now());
        System.out.println("🔍 ChatServiceImpl.createGroupChat() - Creating room with name: " + groupName + ", isGroup: true");
        
        ChatRoom savedRoom = chatRoomDao.save(room);
        System.out.println("🔍 ChatServiceImpl.createGroupChat() - Room saved with ID: " + savedRoom.getId());
        
        // Thêm tất cả user vào room
        for (Long userId : userIds) {
            ChatRoomMember member = new ChatRoomMember();
            member.setRoom(savedRoom);
            member.setUser(new User(userId));
            member.setJoinedAt(LocalDateTime.now());
            chatRoomMemberDao.save(member);
            System.out.println("🔍 ChatServiceImpl.createGroupChat() - Added member: " + userId);
        }
        
        // Load members để trả về đầy đủ thông tin
        List<ChatRoomMember> members = chatRoomMemberDao.findByRoomId(savedRoom.getId());
        
        // Tạo DTO thủ công để tránh vòng lặp vô hạn
        ChatRoomDto dto = ChatMapper.toChatRoomDto(savedRoom);
        
        // Map members thủ công
        if (members != null) {
            List<ChatRoomMemberDto> memberDtos = members.stream()
                    .map(member -> ChatRoomMemberDto.builder()
                            .id(member.getId())
                            .roomId(savedRoom.getId())
                            .userId(member.getUser().getId())
                            .username(member.getUser().getUsername())
                            .fullName(member.getUser().getFirstName() != null && member.getUser().getLastName() != null 
                                    ? member.getUser().getFirstName() + " " + member.getUser().getLastName()
                                    : member.getUser().getUsername())
                            .avatar(member.getUser().getUserProfile() != null ? member.getUser().getUserProfile().getAvatar() : null)
                            .isOnline(false) // TODO: implement online status
                            .joinedAt(member.getJoinedAt())
                            .build())
                    .collect(Collectors.toList());
            dto.setMembers(memberDtos);
            dto.setMemberCount(memberDtos.size());
        }
        
        return dto;
    }

    @Override
    @Transactional
    public void deleteChatRoom(Long roomId, Long userId) {
        System.out.println("🔍 ChatServiceImpl.deleteChatRoom() - Room ID: " + roomId + ", User ID: " + userId);
        
        // Kiểm tra quyền truy cập
        if (!hasAccessToRoom(roomId, userId)) {
            throw new RuntimeException("Bạn không có quyền xóa cuộc trò chuyện này");
        }
        
        // Xóa tất cả tin nhắn trong room
        chatMessageDao.deleteByRoomId(roomId);
        System.out.println("🔍 ChatServiceImpl.deleteChatRoom() - Messages deleted");
        
        // Xóa tất cả members
        chatRoomMemberDao.deleteByRoomId(roomId);
        System.out.println("🔍 ChatServiceImpl.deleteChatRoom() - Members deleted");
        
        // Xóa room
        chatRoomDao.deleteById(roomId);
        System.out.println("🔍 ChatServiceImpl.deleteChatRoom() - Room deleted");
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