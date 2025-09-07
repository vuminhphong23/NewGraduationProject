package GraduationProject.forumikaa.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRoomDto {
    private Long id;
    private String roomName;
    private Boolean isGroup;
    private String roomAvatar;
    private Long createdById;
    private String createdByUsername;
    private String createdByFullName;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
    
    private List<ChatRoomMemberDto> members;
    private ChatMessageDto lastMessage;
    private String lastUnreadMessage;
    private int unreadCount;
    private int memberCount;
}
