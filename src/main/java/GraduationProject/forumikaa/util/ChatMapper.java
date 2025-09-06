package GraduationProject.forumikaa.util;

import GraduationProject.forumikaa.dto.ChatMessageDto;
import GraduationProject.forumikaa.dto.ChatRoomDto;
import GraduationProject.forumikaa.entity.ChatMessage;
import GraduationProject.forumikaa.entity.ChatRoom;
import GraduationProject.forumikaa.entity.User;

import java.util.List;
import java.util.stream.Collectors;

public class ChatMapper {

    public static ChatRoomDto toChatRoomDto(ChatRoom room) {
        if (room == null) return null;
        
        return ChatRoomDto.builder()
                .id(room.getId())
                .roomName(room.getRoomName())
                .isGroup(room.isGroup())
                .createdAt(room.getCreatedAt())
                .updatedAt(room.getUpdatedAt())
                .build();
    }

    public static ChatMessageDto toChatMessageDto(ChatMessage message) {
        if (message == null) return null;
        
        return ChatMessageDto.builder()
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
    }

    public static List<ChatRoomDto> toChatRoomDtoList(List<ChatRoom> rooms) {
        if (rooms == null) return null;
        return rooms.stream()
                .map(ChatMapper::toChatRoomDto)
                .collect(Collectors.toList());
    }

    public static List<ChatMessageDto> toChatMessageDtoList(List<ChatMessage> messages) {
        if (messages == null) return null;
        try {
            System.out.println("ChatMapper.toChatMessageDtoList() - Processing " + messages.size() + " messages");
            return messages.stream()
                    .map(ChatMapper::toChatMessageDto)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("ChatMapper.toChatMessageDtoList() - Error: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
}