package GraduationProject.forumikaa.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SuggestedPostDto {
    private Long id;
    private String title;
    private String content;
    private Long userId;
    private LocalDateTime createdAt;
    private String privacy;
    private Integer friendshipLevel;
    private String userName;
    private String firstName;
    private String lastName;
    private String userAvatar;
    private Long likeCount;
    private Long commentCount;
    private Long shareCount;
    private List<String> topicNames; // Thêm field này để lưu topics
}



