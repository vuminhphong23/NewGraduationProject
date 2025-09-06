package GraduationProject.forumikaa.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebSocketMessageDto {
    private String type; // MESSAGE, JOIN_ROOM, LEAVE_ROOM, TYPING, STOP_TYPING, USER_ONLINE, USER_OFFLINE
    private Long roomId;
    private Long userId;
    private String username;
    private String content;
    private ChatMessageDto message;
    private ChatRoomDto room;
    private Object data;
    private String timestamp;
}
