package GraduationProject.forumikaa.service;

import GraduationProject.forumikaa.dao.ChatAttachmentRepository;
import GraduationProject.forumikaa.dao.ChatMessageDao;
import GraduationProject.forumikaa.dao.ChatRoomDao;
import GraduationProject.forumikaa.dao.ChatRoomMemberDao;
import GraduationProject.forumikaa.service.ChatAttachmentService;
import GraduationProject.forumikaa.dto.ChatMessageDto;
import GraduationProject.forumikaa.dto.ChatRoomDto;
import GraduationProject.forumikaa.dto.ChatRoomMemberDto;
import GraduationProject.forumikaa.dto.FileUploadResponse;
import GraduationProject.forumikaa.entity.ChatMessage;
import GraduationProject.forumikaa.entity.ChatRoom;
import GraduationProject.forumikaa.entity.ChatRoomMember;
import GraduationProject.forumikaa.entity.User;
import GraduationProject.forumikaa.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
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
    
    @Autowired
    private ChatAttachmentRepository chatAttachmentRepository;
    
    @Autowired
    private ChatAttachmentService chatAttachmentService;
    
    @Override
    @Transactional(readOnly = true)
    public List<ChatRoomDto> getUserChatRooms(Long userId) {
        List<ChatRoom> rooms = chatRoomDao.findByUserIdOrderByLastMessageDesc(userId);
        return rooms.stream()
                .map(room -> {
                    // Load members riêng cho mỗi room
                    List<ChatRoomMember> members = chatRoomMemberDao.findByRoomId(room.getId());
                    
                    // Tạo DTO với members riêng biệt
                    ChatRoomDto dto = ChatRoomDto.builder()
                            .id(room.getId())
                            .roomName(room.getRoomName())
                            .isGroup(room.getIsGroup())
                            .roomAvatar(room.getRoomAvatar())
                            .createdAt(room.getCreatedAt())
                            .updatedAt(room.getUpdatedAt())
                            .build();
                    
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
                        ChatMessage lastMsg = lastMessages.getContent().get(0);
                        ChatMessageDto lastMessageDto = ChatMessageDto.builder()
                                .id(lastMsg.getId())
                                .roomId(lastMsg.getRoom() != null ? lastMsg.getRoom().getId() : null)
                                .senderId(lastMsg.getSender() != null ? lastMsg.getSender().getId() : null)
                                .senderUsername(lastMsg.getSender() != null ? lastMsg.getSender().getUsername() : null)
                                .content(lastMsg.getContent())
                                .messageType(lastMsg.getMessageType() != null ? lastMsg.getMessageType().name() : null)
                                .isRead(lastMsg.isRead())
                                .createdAt(lastMsg.getCreatedAt())
                                .readAt(lastMsg.getReadAt())
                                .build();
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
            // Load members để trả về đầy đủ thông tin
            List<ChatRoomMember> members = chatRoomMemberDao.findByRoomId(existingRoom.get().getId());
            
            // Tạo DTO thủ công để tránh vòng lặp vô hạn
            ChatRoomDto dto = ChatRoomDto.builder()
                    .id(existingRoom.get().getId())
                    .roomName(existingRoom.get().getRoomName())
                    .isGroup(existingRoom.get().getIsGroup())
                    .roomAvatar(existingRoom.get().getRoomAvatar())
                    .createdAt(existingRoom.get().getCreatedAt())
                    .updatedAt(existingRoom.get().getUpdatedAt())
                    .build(); 
            
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
        
        
        // Tạo chat room mới
        ChatRoom room = new ChatRoom();
        room.setRoomName("Private Chat");
        room.setIsGroup(false);
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
        ChatRoomDto dto = ChatRoomDto.builder()
                .id(savedRoom.getId())
                .roomName(savedRoom.getRoomName())
                .isGroup(savedRoom.getIsGroup())
                .roomAvatar(savedRoom.getRoomAvatar())
                .createdAt(savedRoom.getCreatedAt())
                .updatedAt(savedRoom.getUpdatedAt())
                .build();
        
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
        // Tạo chat room mới
        ChatRoom room = new ChatRoom();
        room.setRoomName(groupName);
        room.setIsGroup(true);
        room.setCreatedAt(LocalDateTime.now());
        room.setUpdatedAt(LocalDateTime.now());
        
        ChatRoom savedRoom = chatRoomDao.save(room);
        
        // Thêm tất cả user vào room
        for (Long userId : userIds) {
            ChatRoomMember member = new ChatRoomMember();
            member.setRoom(savedRoom);
            member.setUser(new User(userId));
            member.setJoinedAt(LocalDateTime.now());
            chatRoomMemberDao.save(member);
        }
        
        // Load members để trả về đầy đủ thông tin
        List<ChatRoomMember> members = chatRoomMemberDao.findByRoomId(savedRoom.getId());
        
        // Tạo DTO thủ công để tránh vòng lặp vô hạn
        ChatRoomDto dto = ChatRoomDto.builder()
                .id(savedRoom.getId())
                .roomName(savedRoom.getRoomName())
                .isGroup(savedRoom.getIsGroup())
                .roomAvatar(savedRoom.getRoomAvatar())
                .createdAt(savedRoom.getCreatedAt())
                .updatedAt(savedRoom.getUpdatedAt())
                .build();
        
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
        // Kiểm tra quyền truy cập
        if (!hasAccessToRoom(roomId, userId)) {
            throw new RuntimeException("Bạn không có quyền xóa cuộc trò chuyện này");
        }
        
        // Xóa tất cả attachments trong room trước
        chatAttachmentRepository.deleteByRoomId(roomId);
        
        // Xóa tất cả tin nhắn trong room
        chatMessageDao.deleteByRoomId(roomId);
        
        // Xóa tất cả members
        chatRoomMemberDao.deleteByRoomId(roomId);
        
        // Xóa room
        chatRoomDao.deleteById(roomId);
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
            List<ChatMessage> messages = chatMessageDao.findByRoomIdOrderByCreatedAtAsc(roomId);
            return messages.stream()
                    .<ChatMessageDto>map(message -> {
                        // Load attachments cho message
                        List<FileUploadResponse> attachments = new ArrayList<>();
                        if (message.getAttachments() != null && !message.getAttachments().isEmpty()) {
                            attachments = message.getAttachments().stream()
                                    .map(attachment -> {
                                        FileUploadResponse response = new FileUploadResponse();
                                        response.setId(attachment.getId());
                                        response.setFileName(attachment.getFileName());
                                        response.setOriginalName(attachment.getOriginalName());
                                        response.setFilePath(attachment.getFilePath());
                                        response.setFileSize(attachment.getFileSize());
                                        response.setMimeType(attachment.getMimeType());
                                        // response.setFileExtension(attachment.getFileExtension()); // Method không tồn tại
                                        // Set download URL - cho local storage cần URL đầy đủ
                                        if (attachment.isCloudStorage()) {
                                            response.setDownloadUrl(attachment.getDisplayUrl());
                                            response.setPreviewUrl(attachment.getDisplayUrl());
                                        } else {
                                            // Local storage - cần URL đầy đủ
                                            response.setDownloadUrl("/api/chat/files/download/" + attachment.getId());
                                            response.setPreviewUrl("/files/" + attachment.getFilePath());
                                        }
                                        response.setThumbnailUrl(attachment.getDisplayThumbnailUrl());
                                        response.setCloudinaryPublicId(attachment.getCloudinaryPublicId());
                                        response.setCloudinaryUrl(attachment.getCloudinaryUrl());
                                        response.setCloudStorage(attachment.isCloudStorage());
                                        response.setFileType(attachment.getAttachmentType().name().toLowerCase());
                                        return response;
                                    })
                                    .collect(Collectors.toList());
                        }
                        
                        return ChatMessageDto.builder()
                                .id(message.getId())
                                .roomId(message.getRoom() != null ? message.getRoom().getId() : null)
                                .senderId(message.getSender() != null ? message.getSender().getId() : null)
                                .senderUsername(message.getSender() != null ? message.getSender().getUsername() : null)
                                .senderFullName(message.getSender() != null ? message.getSender().getUsername() : null) // User không có getFullName
                                .senderAvatar(null) // User không có getAvatar
                                .content(message.getContent())
                                .messageType(message.getMessageType() != null ? message.getMessageType().name() : null)
                                .isRead(message.isRead())
                                .createdAt(message.getCreatedAt())
                                .readAt(message.getReadAt())
                                .attachments(attachments)
                                .build();
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
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
        
        // Lấy attachments của message nếu có
        List<FileUploadResponse> attachments = chatAttachmentService.getAttachmentsByMessageId(message.getId());
        
        ChatMessageDto messageDto = ChatMessageDto.builder()
                .id(message.getId())
                .roomId(message.getRoom() != null ? message.getRoom().getId() : null)
                .senderId(message.getSender() != null ? message.getSender().getId() : null)
                .senderUsername(message.getSender() != null ? message.getSender().getUsername() : null)
                .content(message.getContent())
                .messageType(message.getMessageType() != null ? message.getMessageType().name() : null)
                .isRead(message.isRead())
                .createdAt(message.getCreatedAt())
                .readAt(message.getReadAt())
                .build();
        
        // Thêm attachments vào DTO nếu có
        if (attachments != null && !attachments.isEmpty()) {
            messageDto.setAttachments(attachments);
        }
        
        return messageDto;
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