package GraduationProject.forumikaa.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupMemberDto {
    private Long id;
    private Long userId;
    private String username;
    private String firstName;
    private String lastName;
    private String fullName;
    private String avatar;
    private String role;
    private boolean isOnline;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime joinedAt;
    private Long memberCount;
    private Long postCount;
}
